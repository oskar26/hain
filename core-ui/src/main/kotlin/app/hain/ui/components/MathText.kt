package app.hain.ui.components

/**
 * T-288 (Oskar 06.11.: "Mathe-Formeln werden falsch angezeigt"): LFM3 writes formulas in LaTeX
 * (`$x^2$`, `$$\frac{a}{b}$$`, `\( … \)`, `\[ … \]`). The markdown renderer knows no LaTeX, so the
 * chat showed `\frac{a}{b}`, lost backslashes and turned `a_1 … b_2` into italics.
 *
 * Before the answer goes to the renderer, [prepare] turns every formula outside code into readable
 * Unicode ([Latex.toUnicode]): an inline formula stays in the sentence (markdown characters escaped),
 * a display formula becomes its own ```` ```hain-math ```` block that [AssistantBlock] draws centred
 * in a formula font. No library (none fitting and GPL-compatible for Compose without a WebView). Pure, tested.
 */
object MathText {
    /** Fence language of a display formula block. */
    const val FENCE_LANG = "hain-math"

    /** Markdown with every formula outside code made readable; text without formulas comes back as it is. */
    fun prepare(markdown: String): String {
        if (!mayHaveMath(markdown)) return markdown
        val out = StringBuilder(markdown.length + EXTRA)
        var inFence = false
        val lines = markdown.split('\n')
        var i = 0
        val prose = StringBuilder()

        fun flushProse() {
            if (prose.isNotEmpty()) {
                out.append(convertProse(prose.toString()))
                prose.setLength(0)
            }
        }
        while (i < lines.size) {
            val line = lines[i]
            val fence = FENCE.matches(line)
            if (fence || inFence) {
                flushProse()
                out.append(line)
                if (i < lines.size - 1) out.append('\n')
                if (fence) inFence = !inFence
            } else {
                prose.append(line)
                if (i < lines.size - 1) prose.append('\n')
            }
            i++
        }
        flushProse()
        return out.toString()
    }

    private fun mayHaveMath(text: String): Boolean = text.contains('$') || text.contains("\\(") || text.contains("\\[")

    /** Prose (no fenced code): display formulas first, then inline ones, inline code untouched. */
    @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements", "NestedBlockDepth", "LongMethod")
    private fun convertProse(text: String): String {
        val out = StringBuilder(text.length + EXTRA)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '`' -> {
                    // inline code: copy up to the matching run of backticks
                    val run = countRun(text, i, '`')
                    val close = text.indexOf("`".repeat(run), i + run)
                    val end = if (close < 0) text.length else close + run
                    out.append(text, i, end)
                    i = end
                }
                // a spacing command the model wrote between two formulas ("\(…\) \quad (B)")
                text.startsWith("\\qquad", i) || text.startsWith("\\quad", i) -> {
                    out.append("  ")
                    i += if (text.startsWith("\\qquad", i)) QQUAD_LEN else QUAD_LEN
                }
                c == '\\' && i + 1 < text.length && text[i + 1] == '$' -> {
                    out.append("\\$")
                    i += 2
                }
                text.startsWith("$$", i) || text.startsWith("\\[", i) -> {
                    val closer = if (text[i] == '$') "$$" else "\\]"
                    val close = text.indexOf(closer, i + 2)
                    // still streaming: an open display formula runs to the end
                    val inner = if (close < 0) text.substring(i + 2) else text.substring(i + 2, close)
                    out.append(displayBlock(inner, out))
                    i = if (close < 0) text.length else close + 2
                    // the line break after the block is part of the block
                    if (i < text.length && text[i] == '\n') i++
                }
                text.startsWith("\\(", i) -> {
                    val close = text.indexOf("\\)", i + 2)
                    if (close < 0 && text.length - i <= OPEN_INLINE_MAX) {
                        // still streaming: the open formula is shown as a formula already (no flicker)
                        out.append(inline(text.substring(i + 2)))
                        i = text.length
                    } else if (close < 0) {
                        out.append(c)
                        i++
                    } else {
                        out.append(inline(text.substring(i + 2, close)))
                        i = close + 2
                    }
                }
                c == '$' -> {
                    val close = inlineDollarEnd(text, i)
                    if (close < 0) {
                        out.append(c)
                        i++
                    } else {
                        out.append(inline(text.substring(i + 1, close)))
                        i = close + 1
                    }
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    /**
     * End of an inline `$…$` formula that starts at [start], or -1 (Pandoc's rule, so prices stay text:
     * no space after the opening `$`, none before the closing one, no digit right after it, one line).
     */
    @Suppress("ReturnCount")
    private fun inlineDollarEnd(
        text: String,
        start: Int,
    ): Int {
        val first = text.getOrNull(start + 1) ?: return -1
        if (first.isWhitespace() || first == '$') return -1
        var j = start + 1
        while (j < text.length) {
            val ch = text[j]
            if (ch == '\n') return -1
            if (ch == '\\') {
                j += 2
                continue
            }
            if (ch == '$') {
                val before = text[j - 1]
                val after = text.getOrNull(j + 1)
                if (before.isWhitespace() || (after != null && after.isDigit())) return -1
                return j
            }
            j++
        }
        return -1
    }

    private fun countRun(
        text: String,
        at: Int,
        ch: Char,
    ): Int {
        var n = 0
        while (at + n < text.length && text[at + n] == ch) n++
        return n
    }

    private fun inline(latex: String): String = escapeMarkdown(Latex.toUnicode(latex).replace('\n', ' '))

    private fun displayBlock(
        latex: String,
        before: CharSequence,
    ): String {
        val body =
            Latex
                .toUnicode(latex)
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        if (body.isEmpty()) return ""
        val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
        return lead + "```" + FENCE_LANG + "\n" + body.joinToString("\n") + "\n```\n"
    }

    /** A formula inside a sentence must not start emphasis, links or code in the renderer. */
    internal fun escapeMarkdown(text: String): String =
        buildString(text.length + EXTRA) {
            text.forEach { ch ->
                if (ch in MARKDOWN_SPECIAL) append('\\')
                append(ch)
            }
        }

    private val FENCE = Regex("^\\s{0,3}(```|~~~).*")
    private const val MARKDOWN_SPECIAL = "\\`*_[]<>#|~"
    private const val EXTRA = 16

    /** An open `\(` this close to the end is a formula still being written. */
    private const val OPEN_INLINE_MAX = 160
    private const val QUAD_LEN = 5
    private const val QQUAD_LEN = 6
}

/**
 * The common LaTeX of school maths as plain Unicode: fractions, roots, powers, indices, Greek
 * letters, operators and relations, `\text{}`, `\left( … \right)`, simple environments.
 * Unknown commands lose their backslash, so nothing breaks; braces are removed. Pure, tested.
 */
object Latex {
    fun toUnicode(latex: String): String {
        val parser = Parser(latex.trim())
        return tidy(parser.parseAll())
    }

    /** Spaces around relations and a single space elsewhere. */
    private fun tidy(text: String): String =
        text
            .replace(Regex("[ \\t]+"), " ")
            // "sin ²" → "sin²": a power or index sticks to what it belongs to
            .replace(Regex(" +([⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁿⁱ₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎]+)"), "$1")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\( "), "(")
            .replace(Regex(" \\)"), ")")
            .trim()

    @Suppress("TooManyFunctions") // one small function per LaTeX construct
    private class Parser(
        private val s: String,
    ) {
        private var i = 0

        fun parseAll(): String {
            val out = StringBuilder()
            while (i < s.length) out.append(atom())
            return out.toString()
        }

        /** One piece: a command, a group, a script or a plain character. */
        @Suppress("CyclomaticComplexMethod")
        private fun atom(): String {
            val c = s[i]
            return when (c) {
                '\\' -> command()
                '{' -> group()
                '}' -> {
                    i++
                    ""
                }
                '^' -> {
                    i++
                    script(argument(), SUPERSCRIPT, "^")
                }
                '_' -> {
                    i++
                    script(argument(), SUBSCRIPT, "_")
                }
                '&' -> {
                    i++
                    " "
                }
                '~' -> {
                    i++
                    " "
                }
                '*' -> {
                    i++
                    "·"
                }
                '-' -> {
                    i++
                    "−"
                }
                '=', '<', '>', '+' -> {
                    i++
                    " $c "
                }
                else -> {
                    i++
                    c.toString()
                }
            }
        }

        /** `{…}` as text. */
        private fun group(): String {
            i++ // {
            val out = StringBuilder()
            var depth = 1
            while (i < s.length) {
                when (s[i]) {
                    '{' -> {
                        depth++
                        out.append(group())
                        depth--
                        continue
                    }
                    '}' -> {
                        i++
                        return out.toString()
                    }
                    else -> out.append(atom())
                }
            }
            return out.toString()
        }

        /** The argument of a command or script: a group, a command or one character. */
        private fun argument(): String {
            while (i < s.length && s[i] == ' ') i++
            if (i >= s.length) return ""
            return when (s[i]) {
                '{' -> group()
                '\\' -> command()
                else -> s[i++].toString()
            }
        }

        /** Raw text of a `{…}` argument without parsing (for `\text`, `\begin`). */
        @Suppress("ReturnCount")
        private fun rawArgument(): String {
            while (i < s.length && s[i] == ' ') i++
            if (i >= s.length || s[i] != '{') return ""
            var depth = 0
            val start = i
            while (i < s.length) {
                when (s[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            i++
                            return s.substring(start + 1, i - 1)
                        }
                    }
                }
                i++
            }
            return s.substring(start + 1)
        }

        @Suppress("ReturnCount")
        private fun optional(): String? {
            if (i >= s.length || s[i] != '[') return null
            val close = s.indexOf(']', i)
            if (close < 0) return null
            val inner = s.substring(i + 1, close)
            i = close + 1
            return Latex.toUnicode(inner)
        }

        @Suppress("CyclomaticComplexMethod", "LongMethod", "ReturnCount")
        private fun command(): String {
            i++ // backslash
            if (i >= s.length) return ""
            val first = s[i]
            if (!first.isLetter()) {
                i++
                return when (first) {
                    ',', ':', ';', ' ' -> " "
                    '!' -> ""
                    '\\' -> "\n"
                    '{', '}', '%', '$', '#', '&', '_' -> first.toString()
                    '|' -> "‖"
                    else -> first.toString()
                }
            }
            val start = i
            while (i < s.length && s[i].isLetter()) i++
            val name = s.substring(start, i)
            SYMBOLS[name]?.let { return it }
            return when (name) {
                "frac", "dfrac", "tfrac", "cfrac" -> fraction(argument(), argument())
                "sqrt" -> {
                    val index = optional()
                    val body = argument()
                    val root =
                        when (index) {
                            null -> "√"
                            "3" -> "∛"
                            "4" -> "∜"
                            else -> script(index, SUPERSCRIPT, "") + "√"
                        }
                    root + wrapIfNeeded(body)
                }
                "text", "textrm", "textit", "textbf", "mathrm", "mathit", "mathbf", "mathsf", "mathtt",
                "operatorname", "mbox", "boldsymbol", "bm", "emph",
                -> {
                    if (name.startsWith("text") || name == "mbox") rawArgument() else argument()
                }
                "mathbb" -> argument().map { BLACKBOARD[it] ?: it.toString() }.joinToString("")
                "vec" -> argument() + "⃗"
                "bar", "overline" -> argument() + "̅"
                "hat" -> argument() + "̂"
                "dot" -> argument() + "̇"
                "tilde" -> argument() + "̃"
                "left", "right", "big", "Big", "bigg", "Bigg", "bigl", "bigr", "Bigl", "Bigr" -> {
                    // the delimiter that follows is drawn as itself; "." means none
                    if (i < s.length && s[i] == '.') i++
                    ""
                }
                "begin" -> {
                    rawArgument()
                    if (i < s.length && s[i] == '{') rawArgument() // column spec of array
                    "\n"
                }
                "end" -> {
                    rawArgument()
                    "\n"
                }
                "displaystyle", "textstyle", "limits", "nolimits", "quad", "qquad" ->
                    if (name.endsWith("quad")) "  " else ""
                "binom" -> {
                    val n = argument()
                    val k = argument()
                    "C($n, $k)"
                }
                "pmod" -> " (mod ${argument()})"
                in FUNCTIONS -> "$name "
                else -> name
            }
        }

        private fun fraction(
            top: String,
            bottom: String,
        ): String = wrapIfNeeded(top) + "/" + wrapBottom(bottom)
    }

    /** `a + b` gets brackets as the top of a fraction or under a root; `2`, `2x` and `n(n + 1)` do not. */
    internal fun wrapIfNeeded(text: String): String {
        val t = text.trim()
        return if (hasTopLevelOperator(t)) "($t)" else t
    }

    /** Below the fraction bar also `2a` gets brackets (`1/2a` would read as `a/2`); `2`, `x²` do not. */
    internal fun wrapBottom(text: String): String {
        val t = text.trim()
        val core = t.filterNot { it in SCRIPT_CHARS }
        val single = core.length <= 1 || core.all { it.isDigit() || it == ',' || it == '.' }
        return if (hasTopLevelOperator(t) || !single) "($t)" else t
    }

    private fun hasTopLevelOperator(t: String): Boolean {
        var depth = 0
        t.forEach { ch ->
            when (ch) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                else -> if (depth == 0 && ch in TOP_LEVEL_OPERATORS) return true
            }
        }
        return false
    }

    private const val TOP_LEVEL_OPERATORS = " +−-±∓×÷=<>≤≥≠≈"

    /** Superscript/subscript in Unicode when every character has one, else `^(…)` / `_(…)`. */
    @Suppress("ReturnCount")
    internal fun script(
        text: String,
        map: Map<Char, Char>,
        fallback: String,
    ): String {
        val t = text.replace(" ", "")
        if (t.isEmpty()) return ""
        if (t == "∘" && map === SUPERSCRIPT) return "°"
        if (t == "′" || t == "′′") return t
        // "x_{1,2}": a comma between indices stays a comma
        val mapped = t.map { if (it == ',' && map === SUBSCRIPT) ',' else map[it] }
        if (mapped.all { it != null }) return mapped.joinToString("")
        return if (t.length == 1) "$fallback$t" else "$fallback($t)"
    }

    private val SCRIPT_CHARS: Set<Char> =
        "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁿⁱ₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎ₐₑₒₓᵢⱼₖₙₘₜ".toSet()

    internal val SUPERSCRIPT: Map<Char, Char> =
        mapOf(
            '0' to '⁰',
            '1' to '¹',
            '2' to '²',
            '3' to '³',
            '4' to '⁴',
            '5' to '⁵',
            '6' to '⁶',
            '7' to '⁷',
            '8' to '⁸',
            '9' to '⁹',
            '+' to '⁺',
            '-' to '⁻',
            '−' to '⁻',
            '=' to '⁼',
            '(' to '⁽',
            ')' to '⁾',
            'n' to 'ⁿ',
            'i' to 'ⁱ',
            'x' to 'ˣ',
            'a' to 'ᵃ',
            'b' to 'ᵇ',
            'c' to 'ᶜ',
            'd' to 'ᵈ',
            'e' to 'ᵉ',
            'k' to 'ᵏ',
            'm' to 'ᵐ',
            't' to 'ᵗ',
            'T' to 'ᵀ',
        )

    internal val SUBSCRIPT: Map<Char, Char> =
        mapOf(
            '0' to '₀',
            '1' to '₁',
            '2' to '₂',
            '3' to '₃',
            '4' to '₄',
            '5' to '₅',
            '6' to '₆',
            '7' to '₇',
            '8' to '₈',
            '9' to '₉',
            '+' to '₊',
            '-' to '₋',
            '−' to '₋',
            '=' to '₌',
            '(' to '₍',
            ')' to '₎',
            'a' to 'ₐ',
            'e' to 'ₑ',
            'o' to 'ₒ',
            'x' to 'ₓ',
            'i' to 'ᵢ',
            'j' to 'ⱼ',
            'k' to 'ₖ',
            'n' to 'ₙ',
            'm' to 'ₘ',
            't' to 'ₜ',
            'h' to 'ₕ',
            'l' to 'ₗ',
            'p' to 'ₚ',
            's' to 'ₛ',
            'r' to 'ᵣ',
            'u' to 'ᵤ',
            'v' to 'ᵥ',
        )

    private val BLACKBOARD: Map<Char, String> =
        mapOf('R' to "ℝ", 'N' to "ℕ", 'Z' to "ℤ", 'Q' to "ℚ", 'C' to "ℂ", 'P' to "ℙ")

    private val FUNCTIONS =
        setOf(
            "sin",
            "cos",
            "tan",
            "cot",
            "sec",
            "csc",
            "arcsin",
            "arccos",
            "arctan",
            "sinh",
            "cosh",
            "tanh",
            "log",
            "ln",
            "lg",
            "exp",
            "lim",
            "max",
            "min",
            "sup",
            "inf",
            "det",
            "gcd",
            "deg",
            "dim",
            "ker",
            "mod",
        )

    private val SYMBOLS: Map<String, String> =
        mapOf(
            // Greek
            "alpha" to "α",
            "beta" to "β",
            "gamma" to "γ",
            "delta" to "δ",
            "epsilon" to "ε",
            "varepsilon" to "ε",
            "zeta" to "ζ",
            "eta" to "η",
            "theta" to "θ",
            "vartheta" to "ϑ",
            "iota" to "ι",
            "kappa" to "κ",
            "lambda" to "λ",
            "mu" to "μ",
            "nu" to "ν",
            "xi" to "ξ",
            "pi" to "π",
            "varpi" to "ϖ",
            "rho" to "ρ",
            "sigma" to "σ",
            "varsigma" to "ς",
            "tau" to "τ",
            "upsilon" to "υ",
            "phi" to "φ",
            "varphi" to "φ",
            "chi" to "χ",
            "psi" to "ψ",
            "omega" to "ω",
            "Gamma" to "Γ",
            "Delta" to "Δ",
            "Theta" to "Θ",
            "Lambda" to "Λ",
            "Xi" to "Ξ",
            "Pi" to "Π",
            "Sigma" to "Σ",
            "Upsilon" to "Υ",
            "Phi" to "Φ",
            "Psi" to "Ψ",
            "Omega" to "Ω",
            // operators
            "cdot" to "·",
            "times" to " × ",
            "div" to " ÷ ",
            "pm" to " ± ",
            "mp" to " ∓ ",
            "ast" to "∗",
            "circ" to "∘",
            "bullet" to "•",
            "star" to "⋆",
            "oplus" to "⊕",
            "otimes" to "⊗",
            // relations
            "le" to " ≤ ",
            "leq" to " ≤ ",
            "leqslant" to " ≤ ",
            "ge" to " ≥ ",
            "geq" to " ≥ ",
            "geqslant" to " ≥ ",
            "ne" to " ≠ ",
            "neq" to " ≠ ",
            "approx" to " ≈ ",
            "equiv" to " ≡ ",
            "sim" to " ∼ ",
            "simeq" to " ≃ ",
            "cong" to " ≅ ",
            "propto" to " ∝ ",
            "ll" to " ≪ ",
            "gg" to " ≫ ",
            "lt" to " < ",
            "gt" to " > ",
            // arrows
            "to" to " → ",
            "rightarrow" to " → ",
            "leftarrow" to " ← ",
            "Rightarrow" to " ⇒ ",
            "Leftarrow" to " ⇐ ",
            "implies" to " ⇒ ",
            "iff" to " ⇔ ",
            "Leftrightarrow" to " ⇔ ",
            "leftrightarrow" to " ↔ ",
            "mapsto" to " ↦ ",
            "longrightarrow" to " ⟶ ",
            "uparrow" to "↑",
            "downarrow" to "↓",
            // sets and logic
            "in" to " ∈ ",
            "notin" to " ∉ ",
            "ni" to " ∋ ",
            "subset" to " ⊂ ",
            "subseteq" to " ⊆ ",
            "supset" to " ⊃ ",
            "supseteq" to " ⊇ ",
            "cup" to " ∪ ",
            "cap" to " ∩ ",
            "setminus" to " ∖ ",
            "emptyset" to "∅",
            "varnothing" to "∅",
            "forall" to "∀",
            "exists" to "∃",
            "neg" to "¬",
            "lnot" to "¬",
            "land" to " ∧ ",
            "wedge" to " ∧ ",
            "lor" to " ∨ ",
            "vee" to " ∨ ",
            // big operators and calculus
            "sum" to "∑",
            "prod" to "∏",
            "int" to "∫",
            "iint" to "∬",
            "oint" to "∮",
            "partial" to "∂",
            "nabla" to "∇",
            "infty" to "∞",
            "lim" to "lim ",
            // geometry and misc
            "angle" to "∠",
            "triangle" to "△",
            "perp" to " ⊥ ",
            "parallel" to " ∥ ",
            "degree" to "°",
            "ldots" to "…",
            "dots" to "…",
            "cdots" to "⋯",
            "vdots" to "⋮",
            "prime" to "′",
            "hbar" to "ℏ",
            "ell" to "ℓ",
            "Re" to "ℜ",
            "Im" to "ℑ",
            "langle" to "⟨",
            "rangle" to "⟩",
            "lfloor" to "⌊",
            "rfloor" to "⌋",
            "lceil" to "⌈",
            "rceil" to "⌉",
            "mid" to " | ",
            "vert" to "|",
            "Vert" to "‖",
            "lbrace" to "{",
            "rbrace" to "}",
            "percent" to "%",
            "euro" to "€",
            "checkmark" to "✓",
            "therefore" to " ∴ ",
            "because" to " ∵ ",
        )
}
