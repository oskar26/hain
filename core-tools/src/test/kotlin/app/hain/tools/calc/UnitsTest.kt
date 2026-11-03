package app.hain.tools.calc

import app.hain.tools.internal.calc.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {
    private fun convert(text: String): Double {
        val c = Units.parse(text)
        assertNotNull(text, c)
        return c!!.result
    }

    @Test
    fun lengthMassVolume() {
        assertEquals(5000.0, convert("5 km in m"), 1e-9)
        assertEquals(2.54, convert("1 inch in cm"), 1e-9)
        assertEquals(1.609344, convert("1 mi to km"), 1e-9)
        assertEquals(7.71617918, convert("3,5 kg in lb"), 1e-6)
        assertEquals(500.0, convert("0,5 l in ml"), 1e-9)
        assertEquals(250.0, convert("250g in g"), 1e-9)
    }

    @Test
    fun temperature() {
        assertEquals(37.7777777, convert("100 °F in °C"), 1e-6)
        assertEquals(212.0, convert("100 C to F"), 1e-9)
        assertEquals(273.15, convert("0 celsius in kelvin"), 1e-9)
    }

    @Test
    fun speedTimeData() {
        assertEquals(100.0, convert("27,7777778 m/s in km/h"), 1e-5)
        assertEquals(90.0, convert("1,5 h in min"), 1e-9)
        assertEquals(1000.0, convert("1 GB in MB"), 1e-9)
        assertEquals(1024.0, convert("1 MiB in KiB"), 1e-9)
        assertEquals(2.0, convert("2 ha in hektar"), 1e-9)
    }

    @Test
    fun expressionAsAmount() {
        assertEquals(6000.0, convert("2*3 km in m"), 1e-9)
    }

    @Test
    fun notAConversion() {
        assertNull(Units.parse("5 km in kg"))
        assertNull(Units.parse("Wetter in Köln"))
        assertNull(Units.parse("5 km"))
    }
}
