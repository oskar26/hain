package app.hain.models.internal

import app.hain.models.api.ModelFile
import app.hain.models.api.ModelInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/** Reads `assets/catalog.json` (MODELS.md). */
object CatalogParser {
    private val SHA256 = Regex("^[0-9a-f]{64}$")

    fun parse(json: String): List<ModelInfo> =
        Json
            .parseToJsonElement(json)
            .jsonObject
            .getValue("models")
            .jsonArray
            .map { it.jsonObject.toModel() }

    private fun JsonObject.toModel(): ModelInfo {
        val files =
            getValue("files").jsonArray.map { file ->
                val o = file.jsonObject
                val sha = o.string("sha256")
                require(SHA256.matches(sha)) { "bad sha256 for ${string("id")}" }
                ModelFile(url = o.string("url"), sha256 = sha, size = o.getValue("size").jsonPrimitive.long)
            }
        val minRam = get("min_ram_gb")?.jsonPrimitive?.int ?: 0
        return ModelInfo(
            id = string("id"),
            name = string("name"),
            kind = string("kind"),
            files = files,
            minRamGb = minRam,
            licence = string("licence"),
            licenceUrl = string("licence_url"),
            isDefault = get("default")?.jsonPrimitive?.boolean ?: false,
            lang = get("lang")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
            tier = get("tier")?.jsonPrimitive?.content ?: ModelInfo.TIER_STANDARD,
            recommendRamGb = get("recommend_ram_gb")?.jsonPrimitive?.int ?: minRam,
        )
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
}
