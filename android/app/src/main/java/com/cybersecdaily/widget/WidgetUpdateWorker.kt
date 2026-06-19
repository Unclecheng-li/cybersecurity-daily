package com.cybersecdaily.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        SettingsManager.init(applicationContext)
        val isImmediate = tags.contains("immediate")

        // Immediate refresh: always fetch and update
        if (isImmediate) {
            return fetchAndUpdate()
        }

        // Periodic refresh: lightweight date check first
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        if (hour < 12) {
            return Result.success() // Skip before noon
        }

        // Check if new content is available
        val latestDate = try {
            ReportFetcher.fetchLatestDateOnly()
        } catch (_: Exception) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }

        if (latestDate.isEmpty()) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }

        val lastKnown = SettingsManager.lastKnownDate
        if (latestDate == lastKnown && lastKnown.isNotEmpty()) {
            return Result.success() // Already up to date
        }

        // New content found, fetch and update
        return fetchAndUpdate()
    }

    private suspend fun fetchAndUpdate(): Result {
        val report = ReportFetcher.fetchLatest()
        CyberSecDailyWidgetReceiver.updateWidgets(applicationContext, report)

        if (report.error == null && report.date.isNotBlank()) {
            SettingsManager.lastKnownDate = report.date
            return Result.success()
        }
        return if (runAttemptCount < 2) Result.retry() else Result.failure()
    }
}
