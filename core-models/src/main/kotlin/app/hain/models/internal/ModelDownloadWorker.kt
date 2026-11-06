package app.hain.models.internal

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGates
import app.hain.common.api.net.Purpose
import app.hain.models.R
import app.hain.models.api.FailReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads one model file (MODELS.md "Downloader"): foreground worker with a progress notification,
 * HTTP Range resume into `<name>.part`, SHA-256 check, then atomic rename.
 */
@Suppress("TooManyFunctions") // download, check, foreground and done notifications in one worker
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val url = inputData.getString(KEY_URL) ?: return@withContext fail(FailReason.UNKNOWN)
            // T-211: own models from a direct link may come without a checksum ("" = skip the check)
            val sha = inputData.getString(KEY_SHA).orEmpty()
            val size = inputData.getLong(KEY_SIZE, -1L)
            val target = File(inputData.getString(KEY_TARGET) ?: return@withContext fail(FailReason.UNKNOWN))
            val title = inputData.getString(KEY_TITLE).orEmpty()
            val part = File(target.parentFile, target.name + ".part")
            target.parentFile?.mkdirs()
            val dir = target.parentFile ?: return@withContext fail(FailReason.STORAGE)
            if (dir.usableSpace < ((size - part.length()) * SPACE_FACTOR).toLong()) {
                return@withContext fail(FailReason.STORAGE)
            }
            // T-287: a foreground service (dataSync), so the download goes on when Hain is closed
            runCatching { setForeground(foregroundInfo(title, part.length(), size)) }
            try {
                // 3b-2: through the NetworkGate, so the download shows up in the network log
                val before = part.length()
                val gate = NetworkGates.current
                val request = GateRequest(url, Purpose.MODEL_DOWNLOAD, userStarted = true)
                if (gate == null) {
                    fetch(url, part, size, title)
                } else {
                    gate.stream(request) {
                        fetch(url, part, size, title)
                        (part.length() - before).coerceAtLeast(0)
                    }
                }
            } catch (e: IOException) {
                return@withContext if (runAttemptCount < MAX_RETRIES) Result.retry() else fail(FailReason.NETWORK, e)
            } catch (e: IllegalArgumentException) {
                return@withContext fail(FailReason.UNKNOWN, e) // not an https address
            }
            setProgress(workDataOf(KEY_PHASE to PHASE_VERIFY))
            if (sha.isNotBlank() && !sha256(part).equals(sha, ignoreCase = true)) {
                part.delete()
                return@withContext fail(FailReason.CHECKSUM)
            }
            if (!part.renameTo(target)) return@withContext fail(FailReason.STORAGE)
            notifyDone(title)
            Result.success()
        }

    private suspend fun fetch(
        url: String,
        part: File,
        size: Long,
        title: String,
    ) {
        var offset = part.length()
        if (size > 0 && offset >= size) return
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Hain/0.1 (Android)")
        if (offset > 0) conn.setRequestProperty("Range", "bytes=$offset-")
        try {
            val code = conn.responseCode
            // T-287: the part file is already whole (size unknown, e.g. own models): nothing left to load
            if (code == HTTP_RANGE_NOT_SATISFIABLE && offset > 0) return
            if (code == HttpURLConnection.HTTP_OK && offset > 0) {
                offset = 0 // server ignored the range: start over
            } else if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("HTTP $code")
            }
            val total = if (size > 0) size else offset + conn.contentLengthLong
            conn.inputStream.use { input -> copyWithProgress(input, part, offset, total, title) }
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun copyWithProgress(
        input: java.io.InputStream,
        part: File,
        offset: Long,
        total: Long,
        title: String,
    ) {
        var lastReport = 0L
        var done = offset
        FileOutputStream(part, offset > 0).use { out ->
            val buf = ByteArray(BUFFER)
            var n = input.read(buf)
            while (n >= 0) {
                if (isStopped) throw IOException("stopped")
                out.write(buf, 0, n)
                done += n
                val now = System.currentTimeMillis()
                if (now - lastReport > REPORT_MS) {
                    lastReport = now
                    setProgress(workDataOf(KEY_DONE to done, KEY_TOTAL to total))
                    runCatching { setForeground(foregroundInfo(title, done, total)) }
                }
                n = input.read(buf)
            }
        }
    }

    private fun percent(
        done: Long,
        total: Long,
    ): Int = if (total > 0) (done * PERCENT / total).toInt() else 0

    private fun fail(
        reason: FailReason,
        e: Throwable? = null,
    ): Result {
        e?.let { android.util.Log.w(TAG, "download failed", it) }
        return Result.failure(workDataOf(KEY_FAIL to reason.name))
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(BUFFER)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Needed for an expedited job on Android 11 and older (T-287). */
    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(inputData.getString(KEY_TITLE).orEmpty(), 0L, inputData.getLong(KEY_SIZE, -1L))

    private fun channel(): NotificationManager {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val name = applicationContext.getString(R.string.models_download_channel)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_LOW))
        }
        return nm
    }

    private fun foregroundInfo(
        title: String,
        done: Long,
        total: Long,
    ): ForegroundInfo {
        channel()
        val percent = percent(done, total)
        // T-287: "Stopp" right in the notification; the app then shows the download as paused
        val stop = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val builder =
            NotificationCompat
                .Builder(applicationContext, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(applicationContext.getString(R.string.models_downloading, title))
                .setProgress(PERCENT, percent, total <= 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .addAction(0, applicationContext.getString(R.string.models_download_stop), stop)
        if (total > 0) {
            val line = applicationContext.getString(R.string.models_download_progress, percent, mb(done), mb(total))
            builder.setContentText(line)
        }
        launchIntent()?.let(builder::setContentIntent)
        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /** T-287: "Fertig" after a download that ran while Hain was closed (only with the permission). */
    private fun notifyDone(title: String) {
        val granted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val builder =
            NotificationCompat
                .Builder(applicationContext, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(applicationContext.getString(R.string.models_download_done, title))
                .setAutoCancel(true)
        launchIntent()?.let(builder::setContentIntent)
        runCatching { channel().notify(DONE_NOTIFICATION_ID, builder.build()) }
    }

    private fun launchIntent(): PendingIntent? {
        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        return launch?.let {
            PendingIntent.getActivity(applicationContext, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun mb(bytes: Long): String = (bytes / MB).toString()

    companion object {
        const val KEY_URL = "url"
        const val KEY_SHA = "sha256"
        const val KEY_SIZE = "size"
        const val KEY_TARGET = "target"
        const val KEY_TITLE = "title"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_PHASE = "phase"
        const val KEY_FAIL = "fail"
        const val PHASE_VERIFY = "verify"
        private const val TAG = "HainDownload"
        private const val CHANNEL = "model_downloads"
        private const val NOTIFICATION_ID = 4711
        private const val DONE_NOTIFICATION_ID = 4712
        private const val HTTP_RANGE_NOT_SATISFIABLE = 416
        private const val MB = 1_000_000L
        private const val BUFFER = 256 * 1024
        private const val TIMEOUT_MS = 30_000
        private const val REPORT_MS = 500L
        private const val PERCENT = 100
        private const val MAX_RETRIES = 5
        private const val SPACE_FACTOR = 1.1
    }
}
