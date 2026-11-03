package app.hain.tools.internal.calc

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Thrown for an expression the calculator cannot read. */
class CalcException(
    message: String,
) : IllegalArgumentException(message)

/**
 * Own recursive-descent calculator (T-063, TOOLS.md §5): `+ - * / ^ ( )`, `%` after a number
 * (`15%` = 0.15), `sqrt sin cos tan log ln abs`, `pi e`, decimal comma (`3,5`), `×`/`÷`/`·` and a
 * number right before a bracket or name (`2pi`, `3(4+1)`).
 */
object Calculator {
    fun evaluate(expression: String): Double {
        val parser = Parser(tokenize(expression))
        val value = parser.expression()
        if (!parser.atEnd()) throw CalcException("Unexpected '${parser.peek()}'")
        if (value.isNaN() || value.isInfinite()) throw CalcException("No finite result")
        return value
    }

    private sealed interface Token {
        data class Num(
            val value: Double,
        ) : Token

        data class Name(
            val value: String,
        ) : Token

        data class Op(
            val value: Char,
        ) : Token
    }

    private const val OPERATORS = "+-*/^%()"

    @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements", "ComplexCondition")
    private fun tokenize(input: String): List<Token> {
        val text =
            input
                .replace('×', '*')
                .replace('·', '*')
                .replace('÷', '/')
                .replace('−', '-')
                .replace("π", "pi")
                .replace("√", "sqrt")
                .replace("**", "^")
        val out = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || ((c == '.' || c == ',') && i + 1 < text.length && text[i + 1].isDigit()) -> {
                    val start = i
                    while (i < text.length && (text[i].isDigit() || text[i] == '.' || text[i] == ',')) i++
                    val raw = text.substring(start, i).replace(',', '.')
                    val value = raw.toDoubleOrNull() ?: throw CalcException("Bad number '$raw'")
                    out += Token.Num(value)
                }
                c.isLetter() -> {
                    val start = i
                    while (i < text.length && text[i].isLetter()) i++
                    out += Token.Name(text.substring(start, i).lowercase())
                }
                c in OPERATORS -> {
                    out += Token.Op(c)
                    i++
                }
                else -> throw CalcException("Unknown sign '$c'")
            }
        }
        return out
    }

    @Suppress("TooManyFunctions") // one small function per grammar rule
    private class Parser(
        private val tokens: List<Token>,
    ) {
        private var pos = 0

        fun atEnd() = pos >= tokens.size

        fun peek(): Token? = tokens.getOrNull(pos)

        private fun isOp(c: Char) = (peek() as? Token.Op)?.value == c

        private fun take(): Token = tokens.getOrNull(pos++) ?: throw CalcException("Expression ends too early")

        fun expression(): Double {
            var value = term()
            while (isOp('+') || isOp('-')) {
                val op = (take() as Token.Op).value
                val right = term()
                value = if (op == '+') value + right else value - right
            }
            return value
        }

        private fun term(): Double {
            var value = unary()
            while (true) {
                value =
                    when {
                        isOp('*') -> {
                            take()
                            value * unary()
                        }
                        isOp('/') -> {
                            take()
                            val d = unary()
                            if (d == 0.0) throw CalcException("Division by zero")
                            value / d
                        }
                        // implicit multiplication: 2pi, 3(4+1), 2 sqrt(9)
                        peek() is Token.Name || isOp('(') -> value * unary()
                        else -> return value
                    }
            }
        }

        private fun unary(): Double =
            when {
                isOp('-') -> {
                    take()
                    -unary()
                }
                isOp('+') -> {
                    take()
                    unary()
                }
                else -> power()
            }

        private fun power(): Double {
            val base = postfix()
            if (isOp('^')) {
                take()
                return base.pow(unary())
            }
            return base
        }

        private fun postfix(): Double {
            var value = primary()
            while (isOp('%')) {
                take()
                value /= PERCENT
            }
            return value
        }

        private fun primary(): Double =
            when (val t = take()) {
                is Token.Num -> t.value
                is Token.Op ->
                    if (t.value == '(') {
                        val inner = expression()
                        if (!isOp(')')) throw CalcException("Missing ')'")
                        take()
                        inner
                    } else {
                        throw CalcException("Unexpected '${t.value}'")
                    }
                is Token.Name -> name(t.value)
            }

        private fun name(name: String): Double {
            CONSTANTS[name]?.let { return it }
            val f = FUNCTIONS[name] ?: throw CalcException("Unknown name '$name'")
            return f(postfix())
        }
    }

    private const val PERCENT = 100.0
    private val CONSTANTS = mapOf("pi" to Math.PI, "e" to Math.E)
    private val FUNCTIONS: Map<String, (Double) -> Double> =
        mapOf(
            "sqrt" to { x ->
                if (x < 0) throw CalcException("Square root of a negative number")
                sqrt(x)
            },
            "wurzel" to { x -> sqrt(x) },
            "sin" to { x -> sin(x) },
            "cos" to { x -> cos(x) },
            "tan" to { x -> tan(x) },
            "log" to { x -> log10(x) },
            "ln" to { x -> ln(x) },
            "abs" to { x -> abs(x) },
        )
}
