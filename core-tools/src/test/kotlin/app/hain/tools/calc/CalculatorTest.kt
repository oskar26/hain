package app.hain.tools.calc

import app.hain.tools.internal.calc.CalcException
import app.hain.tools.internal.calc.Calculator
import app.hain.tools.internal.calc.CalculatorTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorTest {
    private fun eval(s: String) = Calculator.evaluate(s)

    private fun check(
        expression: String,
        expected: Double,
    ) = assertEquals(expression, expected, eval(expression), 1e-9)

    @Test
    fun basics() {
        check("1+2", 3.0)
        check("2+3*4", 14.0)
        check("(2+3)*4", 20.0)
        check("10/4", 2.5)
        check("10 - 2 - 3", 5.0)
        check("2^10", 1024.0)
        check("2^3^2", 512.0)
        check("-2^2", -4.0)
        check("2^-1", 0.5)
        check("--3", 3.0)
        check("+5", 5.0)
    }

    @Test
    fun decimalsAndSigns() {
        check("3,5 * 2", 7.0)
        check("0.1 + 0.2", 0.3)
        check(",5 + 1", 1.5)
        check("12 × 7", 84.0)
        check("9 ÷ 3", 3.0)
        check("4 · 2", 8.0)
        check("7 − 2", 5.0)
        check("2**3", 8.0)
    }

    @Test
    fun percentFunctionsConstants() {
        check("15%", 0.15)
        check("80 * 15%", 12.0)
        check("sqrt(16)", 4.0)
        check("√9", 3.0)
        check("sqrt 25", 5.0)
        check("abs(-3)", 3.0)
        check("log(1000)", 3.0)
        check("ln(e)", 1.0)
        check("sin(0)", 0.0)
        check("cos(0)", 1.0)
        check("tan(0)", 0.0)
        check("pi", Math.PI)
        check("2pi", 2 * Math.PI)
        check("2π", 2 * Math.PI)
        check("3(4+1)", 15.0)
    }

    @Test
    fun errors() {
        assertThrows(CalcException::class.java) { eval("1/0") }
        assertThrows(CalcException::class.java) { eval("2+") }
        assertThrows(CalcException::class.java) { eval("(1+2") }
        assertThrows(CalcException::class.java) { eval("foo(2)") }
        assertThrows(CalcException::class.java) { eval("sqrt(-1)") }
        assertThrows(CalcException::class.java) { eval("1 # 2") }
    }

    @Test
    fun formatting() {
        assertEquals("0,3", CalculatorTool.format(0.1 + 0.2, "de"))
        assertEquals("0.3", CalculatorTool.format(0.1 + 0.2, "en"))
        assertEquals("1024", CalculatorTool.format(1024.0, "en"))
        assertEquals("3,141592654", CalculatorTool.format(Math.PI, "de"))
        assertEquals("0", CalculatorTool.format(-0.0, "en"))
    }

    @Test
    fun directAnswerOnlyForPureSums() {
        assertEquals("12*7 = 84", CalculatorTool.directAnswer("12*7", "de"))
        assertEquals("3,5*2 = 7", CalculatorTool.directAnswer("3,5*2 =", "de"))
        assertEquals("5 km = 5000 m", CalculatorTool.directAnswer("5 km in m", "en"))
        assertNull(CalculatorTool.directAnswer("Was ist 2+2?", "de"))
        assertNull(CalculatorTool.directAnswer("Hallo", "de"))
        assertNull(CalculatorTool.directAnswer("42", "de"))
        assertNull(CalculatorTool.directAnswer("Wie wird das Wetter in Köln?", "de"))
    }
}
