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
import app.hain.models.api.ModelFile
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/** Models live in `getExternalFilesDir("models")` (DATA.md "Files"), fallback internal storage. */
@Suppress("TooManyFunctions") // catalogue, files and downloads (single file and packs) in one place
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

    override fun mainFile(model: ModelInfo): File = fileOf(model, model.files.first())

    /** T-140: a pack of several files (dictation) gets its own folder, a single file lies in models/. */
    override fun packDir(model: ModelInfo): File = if (model.isMultiFile) File(modelsDir, model.id) else modelsDir

    private fun fileOf(
        model: ModelInfo,
        file: ModelFile,
    ): File = File(packDir(model), file.fileName)

    private fun isComplete(
        model: ModelInfo,
        file: ModelFile,
    ): Boolean = fileOf(model, file).let { it.exists() && (file.size <= 0 || it.length() == file.size) }

    override fun isInstalled(model: ModelInfo): Boolean {
        // own models from a direct link have no known size (T-211): the finished file counts
        val installed = model.files.all { f -> isComplete(model, f) }
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
                model.isMultiFile -> PackProgress.combine(packFiles(model, infos))
                else -> info.toState(model)
            }
        }

    /** T-140: the state of every file of a pack, from the job tagged with the file's index. */
    private fun packFiles(
        model: ModelInfo,
        infos: List<WorkInfo>,
    ): List<FileProgress> =
        model.files.mapIndexed { index, file ->
            val info = infos.lastOrNull { TAG_FILE + index in it.tags }
            when {
                isComplete(model, file) -> FileProgress(file.size, FileProgress.Phase.COMPLETE)
                info == null -> FileProgress(file.size, FileProgress.Phase.NONE)
                else -> info.toFileProgress(file.size)
            }
        }

    private fun WorkInfo.toFileProgress(size: Long): FileProgress =
        when (state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> FileProgress(size, FileProgress.Phase.QUEUED)
            WorkInfo.State.RUNNING ->
                if (progress.getString(ModelDownloadWorker.KEY_PHASE) == ModelDownloadWorker.PHASE_VERIFY) {
                    FileProgress(size, FileProgress.Phase.VERIFYING, size)
                } else {
                    FileProgress(size, FileProgress.Phase.RUNNING, progress.getLong(ModelDownloadWorker.KEY_DONE, 0L))
                }
            WorkInfo.State.SUCCEEDED -> FileProgress(size, FileProgress.Phase.COMPLETE)
            WorkInfo.State.FAILED -> {
                val reason = outputData.getString(ModelDownloadWorker.KEY_FAIL)
                // later files of a failed chain fail without a reason of their own
                val fail = reason?.let { r -> runCatching { FailReason.valueOf(r) }.getOrNull() }
                FileProgress(size, FileProgress.Phase.FAILED, fail = fail)
            }
            WorkInfo.State.CANCELLED -> FileProgress(size, FileProgress.Phase.CANCELLED)
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
        if (model.isMultiFile) {
            startPackDownload(model)
            return
        }
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

    /** T-140: the files of a pack one after another (one chain, one unique work), skipping finished ones. */
    private fun startPackDownload(model: ModelInfo) {
        val requests =
            model.files.withIndex().filterNot { (_, f) -> isComplete(model, f) }.map { (index, f) ->
                OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                    .setInputData(
                        workDataOf(
                            ModelDownloadWorker.KEY_URL to f.url,
                            ModelDownloadWorker.KEY_SHA to f.sha256,
                            ModelDownloadWorker.KEY_SIZE to f.size,
                            ModelDownloadWorker.KEY_TARGET to fileOf(model, f).absolutePath,
                            ModelDownloadWorker.KEY_TITLE to model.name,
                        ),
                    ).addTag(TAG_FILE + index)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            }
        if (requests.isEmpty()) return
        packDir(model).mkdirs()
        val first = workManager.beginUniqueWork(workName(model), ExistingWorkPolicy.KEEP, requests.first())
        requests.drop(1).fold(first) { chain, next -> chain.then(next) }.enqueue()
    }

    override fun cancelDownload(model: ModelInfo) {
        workManager.cancelUniqueWork(workName(model))
    }

    override fun delete(model: ModelInfo) {
        cancelDownload(model)
        model.files.forEach { f ->
            val target = fileOf(model, f)
            target.delete()
            File(target.parentFile, target.name + ".part").delete()
        }
        if (model.isMultiFile) packDir(model).delete() // only when empty
    }

    override fun freeSpace(): Long = modelsDir.usableSpace

    private fun workName(model: ModelInfo) = "download-${model.id}"

    private companion object {
        const val LEGACY_BRAIN_FILE = "LFM3-1.7B-Q4_K_M.gguf"
        const val TAG_FILE = "pack-file-"
    }
}
