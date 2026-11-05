package app.hain.feature.news.api

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.hain.feature.news.R
import app.hain.tools.internal.news.NewsRepository
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * The optional daily briefing note (T-079, off by default; News → Einstellungen). Once a day around
 * [HOUR] o'clock a background job loads the feeds through the gate, makes today's briefing (the
 * stories' own sentences, no brain in the background) and shows a quiet notification. Tapping it
 * opens Hain on the news page.
 */
class BriefingDaily(
    private val context: Context,
    private val repository: () -> NewsRepository,
    private val maker: BriefingMaker,
    private val lang: () -> String,
) {
    /** One run of the job; `true` when a note was shown. */
    suspend fun run(): Boolean {
        val news = repository()
        news.load()
        news.refresh()
        val briefing = maker.today(news.items(), lang(), useBrain = false) ?: return false
        return notify(briefing)
    }

    @Suppress("ReturnCount")
    private fun notify(briefing: Briefing): Boolean {
        val allowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!allowed || briefing.points.isEmpty()) return false
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val name = context.getString(R.string.news_briefing_channel)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_LOW))
        }
        val open =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                putExtra(EXTRA_OPEN, OPEN_NEWS)
            }
        val tap =
            open?.let {
                PendingIntent.getActivity(
                    context,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        val inbox = NotificationCompat.InboxStyle()
        briefing.points.take(SHOWN_POINTS).forEach { inbox.addLine(it.text) }
        val note =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_news)
                .setContentTitle(context.getString(R.string.news_briefing_title))
                .setContentText(briefing.points.first().text)
                .setStyle(inbox)
                .setAutoCancel(true)
                .setContentIntent(tap)
                .build()
        nm.notify(NOTIFICATION_ID, note)
        return true
    }

    companion object {
        /** Set once by the app; the worker reads it (WorkManager creates workers by itself). */
        @Volatile var current: BriefingDaily? = null

        /** Extra on the launch intent: open the news page. */
        const val EXTRA_OPEN = "hain.open"
        const val OPEN_NEWS = "news"
        const val HOUR = 7
        private const val CHANNEL = "news_briefing"
        private const val WORK = "news-briefing-daily"
        private const val NOTIFICATION_ID = 4712
        private const val SHOWN_POINTS = 5

        /** Turns the daily job on or off (the switch on the news settings page). */
        fun schedule(
            context: Context,
            on: Boolean,
        ) {
            val work = WorkManager.getInstance(context.applicationContext)
            if (!on) {
                work.cancelUniqueWork(WORK)
                return
            }
            val request =
                PeriodicWorkRequestBuilder<BriefingWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(
                        delayUntilNext(ZonedDateTime.now(ZoneId.systemDefault())).toMinutes(),
                        TimeUnit.MINUTES,
                    ).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** Time until the next [HOUR]:00 after [now] (today if still ahead, else tomorrow). */
        fun delayUntilNext(now: ZonedDateTime): Duration {
            var next = now.with(LocalTime.of(HOUR, 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}

/** The worker WorkManager starts for [BriefingDaily]; does nothing when the note is switched off. */
class BriefingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    override suspend fun doWork(): Result {
        if (!SharedNewsPrefs.briefingNotificationOn(applicationContext)) return Result.success()
        val daily = BriefingDaily.current ?: return Result.success()
        return try {
            daily.run()
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
