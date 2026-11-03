package app.hain.feature.chat.internal

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Thumb up or down on an answer (T-057). Stays on this phone, never sent anywhere. */
enum class Rating { UP, DOWN }

/** Tapping the same thumb again takes it back; the other thumb replaces it. */
fun Rating?.toggled(tap: Rating): Rating? = if (this == tap) null else tap

/** Where the thumbs are kept: answer id → rating. */
interface MessageRatings {
    val ratings: StateFlow<Map<Long, Rating>>

    fun set(
        messageId: Long,
        rating: Rating?,
    )
}

/** For tests and previews: forgets everything when the app closes. */
class InMemoryRatings : MessageRatings {
    private val state = MutableStateFlow<Map<Long, Rating>>(emptyMap())
    override val ratings: StateFlow<Map<Long, Rating>> = state.asStateFlow()

    override fun set(
        messageId: Long,
        rating: Rating?,
    ) = state.update { if (rating == null) it - messageId else it + (messageId to rating) }
}

/** Keeps the thumbs in a small private preferences file on the phone. */
class PrefsRatings(
    private val prefs: SharedPreferences,
) : MessageRatings {
    private val state =
        MutableStateFlow(
            prefs.all
                .mapNotNull { (key, value) ->
                    val id = key.removePrefix(PREFIX).toLongOrNull()
                    val rating = (value as? String)?.let { runCatching { Rating.valueOf(it) }.getOrNull() }
                    if (id != null && rating != null) id to rating else null
                }.toMap(),
        )
    override val ratings: StateFlow<Map<Long, Rating>> = state.asStateFlow()

    override fun set(
        messageId: Long,
        rating: Rating?,
    ) {
        val editor = prefs.edit()
        if (rating == null) editor.remove(PREFIX + messageId) else editor.putString(PREFIX + messageId, rating.name)
        editor.apply()
        state.update { if (rating == null) it - messageId else it + (messageId to rating) }
    }

    private companion object {
        const val PREFIX = "r_"
    }
}
