package app.hain.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * T-251 (Oskar 04.11. 22:18, tablets): pages with many tiles (Modelle & Pakete, Gedächtnis) show
 * them in two columns on wide windows instead of one long, stretched column. Phones and tablets in
 * portrait keep one column, so a small screen never gets two cramped columns.
 */
object HainColumns {
    /** Widest a page with columns gets on very wide windows; it stays centred. */
    const val WIDE_PAGE_MAX_DP = 1080

    /** Widest a one-column page gets (as before on tablets in portrait). */
    const val PAGE_MAX_DP = 720

    /** Columns for a window of size class [size]: two from [HainWindowSize.EXPANDED] on. */
    fun count(size: HainWindowSize): Int = if (size.isExpanded) 2 else 1

    /** Max width of a page in dp for [columns] columns. */
    fun pageMaxDp(columns: Int): Int = if (columns > 1) WIDE_PAGE_MAX_DP else PAGE_MAX_DP

    /** [items] cut into rows of [columns]; the last row may be shorter. */
    fun <T> rows(
        items: List<T>,
        columns: Int,
    ): List<List<T>> = items.chunked(columns.coerceAtLeast(1))
}

/**
 * [items] as a calm grid of [columns] columns, inside a scrolling column (no lazy list). Tiles of
 * one row get the same height: [item] receives a modifier that fills the row height. An odd last
 * tile keeps the width of one column.
 */
@Composable
fun <T> HainGrid(
    items: List<T>,
    columns: Int,
    spacing: Dp,
    modifier: Modifier = Modifier,
    item: @Composable (T, Modifier) -> Unit,
) {
    if (columns <= 1) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.forEach { item(it, Modifier) }
        }
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        HainColumns.rows(items, columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                row.forEach { entry ->
                    Box(Modifier.weight(1f).fillMaxHeight()) { item(entry, Modifier.fillMaxHeight()) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
