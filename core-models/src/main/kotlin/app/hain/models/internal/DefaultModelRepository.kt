package app.hain.models.internal

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.hain.models.api.DownloadState
import app.hain.models.api.FailReason
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/** Models live in `getExternalFilesDir("models")` (DATA.md "Files"), fallback internal storage. */
class DefaultModelRepository(
    private val context: Context,
) : ModelRepository {
    override val catalog: List<ModelInfo> by lazy {
        CatalogParser.parse(
            context.assets
                .open("catalog.json")
                .bufferedReader()
                .use { it.readText() },
        )
    }

    override val defaultBrain: ModelInfo get() = catalog.first { it.kind == "brain" && it.isDefault }

    private val workManager get() = WorkManager.getInstance(context)

    private val modelsDir: File
        get() = (context.getExternalFilesDir("models") ?: File(context.filesDir, "models")).apply { mkdirs() }

    override fun mainFile(model: ModelInfo): File = File(modelsDir, model.files.first().fileName)

    override fun isInstalled(model: ModelInfo): Boolean {
        val installed =
            model.files.all { f -> File(modelsDir, f.fileName).let { it.exists() && it.length() == f.size } }
        // The old Q4_K_M brain is replaced by QAD-Q4_0: free the space once the new file is complete.
        if (installed && model.kind == "brain") File(modelsDir, LEGACY_BRAIN_FILE).delete()
        return installed
    }

    override fun downloadState(model: ModelInfo): Flow<DownloadState> =
        workManager.getWorkInfosForUniqueWorkFlow(workName(model)).map { infos ->
            val info = infos.lastOrNull()
            when {
                isInstalled(model) -> DownloadState.Installed
                info == null -> DownloadState.Idle
                else -> info.toState(model)
            }
        }

    private fun WorkInfo.toState(model: ModelInfo): DownloadState =
        when (state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Queued
            WorkInfo.State.RUNNING ->
                if (progress.getString(ModelDownloadWorker.KEY_PHASE) == ModelDownloadWorker.PHASE_VERIFY) {
                    DownloadState.Verifying
                } else {
                    DownloadState.Running(
                        progress.getLong(ModelDownloadWorker.KEY_DONE, 0L),
                        progress.getLong(ModelDownloadWorker.KEY_TOTAL, model.totalSize),
                    )
                }
            WorkInfo.State.SUCCEEDED -> if (isInstalled(model)) DownloadState.Installed else DownloadState.Idle
            WorkInfo.State.FAILED ->
                DownloadState.Failed(
                    runCatching {
                        FailReason.valueOf(outputData.getString(ModelDownloadWorker.KEY_FAIL).orEmpty())
                    }.getOrDefault(FailReason.UNKNOWN),
                )
            WorkInfo.State.CANCELLED -> DownloadState.Idle
        }

    override fun startDownload(model: ModelInfo) {
        val file = model.files.first()
        val request =
            OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(
                    workDataOf(
                        ModelDownloadWorker.KEY_URL to file.url,
                        ModelDownloadWorker.KEY_SHA to file.sha256,
                        ModelDownloadWorker.KEY_SIZE to file.size,
                        ModelDownloadWorker.KEY_TARGET to mainFile(model).absolutePath,
                        ModelDownloadWorker.KEY_TITLE to model.name,
                    ),
                ).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        workManager.enqueueUniqueWork(workName(model), ExistingWorkPolicy.KEEP, request)
    }

    override fun cancelDownload(model: ModelInfo) {
        workManager.cancelUniqueWork(workName(model))
    }

    override fun delete(model: ModelInfo) {
        cancelDownload(model)
        model.files.forEach { f ->
            File(modelsDir, f.fileName).delete()
            File(modelsDir, f.fileName + ".part").delete()
        }
    }

    override fun freeSpace(): Long = modelsDir.usableSpace

    private fun workName(model: ModelInfo) = "download-${model.id}"

    private companion object {
        const val LEGACY_BRAIN_FILE = "LFM3-1.7B-Q4_K_M.gguf"
    }
}
