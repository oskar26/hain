package app.hain.llm.internal

import app.hain.llm.api.ParsedOutput
import app.hain.llm.api.ToolCallParser
import app.hain.llm.api.UiDelta

/**
 * Turns raw tokens into UI deltas while they arrive (LLM.md §4).
 *
 * States: REASONING (until `</think>`), TEXT, TOOL (`<tool_call>` … `</tool_call>`).
 * A possible partial tag at the end of the buffer (at most [MAX_HOLD] chars) is held back until it is
 * clear whether it is a tag. Whitespace runs are held until more content follows in the same segment, so
 * for well-formed output with one text segment the concatenated deltas equal the trimmed parts of
 * [ParsedOutput]. Whitespace around tool-call blocks is not streamed.
 *
 * The final word is [finish], which runs the strict [ToolCallParser] on the full output; the UI should
 * replace the streamed parts with it (e.g. malformed tool XML becomes plain text there, and output that
 * never closed `</think>` although thinking was on becomes plain text).
 *
 * Not thread-safe: feed tokens from one coroutine.
 */
class StreamSplitter(
    thinkingEnabled: Boolean,
    private val parser: ToolCallParser = XmlToolCallParser(),
) {
    private enum class State { REASONING, TEXT, TOOL }

    private var state = if (thinkingEnabled) State.REASONING else State.TEXT
    private val full = StringBuilder()
    private val buffer = StringBuilder()
    private val pendingWhitespace = StringBuilder()
    private var segmentHasContent = false
    private var toolBody = StringBuilder()
    private var toolNameSent = false
    private val output = mutableListOf<UiDelta>()

    /** Feed the next chunk of model output; returns the deltas that are now certain. */
    fun feed(chunk: String): List<UiDelta> {
        full.append(chunk)
        buffer.append(chunk)
        drain(final = false)
        return takeOutput()
    }

    /** Ends the stream: flushes held text and parses the whole output strictly. */
    fun finish(): Pair<List<UiDelta>, ParsedOutput> {
        drain(final = true)
        if (state == State.TOOL && !toolNameSent) {
            output += UiDelta.ToolCallStarted(null)
            toolNameSent = true
        }
        return takeOutput() to parser.parse(full.toString())
    }

    private fun takeOutput(): List<UiDelta> = output.toList().also { output.clear() }

    private fun drain(final: Boolean) {
        var foundTag = true
        while (foundTag && buffer.isNotEmpty()) {
            val tag = closingTagFor(state)
            val text = buffer.toString()
            val hit = text.indexOf(tag)
            foundTag = hit >= 0
            if (foundTag) {
                consume(text.substring(0, hit))
                buffer.delete(0, hit + tag.length)
                switchAfter(tag)
            } else {
                val hold = if (final) 0 else partialTagLength(text, tag)
                consume(text.substring(0, text.length - hold))
                buffer.delete(0, text.length - hold)
            }
        }
        if (final) pendingWhitespace.clear()
    }

    private fun closingTagFor(state: State): String =
        when (state) {
            State.REASONING -> THINK_CLOSE
            State.TEXT -> CALL_OPEN
            State.TOOL -> CALL_CLOSE
        }

    /** Length of the longest suffix of [text] that is a proper prefix of [tag] (capped at [MAX_HOLD]). */
    private fun partialTagLength(
        text: String,
        tag: String,
    ): Int {
        val max = minOf(tag.length - 1, text.length, MAX_HOLD)
        for (len in max downTo 1) {
            if (tag.startsWith(text.substring(text.length - len))) return len
        }
        return 0
    }

    private fun switchAfter(tag: String) {
        when (tag) {
            THINK_CLOSE -> enter(State.TEXT)
            CALL_OPEN -> {
                enter(State.TOOL)
                toolBody = StringBuilder()
                toolNameSent = false
            }
            CALL_CLOSE -> {
                if (!toolNameSent) output += UiDelta.ToolCallStarted(null)
                enter(State.TEXT)
            }
        }
    }

    private fun enter(next: State) {
        state = next
        pendingWhitespace.clear()
        segmentHasContent = false
    }

    private fun consume(piece: String) {
        if (piece.isEmpty()) return
        if (state == State.TOOL) {
            toolBody.append(piece)
            if (!toolNameSent) {
                FUNCTION_HEADER.find(toolBody)?.let {
                    output += UiDelta.ToolCallStarted(it.groupValues[1])
                    toolNameSent = true
                }
            }
            return
        }
        val emitted = StringBuilder()
        for (ch in piece) {
            if (ch.isWhitespace()) {
                if (segmentHasContent) pendingWhitespace.append(ch)
            } else {
                emitted.append(pendingWhitespace).append(ch)
                pendingWhitespace.clear()
                segmentHasContent = true
            }
        }
        if (emitted.isNotEmpty()) output += delta(emitted.toString())
    }

    private fun delta(text: String): UiDelta {
        val reasoning = state == State.REASONING
        return if (reasoning) UiDelta.Reasoning(text) else UiDelta.Text(text)
    }

    companion object {
        /** Longest tag we wait for (`</tool_call>`). */
        const val MAX_HOLD = 12
        private const val THINK_CLOSE = "</think>"
        private const val CALL_OPEN = "<tool_call>"
        private const val CALL_CLOSE = "</tool_call>"
        private val FUNCTION_HEADER = Regex("""<function=([A-Za-z0-9_.\-]{1,64})>""")
    }
}
