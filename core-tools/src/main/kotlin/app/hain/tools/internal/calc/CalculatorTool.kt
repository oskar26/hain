package app.hain.tools.internal.calc

import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Args
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.math.BigDecimal
import java.math.MathContext

/** `calculate` (T-063): maths and unit conversions, fully offline. */
class CalculatorTool : Tool {
    override val online = false
    override val schema =
        ToolSchema(
            name = "calculate",
            description = "Exact maths and unit conversion, e.g. '12*7', '15% * 80', '5 km in mi', '70 F in C'.",
            parameters =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("expression") {
                            put("type", "string")
                            put("description", "The expression or conversion")
                        }
                    }
                    putJsonArray("required") { add(JsonPrimitive("expression")) }
                },
        )

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val expression = Args.required(args, "expression")
        val (result, shown) = compute(expression, ctx.lang)
        val json = JsonObject(mapOf("expression" to JsonPrimitive(expression), "result" to JsonPrimitive(result)))
        return ToolResult(forModel = json.toString(), label = shown)
    }

    companion object {
        private const val DIGITS = 10
        private val EXPRESSION_ONLY = Regex("^[\\d\\s.,+\\-*/^%()×÷·−πa-z√]+$", RegexOption.IGNORE_CASE)
        private val HAS_OPERATOR = Regex("[+\\-*/^%×÷·−√]|sqrt|wurzel|sin|cos|tan|log|ln")

        /** (result for the brain, short line for the chat). Throws [CalcException] on bad input. */
        fun compute(
            expression: String,
            lang: String,
        ): Pair<String, String> {
            val clean =
                expression
                    .trim()
                    .removeSuffix("=")
                    .removeSuffix("?")
                    .trim()
            val conversion = Units.parse(clean)
            if (conversion != null) {
                val text = "${format(conversion.result, "en")} ${conversion.to.symbol}"
                val shown =
                    "${format(conversion.value, lang)} ${conversion.from.symbol} = " +
                        "${format(conversion.result, lang)} ${conversion.to.symbol}"
                return text to shown
            }
            val value = Calculator.evaluate(clean)
            return format(value, "en") to "$clean = ${format(value, lang)}"
        }

        /**
         * The whole message is a sum like "12*7" or "5 km in m": answer at once, without the brain
         * (TOOLS.md §5). `null` when it is not, or cannot be computed.
         */
        fun directAnswer(
            message: String,
            lang: String,
        ): String? {
            val clean =
                message
                    .trim()
                    .removeSuffix("=")
                    .removeSuffix("?")
                    .trim()
            val isSum =
                clean.any { it.isDigit() } &&
                    EXPRESSION_ONLY.matches(clean) &&
                    HAS_OPERATOR.containsMatchIn(clean) &&
                    runCatching { Calculator.evaluate(clean) }.isSuccess
            val isConversion = clean.any { it.isDigit() } && Units.parse(clean) != null
            if (!isSum && !isConversion) return null
            return runCatching { compute(clean, lang).second }.getOrNull()
        }

        /** At most 10 significant digits, no trailing zeros; decimal comma in German. */
        fun format(
            value: Double,
            lang: String,
        ): String {
            val rounded = BigDecimal(value).round(MathContext(DIGITS)).stripTrailingZeros()
            val text = if (rounded.compareTo(BigDecimal.ZERO) == 0) "0" else rounded.toPlainString()
            return if (lang == "de") text.replace('.', ',') else text
        }
    }
}
