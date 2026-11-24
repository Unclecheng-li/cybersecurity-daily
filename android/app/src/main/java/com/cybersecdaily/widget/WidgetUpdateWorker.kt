package com.cybersecdaily.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val TAG = "WidgetUpdateWorker"

class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        refreshMutex.withLock { refresh() }
    }

    private suspend fun refresh(): Result {
        SettingsManager.init(applicationContext)
        val cache = WidgetReportCache(applicationContext)
        val cachedReport = DataHolder.currentReport ?: cache.read()?.also {
            CyberSecDailyWidgetReceiver.updateWidgets(applicationContext, it)
        }
        val isImmediate = tags.contains("immediate")

        // Recover missing widget data even before the usual midday check.
        if (!isImmediate && cachedReport != null && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 12) {
            return Result.success()
        }

        val report = try {
            val date = ReportFetcher.fetchLatestDateOnly()
            if (!isImmediate && cachedReport?.date == date) {
                cachedReport
            } else {
                val html = ReportFetcher.fetchDailyPage(date)
                val downloaded = ReportFetcher.parseDailyHtml(html, date)
                currentCoroutineContext().ensureActive()
                cache.save(date, html)
                SettingsManager.lastKnownDate = date
                downloaded
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Log.w(TAG, "Widget refresh failed", e)
            CyberSecDailyWidgetReceiver.updateWidgets(applicationContext, refreshFailed = true)
            return if (runAttemptCount < 2) Result.retry() else Result.failure()
        }

        currentCoroutineContext().ensureActive()
        CyberSecDailyWidgetReceiver.updateWidgets(applicationContext, report)
        return Result.success()
    }

    companion object {
        // Immediate and periodic work update the same cache and widgets.
        private val refreshMutex = Mutex()
    }
}
