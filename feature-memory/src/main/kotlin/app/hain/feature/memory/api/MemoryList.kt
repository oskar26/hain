package app.hain.feature.memory.api

import app.hain.feature.memory.R
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryTopic
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

    /**
     * The "Themen" tab (7b): kept facts grouped by kind, in a fixed order, with the user's note on
     * each (a topic's title is the kind's key). Kinds without facts but with a note stay listed.
     */
    fun topics(
        facts: List<MemoryFact>,
        notes: List<MemoryTopic>,
    ): List<TopicGroup> {
        val kept = facts.filter { !it.suggested }
        return MemoryCategory.entries.mapNotNull { category ->
            val inIt = kept.filter { it.category == category }
            val note = notes.firstOrNull { it.title == category.key }?.note.orEmpty()
            if (inIt.isEmpty() && note.isBlank()) null else TopicGroup(category, inIt, note)
        }
    }

    /** The kept fact a suggestion would replace, if it is still there. */
    fun replaced(
        suggestion: MemoryFact,
        facts: List<MemoryFact>,
    ): MemoryFact? = suggestion.replaces?.let { id -> facts.firstOrNull { it.id == id && !it.suggested } }

    private val SPACES = Regex("\\s+")
}

/** One topic of the "Themen" tab. */
data class TopicGroup(
    val category: MemoryCategory,
    val facts: List<MemoryFact>,
    val note: String,
)
