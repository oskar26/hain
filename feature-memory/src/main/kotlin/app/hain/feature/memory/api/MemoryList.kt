package app.hain.feature.memory.api

import app.hain.feature.memory.R
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Pure helpers of the memory page (T-115), unit-tested without Compose. */
internal object MemoryList {
    /** The facts of a tab: suggestions or kept ones, then only those with every word of [query]. */
    fun visible(
        facts: List<MemoryFact>,
        query: String,
        suggestions: Boolean,
    ): List<MemoryFact> {
        val words = query.lowercase().split(SPACES).filter { it.isNotBlank() }
        return facts.filter { fact ->
            fact.suggested == suggestions && words.all { fact.text.lowercase().contains(it) }
        }
    }

    /** "4. Nov. 2026" / "Nov 4, 2026". */
    fun date(
        millis: Long,
        locale: Locale,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String =
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(Instant.ofEpochMilli(millis).atZone(zone))

    fun categoryLabel(category: MemoryCategory): Int =
        when (category) {
            MemoryCategory.PREFERENCE -> R.string.memory_cat_preference
            MemoryCategory.PERSON -> R.string.memory_cat_person
            MemoryCategory.PLACE -> R.string.memory_cat_place
            MemoryCategory.WORK -> R.string.memory_cat_work
            MemoryCategory.HEALTH -> R.string.memory_cat_health
            MemoryCategory.OTHER -> R.string.memory_cat_other
        }

    private val SPACES = Regex("\\s+")
}
