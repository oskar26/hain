package app.hain.ui.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.valentinilk.shimmer.shimmer

/**
 * Shimmer for thinking text, loading cards and running tool chips
 * (UI_LIBRARIES.md §2). Reduce-motion handling lives in the motion system
 * (T-019): callers must not attach this modifier when reduce motion is on.
 */
@Composable
fun Modifier.hainShimmer(): Modifier = shimmer()
