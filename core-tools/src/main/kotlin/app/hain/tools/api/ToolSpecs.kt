package app.hain.tools.api

import app.hain.llm.api.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * One parameter of a tool (4d, T-182): flat, a short description with an example, an [enum] where
 * the value is fixed and [aliases] for enum values the brain writes in its own words ("zu Fuß").
 */
data class ParamSpec(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = false,
    val enum: List<String> = emptyList(),
    val aliases: Map<String, String> = emptyMap(),
)

/**
 * A tool as the brain sees it, read from `tools.json` (one file for the app and the host eval).
 * [names] are wrong tool names the small brain used, [aliases] wrong parameter names; both are
 * mapped by [ToolCallFixer].
 */
data class ToolSpec(
    val name: String,
    val description: String,
    val params: List<ParamSpec>,
    val names: List<String> = emptyList(),
    val aliases: Map<String, String> = emptyMap(),
) {
    val required: List<String> get() = params.filter { it.required }.map { it.name }

    fun param(name: String): ParamSpec? = params.firstOrNull { it.name == name }

    /** The JSON schema line for `<tools>`: type, enum, description; `required` last. */
    val schema: ToolSchema
        get() =
            ToolSchema(
                name = name,
                description = description,
                parameters =
                    buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            params.forEach { p ->
                                putJsonObject(p.name) {
                                    put("type", p.type)
                                    if (p.enum.isNotEmpty()) put("enum", JsonArray(p.enum.map { JsonPrimitive(it) }))
                                    put("description", p.description)
                                }
                            }
                        }
                        putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
                    },
            )
}

/** Loads `app/hain/tools/tools.json` from the module's resources once. */
object ToolSpecs {
    private const val RESOURCE = "/app/hain/tools/tools.json"

    val all: Map<String, ToolSpec> by lazy {
        val text =
            requireNotNull(ToolSpecs::class.java.getResourceAsStream(RESOURCE)) { "missing $RESOURCE" }
                .bufferedReader()
                .use { it.readText() }
        parse(text).associateBy { it.name }
    }

    /** The spec of [name]; every tool in [HainTools] has one (checked by a unit test). */
    fun of(name: String): ToolSpec = requireNotNull(all[name]) { "no spec for tool $name" }

    fun parse(text: String): List<ToolSpec> =
        Json.parseToJsonElement(text).jsonObject["tools"]!!.jsonArray.map { element ->
            val o = element.jsonObject
            ToolSpec(
                name = o.str("name"),
                description = o.str("description"),
                params = o["params"]?.jsonArray.orEmpty().map { param(it.jsonObject) },
                names = o.list("names"),
                aliases = o.map("aliases"),
            )
        }

    private fun param(o: JsonObject) =
        ParamSpec(
            name = o.str("name"),
            type = o.str("type"),
            description = o.str("description"),
            required = o["required"]?.jsonPrimitive?.booleanOrNull ?: false,
            enum = o.list("enum"),
            aliases = o.map("aliases"),
        )

    private fun JsonObject.str(key: String): String = requireNotNull(this[key]?.jsonPrimitive?.content) { "missing $key" }

    private fun JsonObject.list(key: String): List<String> = (this[key] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()

    private fun JsonObject.map(key: String): Map<String, String> =
        (this[key] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
}
