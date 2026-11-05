package app.hain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.hain.common.api.BrainChoiceStore
import app.hain.feature.chat.api.BrainPickerState
import app.hain.models.api.BrainChoice
import app.hain.models.api.DeviceProfile
import app.hain.models.api.DownloadState
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * T-055: glue between the brain picker in the top bar, the catalogue and the DataStore choice.
 * Kept out of MainActivity so the activity only wires it in.
 */
internal class BrainPicking(
    private val models: ModelRepository,
    private val store: BrainChoiceStore,
    private val scope: CoroutineScope,
    /** Phone memory in GB, for "recommended for your phone". */
    private val ramGb: () -> Int,
    /** The own GGUF of developer mode when it is in use and on the phone, else `null`. */
    private val ownModelPath: () -> String?,
) {
    /** Id of the picked brain (Compose state, so the menu follows a pick at once). */
    private var selectedId by mutableStateOf<String?>(null)

    private val brains: List<ModelInfo> get() = BrainChoice.brains(models.catalog)

    /** The brain to load: the picked one when it is on the phone, else the default. */
    fun active(): ModelInfo = BrainChoice.active(models.catalog, selectedId, models::isInstalled)

    /** Main file of the brain in use: the own model of developer mode first, then [active]. */
    fun path(): String = ownModelPath() ?: models.mainFile(active()).absolutePath

    /** True when any brain of the catalogue is on the phone. */
    fun anyInstalled(): Boolean = brains.any(models::isInstalled)

    /**
     * `true` while any brain of the catalogue is on the phone. Waits for the picked brain from
     * DataStore first, so the chat's first load already takes the right file.
     */
    fun installedFlow(): Flow<Boolean> =
        combine(
            store.selectedId.onEach { selectedId = it },
            combine(brains.map { models.downloadState(it) }) { states ->
                states.any { it is DownloadState.Installed }
            },
        ) { _, any -> any }

    /** Rows and actions of the menu; [onSwitch] reloads the brain in the chat. */
    fun state(
        onSwitch: () -> Unit,
        onGetMore: () -> Unit,
    ): BrainPickerState =
        BrainPickerState(
            options = {
                BrainChoice.options(
                    models.catalog,
                    selectedId,
                    models::isInstalled,
                    DeviceProfile(ramGb(), models.freeSpace()),
                )
            },
            ownModelActive = { ownModelPath() != null },
            onPick = { option ->
                selectedId = option.model.id
                scope.launch { store.select(option.model.id) }
                onSwitch()
            },
            onGetMore = onGetMore,
        )
}
