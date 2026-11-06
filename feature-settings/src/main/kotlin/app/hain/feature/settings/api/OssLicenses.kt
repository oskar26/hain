package app.hain.feature.settings.api

import android.content.res.AssetManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * T-285: the open-source licences shown in "Über Hain". `assets/licenses/licenses.json` and the
 * texts next to it are written by `tools/licenses/gen_licenses.py` from `tools/licenses/oss.toml`;
 * everything is read from the APK, never from the network.
 */
internal data class OssEntry(
    val name: String,
    val version: String,
    val licence: String,
    val url: String,
    /** File names in `assets/licenses/texts/`, shown one after the other. */
    val texts: List<String>,
) {
    /** Stable key of the entry (saved while its text is open). */
    fun key(group: OssGroup): String = "${group.id}/$name"
}

/** The groups of the page, in this order; the ids are the `group` values of oss.toml. */
internal enum class OssGroup(
    val id: String,
) {
    LIBRARIES("libraries"),
    MODELS("models"),
    FONTS("fonts"),
    ICONS("icons"),
    ENGINE("engine"),
    CONTENT("content"),
    ;

    companion object {
        fun of(id: String): OssGroup? = entries.firstOrNull { it.id == id }
    }
}

internal data class OssCatalog(
    /** Hain itself (GPL-3.0). */
    val app: OssEntry,
    val groups: List<Pair<OssGroup, List<OssEntry>>>,
) {
    /** The entry behind [key]; [APP_KEY] is Hain itself. */
    fun find(key: String): OssEntry? =
        if (key == APP_KEY) {
            app
        } else {
            groups.firstNotNullOfOrNull { (group, list) -> list.firstOrNull { it.key(group) == key } }
        }

    val count: Int get() = groups.sumOf { it.second.size }

    companion object {
        const val APP_KEY = "app"
        val EMPTY = OssCatalog(OssEntry("Hain", "", "GPL-3.0", "", emptyList()), emptyList())
    }
}

internal object OssLicenses {
    const val DIR = "licenses"

    /** Parses licenses.json; groups the app does not know are left out. */
    fun parse(json: String): OssCatalog {
        val root = Json.parseToJsonElement(json).jsonObject
        val groups =
            root["groups"]?.jsonArray.orEmpty().mapNotNull { element ->
                val obj = element.jsonObject
                val group = OssGroup.of(obj.string("id")) ?: return@mapNotNull null
                group to obj["entries"]?.jsonArray.orEmpty().map { entry(it.jsonObject) }
            }
        return OssCatalog(entry(root.getValue("app").jsonObject), groups.sortedBy { it.first.ordinal })
    }

    /** The catalogue from the APK; empty when the file is missing (it never is in a real build). */
    fun load(assets: AssetManager): OssCatalog =
        runCatching { assets.open("$DIR/licenses.json").bufferedReader().use { it.readText() } }
            .map(::parse)
            .getOrDefault(OssCatalog.EMPTY)

    /** The full licence text of [entry], read offline from the APK and made readable on a phone. */
    fun text(
        assets: AssetManager,
        entry: OssEntry,
    ): String =
        entry.texts.joinToString("\n\n") { name ->
            val raw = runCatching { assets.open("$DIR/texts/$name").bufferedReader().use { it.readText() } }
            reflow(raw.getOrDefault(""))
        }

    /**
     * Licence files are wrapped at about 78 characters; on a phone that breaks lines in the middle.
     * Lines of one paragraph are joined again; blank lines, list items ("1.", "(a)", "-"), rules
     * ("====") and table-like lines (several spaces inside) keep their own line.
     */
    fun reflow(text: String): String =
        text
            .replace("\r\n", "\n")
            .split(Regex("\n\\s*\n"))
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n") { paragraph ->
                val out = StringBuilder()
                var keepNext = false
                paragraph.lines().forEach { raw ->
                    val line = raw.trim()
                    val own = isRule(line) || isTable(raw.trim()) || isListItem(line)
                    when {
                        out.isEmpty() -> out.append(line)
                        own || keepNext -> out.append('\n').append(line)
                        else -> out.append(' ').append(line)
                    }
                    keepNext = isRule(line) || isTable(line)
                }
                out.toString()
            }

    private fun isRule(line: String) = line.length >= RULE && line.all { it in "=-_*~" }

    private fun isTable(line: String) = line.contains(TABLE_GAP)

    private fun isListItem(line: String) = LIST_ITEM.containsMatchIn(line)

    private fun entry(obj: JsonObject) =
        OssEntry(
            name = obj.string("name"),
            version = obj.string("version"),
            licence = obj.string("licence"),
            url = obj.string("url"),
            texts = (obj["texts"] as? JsonArray).orEmpty().map { it.jsonPrimitive.content },
        )

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.content.orEmpty()

    private const val RULE = 3
    private const val TABLE_GAP = "   "
    private val LIST_ITEM = Regex("^(\\(?[0-9]{1,2}[.)]|\\(?[a-z][.)]|\\([ivx]{1,4}\\)|[-*•])\\s")
}
