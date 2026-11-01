package app.hain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hain.ui.pim.Pim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Minimal themed home until the chat screen lands (T-052):
            // Pim greets, on our real background colour — edge-to-edge safe.
            HainTheme {
                val colors = LocalHainColors.current
                val type = LocalHainTypography.current
                val spacing = LocalHainSpacing.current
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(colors.bg)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.lg),
                    ) {
                        Pim(state = PimState.HAPPY, size = 96.dp)
                        BasicText(
                            text = stringResource(R.string.app_name),
                            style = type.title.copy(color = colors.text),
                        )
                    }
                }
            }
        }
    }
}
