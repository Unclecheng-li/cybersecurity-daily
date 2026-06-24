package com.cybersecdaily.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://unclecheng-li.github.io/cybersecurity-daily"
const val ACTION_REFRESH = "com.cybersecdaily.widget.ACTION_REFRESH"

class CyberSecDailyWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            showRefreshProgress(context)
            triggerRefresh(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        cancelLegacyGlanceWork(context)
        updateLoading(context, appWidgetManager, appWidgetIds)
        triggerRefresh(context)
        schedulePeriodicRefresh(context)
    }

    override fun onEnabled(context: Context) {
        cancelLegacyGlanceWork(context)
        triggerRefresh(context)
        schedulePeriodicRefresh(context)
    }

    override fun onDisabled(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(PERIODIC_WORK_NAME)
        wm.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        wm.cancelUniqueWork(LEGACY_REFRESH_WORK_NAME)
        wm.cancelAllWorkByTag(LEGACY_GLANCE_SESSION_TAG)
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "cybersec_daily_periodic_refresh"
        private const val IMMEDIATE_WORK_NAME = "cybersec_daily_immediate_refresh"
        private const val LEGACY_REFRESH_WORK_NAME = "cybersec_daily_refresh"
        private const val LEGACY_GLANCE_SESSION_TAG = "androidx.glance.session.SessionWorker"

        fun triggerRefresh(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setConstraints(constraints)
                .addTag("immediate")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }

        private fun showRefreshProgress(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            val ids = awm.getAppWidgetIds(ComponentName(context, CyberSecDailyWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setViewVisibility(R.id.widget_refresh_progress, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_refresh, android.view.View.GONE)
            for (id in ids) awm.partiallyUpdateAppWidget(id, views)
        }

        fun updateWidgets(context: Context, report: DailyReport) {
            val awm = AppWidgetManager.getInstance(context)
            val ids = awm.getAppWidgetIds(ComponentName(context, CyberSecDailyWidgetReceiver::class.java))
            if (ids.isEmpty()) return

            // Store data for ListView adapter
            SettingsManager.init(context)
            val allArticles = report.articles.filter { it.title.isNotBlank() }
            val showSev = SettingsManager.showSeverity.value
            val filtered = if (showSev && report.error == null) {
                allArticles.filter { SettingsManager.isSeverityVisible(it.severity) }
            } else allArticles

            val maxCount = SettingsManager.headlineCount.value
            DataHolder.currentArticles = filtered.take(maxCount)
            DataHolder.currentReport = report

            val views = createViews(context, report)
            for (id in ids) {
                // Set ListView adapter
                val svcIntent = Intent(context, WidgetDataService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_article_list, svcIntent)

                awm.updateAppWidget(id, views)
                awm.notifyAppWidgetViewDataChanged(id, R.id.widget_article_list)
            }
        }

        private fun updateLoading(ctx: Context, awm: AppWidgetManager, ids: IntArray) {
            DataHolder.currentArticles = listOf(ArticleItem("Fetching latest report..."))
            DataHolder.currentReport = DailyReport(date = "Loading", keywords = "Fetching latest security news...")
            val views = createViews(ctx, DataHolder.currentReport!!)
            for (id in ids) {
                val svcIntent = Intent(ctx, WidgetDataService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_article_list, svcIntent)
                awm.updateAppWidget(id, views)
            }
        }

        private fun cancelLegacyGlanceWork(ctx: Context) {
            val wm = WorkManager.getInstance(ctx)
            wm.cancelUniqueWork(LEGACY_REFRESH_WORK_NAME)
            wm.cancelAllWorkByTag(LEGACY_GLANCE_SESSION_TAG)
        }

        private fun schedulePeriodicRefresh(ctx: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("periodic")
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        private fun createViews(context: Context, report: DailyReport): RemoteViews {
            SettingsManager.init(context)
            val showCharts = SettingsManager.showCharts.value
            val showQuadrant = SettingsManager.showQuadrantChart.value
            val showPie = SettingsManager.showPieChart.value

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val hasError = report.error != null

            // Progress off, refresh on
            views.setViewVisibility(R.id.widget_refresh_progress, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_refresh, android.view.View.VISIBLE)

            views.setTextViewText(R.id.widget_date, report.date.ifBlank { "Latest" })
            views.setTextViewText(
                R.id.widget_keywords,
                if (hasError) "Update failed"
                else "\u4eca\u65e5\u5173\u952e\u8bcd\uff1a" + report.keywords.ifBlank { "Daily security highlights" }
            )

            // Refresh click
            val ri = Intent(context, CyberSecDailyWidgetReceiver::class.java).apply { action = ACTION_REFRESH }
            views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(
                context, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            // Settings gear click
            val settingsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(R.id.widget_settings, PendingIntent.getActivity(
                context, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            // Charts
            val cw = 500; val ch = 220
            val showAny = showCharts && !hasError && report.charts.isNotEmpty()
            val c0 = report.charts.getOrNull(0); val c1 = report.charts.getOrNull(1)
            val s0 = showAny && showQuadrant && c0 != null
            val s1 = showAny && showPie && c1 != null
            if (s0 || s1) {
                views.setViewVisibility(R.id.widget_charts_section, android.view.View.VISIBLE)
                if (s0) {
                    renderChart(c0!!, cw, ch)?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.let { b -> views.setImageViewBitmap(R.id.widget_chart_img_1, b) } }
                    views.setViewVisibility(R.id.widget_chart_img_1, android.view.View.VISIBLE)
                } else views.setViewVisibility(R.id.widget_chart_img_1, android.view.View.GONE)
                if (s1) {
                    renderChart(c1!!, cw, ch)?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.let { b -> views.setImageViewBitmap(R.id.widget_chart_img_2, b) } }
                    views.setViewVisibility(R.id.widget_chart_img_2, android.view.View.VISIBLE)
                } else views.setViewVisibility(R.id.widget_chart_img_2, android.view.View.GONE)
            } else views.setViewVisibility(R.id.widget_charts_section, android.view.View.GONE)

            views.setTextViewText(R.id.widget_footer, if (hasError) "Retrying later" else "Tap to read full report")

            // Root click
            val url = if (!hasError && report.date.isNotBlank()) "$BASE_URL/daily/${report.date}.html" else BASE_URL
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(
                context, url.hashCode(), Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            // PendingIntent template for ListView item clicks
            val itemClickIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            views.setPendingIntentTemplate(R.id.widget_article_list, PendingIntent.getActivity(
                context, url.hashCode(), itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            return views
        }

        private fun renderChart(chart: ChartData, w: Int, h: Int) = try {
            when (chart) {
                is ChartData.QuadrantChart -> ChartRenderer.renderQuadrantChart(chart, w, h)
                is ChartData.PieChart -> ChartRenderer.renderPieChart(chart, w, h)
                is ChartData.FlowChart -> ChartRenderer.renderFlowchart(chart, w, h)
                is ChartData.Timeline -> ChartRenderer.renderTimeline(chart, w, h)
            }
        } catch (_: Exception) { null }
    }
}
