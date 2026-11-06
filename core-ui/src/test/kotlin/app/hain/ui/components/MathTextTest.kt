package app.hain.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-288: formulas as LFM3 writes them, made readable without raw LaTeX. */
class MathTextTest {
    @Test
    fun textWithoutMathIsUntouched() {
        val text = "Hallo **Welt**, das kostet 5 € und *nicht* mehr."
        assertEquals(text, MathText.prepare(text))
    }

    @Test
    fun pricesWithDollarStayText() {
        val text = "Das kostet $5 und später $10."
        assertEquals(text, MathText.prepare(text))
        val spaced = "Zwischen 5 $ und 10 $ pro Monat."
        assertEquals(spaced, MathText.prepare(spaced))
    }

    @Test
    fun inlinePowerAndFraction() {
        val out = MathText.prepare("Die Fläche ist \$A = \\frac{1}{2} g \\cdot h\$.")
        assertEquals("Die Fläche ist A = 1/2 g · h.", out)
    }

    @Test
    fun pythagorasInParentheses() {
        assertEquals("Satz: a² + b² = c²", MathText.prepare("Satz: \\(a^2 + b^2 = c^2\\)"))
    }

    @Test
    fun quadraticFormulaAsDisplayBlock() {
        val lfm =
            "Die Lösungsformel lautet:\n\n\$\$x_{1,2} = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}\$\$\n\n" +
                "Dabei ist a ≠ 0."
        val out = MathText.prepare(lfm)
        assertTrue(out, out.contains("```hain-math\nx₁,₂ = (−b ± √(b² − 4ac))/(2a)\n```"))
        assertTrue(out.startsWith("Die Lösungsformel lautet:"))
        assertTrue(out.endsWith("Dabei ist a ≠ 0."))
        assertFalse(out.contains("\\frac"))
    }

    @Test
    fun bracketDisplayMath() {
        val out = MathText.prepare("So:\n\\[ E = mc^2 \\]\nfertig")
        assertEquals("So:\n```hain-math\nE = mc²\n```\nfertig", out)
    }

    @Test
    fun indicesDoNotBecomeItalics() {
        // a_1 … b_1 used to turn into italics in the renderer
        val out = MathText.prepare("Wenn \$a_1 = 3\$ und \$b_1 = 4\$ sind")
        assertEquals("Wenn a₁ = 3 und b₁ = 4 sind", out)
    }

    @Test
    fun scriptsWithoutUnicodeFallBackToCaret() {
        assertEquals("e^(iπ) + 1 = 0", Latex.toUnicode("e^{i\\pi} + 1 = 0"))
        assertEquals("2⁻¹", Latex.toUnicode("2^{-1}"))
        assertEquals("xⁿ⁺¹", Latex.toUnicode("x^{n+1}"))
    }

    @Test
    fun greekRelationsAndArrows() {
        assertEquals("α ≤ β → γ ≈ π", Latex.toUnicode("\\alpha \\le \\beta \\to \\gamma \\approx \\pi"))
        assertEquals("3 × 4 = 12", Latex.toUnicode("3 \\times 4 = 12"))
        assertEquals("x ∈ ℝ", Latex.toUnicode("x \\in \\mathbb{R}"))
    }

    @Test
    fun rootsAndText() {
        assertEquals("√2 ≈ 1,41", Latex.toUnicode("\\sqrt{2} \\approx 1{,}41"))
        assertEquals("∛8 = 2", Latex.toUnicode("\\sqrt[3]{8} = 2"))
        assertEquals("v = 20 m/s", Latex.toUnicode("v = 20 \\text{ m/s}"))
        assertEquals("v = 20 m/s", Latex.toUnicode("v = 20\\,\\mathrm{m/s}"))
    }

    @Test
    fun degreesAndLeftRight() {
        assertEquals("90°", Latex.toUnicode("90^\\circ"))
        assertEquals("(a + b)²", Latex.toUnicode("\\left(a + b\\right)^2"))
    }

    @Test
    fun sumAndIntegral() {
        assertEquals("∑ᵢ₌₁ⁿ i = n(n + 1)/2", Latex.toUnicode("\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}"))
        assertEquals("∫₀¹ x² dx = 1/3", Latex.toUnicode("\\int_0^1 x^2 \\, dx = \\frac{1}{3}"))
    }

    @Test
    fun functionsKeepTheirNames() {
        assertEquals("sin² x + cos² x = 1", Latex.toUnicode("\\sin^2 x + \\cos^2 x = 1"))
    }

    @Test
    fun unknownCommandLosesBackslashOnly() {
        assertEquals("foo(x)", Latex.toUnicode("\\foo(x)"))
    }

    @Test
    fun casesEnvironmentBecomesLines() {
        val out = Latex.toUnicode("f(x) = \\begin{cases} 1 & x > 0 \\\\ 0 & x \\le 0 \\end{cases}")
        assertEquals("f(x) =\n1 x > 0\n0 x ≤ 0", out)
    }

    @Test
    fun codeIsNeverTouched() {
        val text = "Code:\n```python\nprint(\"\$x^2\$\")\n```\nund `\$PATH`"
        assertEquals(text, MathText.prepare(text))
    }

    @Test
    fun openDisplayFormulaWhileStreaming() {
        val out = MathText.prepare("Formel:\n\$\$\\frac{a}{b")
        assertEquals("Formel:\n```hain-math\na/b\n```\n", out)
    }

    @Test
    fun escapedDollarStays() {
        assertEquals("Preis \\$5", MathText.prepare("Preis \\$5"))
    }

    @Test
    fun inlineFormulaCharactersAreEscaped() {
        assertEquals("a\\*b", MathText.escapeMarkdown("a*b"))
        assertEquals("x \\_ y", MathText.escapeMarkdown("x _ y"))
    }

    @Test
    fun displayInsideSentenceStartsOnOwnLine() {
        val out = MathText.prepare("Also \$\$a=b\$\$ gilt.")
        assertEquals("Also \n```hain-math\na = b\n```\n gilt.", out)
    }

    /** Oskar's screenshot 06.11. 13:14 (LFM3, English): exactly this text. */
    private val oskar =
        "2. \\(2250000a + 1500b + 760 = 2560\\) → \\(2250000a + 1500b = 1800\\) → divide by 300: " +
            "\\(7500a + 5b = 6\\) \\quad (Equation B)\n\n" +
            "Now solve the two-variable linear system:\n" +
            "From (A): \\(b = 1 - 500a\\).\n" +
            "Plug into (B):\n" +
            "\\(7500a + 5(1 - 500a) = 6\\)\n" +
            "→ \\(7500a + 5 - 2500a = 6\\)\n" +
            "→ \\((7500-2500)a = 6 - 5\\) → \\(5000a = 1\\) → \\(a =\n\\frac{1}{5000} = 0.0002\\).\n" +
            "Then \\(b = 1 - 500(0.0002) = 1 - 0.1 = 0.9\\).\n" +
            "So the quadratic is:\n" +
            "\\[ K(x) = 0.0002\\,x^{2} + 0.9\\,x + 760. \\]\n" +
            "You can verify:\n\n" +
            "- \\(K(500)=0.0002\\cdot250000 + 0.9\\cdot500 + 760 = 50 + 450 + 760 = 1260\\) ✓\n" +
            "- \\(K(1500)=0.0002\\cdot2{,}250{,}000 + 0.9\\cdot1500 + 760 = 450 + 1350 + 760 = 2560\\) ✓\n\n" +
            "If you want to plot this in Geogebra, use the **Equation** tool: `0.0002*x^2 + 0.9*x + 760 = y`"

    @Test
    fun oskarsScreenshotHasNoRawLatex() {
        val out = MathText.prepare(oskar)
        val prose = out.replace(Regex("`[^`]*`"), "")
        listOf("\\frac", "\\(", "\\)", "\\[", "\\]", "\\,", "\\cdot", "^{", "\\quad").forEach {
            assertFalse("still raw: $it in\n$out", prose.contains(it))
        }
        assertTrue(out, out.contains("2250000a + 1500b + 760 = 2560 →"))
        assertTrue(out, out.contains("a = 1/5000 = 0.0002"))
        assertTrue(out, out.contains("```hain-math\nK(x) = 0.0002 x² + 0.9 x + 760.\n```"))
        assertTrue(out, out.contains("K(500) = 0.0002·250000 + 0.9·500"))
        assertTrue(out, out.contains("0.0002·2,250,000"))
        // code stays code
        assertTrue(out.contains("`0.0002*x^2 + 0.9*x + 760 = y`"))
        assertTrue(out.contains("**Equation**"))
    }

    @Test
    fun openInlineFormulaWhileStreamingDoesNotFlicker() {
        assertEquals("Also a = 1/5000", MathText.prepare("Also \\(a = \\frac{1}{5000}"))
    }
}
