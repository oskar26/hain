package app.hain.models.api

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import kotlin.math.ceil

/** One GGUF file of an own model (developer mode, T-211). [size] 0 and [sha256] "" = unknown. */
data class GgufFile(
    val repo: String,
    val name: String,
    val url: String,
    val size: Long,
    val sha256: String = "",
) {
    /** "LiquidAI/LFM3-1.7B-GGUF · LFM3-1.7B-Q8_0.gguf" (or only the file for a direct link). */
    val label: String get() = if (repo.isBlank()) name else "$repo · $name"
}

/** What the developer typed: a Hugging Face repo, a direct https link to a .gguf, or nothing usable. */
sealed interface ModelSource {
    data class Repo(
        val id: String,
    ) : ModelSource

    data class Direct(
        val file: GgufFile,
    ) : ModelSource

    data object Invalid : ModelSource
}

/** Result of listing a repo. */
sealed interface HfListing {
    data class Files(
        val files: List<GgufFile>,
    ) : HfListing

    /** [reason]: "offline", "not_found", "network", "no_gguf", "invalid". */
    data class Failed(
        val reason: String,
    ) : HfListing
}

/** Hugging Face model files for developer mode (T-211): pure parsing + one listing call via the gate. */
object HuggingFace {
    private const val HOST = "https://huggingface.co"
    private val REPO = Regex("""^[A-Za-z0-9][A-Za-z0-9._-]*/[A-Za-z0-9][A-Za-z0-9._-]*$""")
    private val json = Json { ignoreUnknownKeys = true }

    /** `LiquidAI/LFM3-1.7B-GGUF`, `https://huggingface.co/LiquidAI/LFM3-1.7B-GGUF` or a direct .gguf link. */
    @Suppress("ReturnCount")
    fun source(input: String): ModelSource {
        val t = input.trim()
        if (t.isEmpty() || t.any { it.isWhitespace() }) return ModelSource.Invalid
        if (t.startsWith("https://", ignoreCase = true) &&
            t.substringBefore('?').endsWith(".gguf", ignoreCase = true)
        ) {
            val name = t.substringBefore('?').substringAfterLast('/')
            return ModelSource.Direct(GgufFile("", name, t, 0L))
        }
        if (t.startsWith("http://", ignoreCase = true)) return ModelSource.Invalid
        val id =
            t
                .removePrefix("https://")
                .removePrefix("huggingface.co/")
                .trimEnd('/')
                .split('/')
                .take(2)
                .joinToString("/")
        return if (REPO.matches(id)) ModelSource.Repo(id) else ModelSource.Invalid
    }

    /** API address that lists the files of [repo] with sizes and sha256 (`blobs=true`). */
    fun apiUrl(repo: String): String = "$HOST/api/models/$repo?blobs=true"

    /** Download address of [file] in [repo]. */
    fun fileUrl(
        repo: String,
        file: String,
    ): String = "$HOST/$repo/resolve/main/$file"

    /** The .gguf files of an API answer, smallest first; `null` when the answer is not readable. */
    @Suppress("ReturnCount")
    fun parseFiles(
        repo: String,
        body: String,
    ): List<GgufFile>? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val siblings = runCatching { root["siblings"]?.jsonArray }.getOrNull() ?: return null
        return siblings
            .mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .mapNotNull { s ->
                val name = s.string("rfilename") ?: return@mapNotNull null
                if (!name.endsWith(".gguf", ignoreCase = true)) return@mapNotNull null
                // split GGUFs (…-00001-of-00003.gguf) cannot be loaded as one file
                if (SPLIT.containsMatchIn(name)) return@mapNotNull null
                // vision projectors are not chat models
                if (name.substringAfterLast('/').startsWith("mmproj", ignoreCase = true)) return@mapNotNull null
                val lfs = s["lfs"] as? JsonObject
                val size = s.long("size") ?: lfs?.long("size") ?: 0L
                GgufFile(repo, name, fileUrl(repo, name), size, lfs?.string("sha256").orEmpty())
            }.sortedBy { it.size }
    }

    /** Rough RAM a GGUF needs to run with Hain's context: 1.5 × file + 2 GB, at least 3 GB. */
    fun minRamGb(size: Long): Int {
        if (size <= 0) return 0
        val gb = size / BYTES_PER_GB
        return maxOf(MIN_RAM_GB, ceil(gb * RAM_FACTOR + RAM_BASE_GB).toInt())
    }

    /** The file as a catalogue entry, so the existing pack downloader can fetch it. */
    fun asModel(file: GgufFile): ModelInfo =
        ModelInfo(
            id =
                "dev-" +
                    file.label
                        .lowercase()
                        .replace(Regex("[^a-z0-9]+"), "-")
                        .trim('-'),
            name = file.name.substringAfterLast('/'),
            kind = "custom",
            files = listOf(ModelFile(file.url, file.sha256, file.size)),
            minRamGb = minRamGb(file.size),
            licence = "",
            licenceUrl = if (file.repo.isBlank()) "" else "$HOST/${file.repo}",
            isDefault = false,
        )

    /** Lists the .gguf files of [repo] through [gate] (purpose MODEL_DOWNLOAD, started by the user). */
    @Suppress("ReturnCount", "SwallowedException") // each failure becomes a short reason for the UI
    suspend fun list(
        gate: NetworkGate,
        repo: String,
    ): HfListing {
        val response =
            try {
                gate.fetch(GateRequest(apiUrl(repo), Purpose.MODEL_DOWNLOAD, userStarted = true))
            } catch (e: OfflineException) {
                return HfListing.Failed("offline")
            } catch (e: IOException) {
                return HfListing.Failed("network")
            } catch (e: IllegalArgumentException) {
                return HfListing.Failed("invalid")
            }
        if (response.code == HTTP_NOT_FOUND || response.code == HTTP_UNAUTHORIZED) return HfListing.Failed("not_found")
        if (!response.ok) return HfListing.Failed("network")
        val files = parseFiles(repo, response.body) ?: return HfListing.Failed("network")
        return if (files.isEmpty()) HfListing.Failed("no_gguf") else HfListing.Files(files)
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull

    private val SPLIT = Regex("""-\d{5}-of-\d{5}\.gguf$""", RegexOption.IGNORE_CASE)
    private const val BYTES_PER_GB = 1_000_000_000.0
    private const val RAM_FACTOR = 1.5
    private const val RAM_BASE_GB = 2.0
    private const val MIN_RAM_GB = 3
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_UNAUTHORIZED = 401
}
