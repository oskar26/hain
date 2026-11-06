package app.hain.ui.components

/**
 * T-298: one answer with every Markdown element Hain draws – for the screenshot tests and to check the
 * renderer by eye (headings H1–H6, emphasis, inline code, code block with language, lists, task list,
 * quote, wide table, line, links, source numbers and formulas).
 */
object MarkdownShowcase {
    val TEXT =
        """
        # Überschrift 1
        Ein Absatz mit **fett**, *kursiv*, ***beidem***, ~~durchgestrichen~~ und `inline code`.

        ## Überschrift 2
        ### Überschrift 3
        #### Überschrift 4
        ##### Überschrift 5
        ###### Überschrift 6

        ```kotlin
        fun main() {
            val gruss = "Hallo Hain – diese Zeile ist absichtlich so lang, dass sie seitwärts scrollt"
            println(gruss)
        }
        ```

        - Punkt eins
        - Punkt zwei
          - verschachtelt
            - noch tiefer
        - Punkt drei

        1. Erstens
        2. Zweitens
           1. zwei a
           2. zwei b
        3. Drittens

        - [x] Erledigt
        - [ ] Offen

        > Ein Zitat: Wer nichts weiß, muss alles glauben.

        | Stadt | Einwohner | Bundesland | Fläche (km²) | Gegründet |
        |---|---:|---|---:|---|
        | Köln | 1 084 000 | Nordrhein-Westfalen | 405 | 38 v. Chr. |
        | Berlin | 3 878 000 | Berlin | 891 | 1237 |

        ---

        Ein [Link zu Wikipedia](https://de.wikipedia.org) und Quellen-Nummern [1] [2].

        Formel im Text: ${'$'}a^2 + b^2 = c^2${'$'} und als Block:

        ${'$'}${'$'}x_{1,2} = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}${'$'}${'$'}
        """.trimIndent()
}
