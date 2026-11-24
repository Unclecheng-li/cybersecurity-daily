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

const val ACTION_REFRESH = "com.cybersecdaily.widget.ACTION_REFRESH"

class CyberSecDailyWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            triggerRefresh(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(ctx: Context, awm: AppWidgetManager, ids: IntArray) {
        cancelLegacyWork(ctx)
        renderWidgets(ctx, awm, ids)
        triggerRefresh(ctx)
        schedulePeriodicRefresh(ctx)
    }

    override fun onEnabled(ctx: Context) {
        cancelLegacyWork(ctx)
        triggerRefresh(ctx)
        schedulePeriodicRefresh(ctx)
    }

    override fun onDisabled(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork(PERIODIC_WORK_NAME)
        wm.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        cancelLegacyWork(ctx)
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "cybersec_daily_periodic_refresh"
        private const val IMMEDIATE_WORK_NAME = "cybersec_daily_widget_refresh"
        private const val LEGACY_IMMEDIATE_WORK_NAME = "cybersec_daily_immediate_refresh"
        private const val LEGACY_REFRESH_WORK_NAME = "cybersec_daily_refresh"
        private const val LEGACY_GLANCE_SESSION_TAG = "androidx.glance.session.SessionWorker"

        fun triggerRefresh(ctx: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .addTag("immediate")
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME, ExistingWorkPolicy.KEEP, request
            )
        }

        fun updateWidgets(ctx: Context, report: DailyReport? = null, refreshFailed: Boolean = false) {
            val awm = AppWidgetManager.getInstance(ctx)
            val ids = awm.getAppWidgetIds(ComponentName(ctx, CyberSecDailyWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            renderWidgets(ctx, awm, ids, report, refreshFailed)
        }

        @Synchronized
        private fun renderWidgets(
            ctx: Context,
            awm: AppWidgetManager,
            ids: IntArray,
            newReport: DailyReport? = null,
            refreshFailed: Boolean = false
        ) {
            SettingsManager.init(ctx)
            if (newReport != null) DataHolder.currentReport = newReport
            val report = DataHolder.currentReport
            val date = (report?.date ?: SettingsManager.lastKnownDate)
                .takeIf(ReportFetcher::isReportDate).orEmpty()
            val url = ReportFetcher.reportUrl(date)

            for (id in ids) {
                val views = createViews(ctx, report, date, url, refreshFailed)
                applyListAdapter(ctx, views, id, url)
                awm.updateAppWidget(id, views)
                awm.notifyAppWidgetViewDataChanged(id, R.id.widget_article_list)
            }
        }

        private fun applyListAdapter(ctx: Context, views: RemoteViews, widgetId: Int, url: String) {
            val svcIntent = Intent(ctx, WidgetDataService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_article_list, svcIntent)

            val itemPI = PendingIntent.getActivity(ctx, widgetId * 1000 + 1,
                Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setPendingIntentTemplate(R.id.widget_article_list, itemPI)
        }

        private fun cancelLegacyWork(ctx: Context) {
            val wm = WorkManager.getInstance(ctx)
            wm.cancelUniqueWork(LEGACY_IMMEDIATE_WORK_NAME)
            wm.cancelUniqueWork(LEGACY_REFRESH_WORK_NAME)
            wm.cancelAllWorkByTag(LEGACY_GLANCE_SESSION_TAG)
        }

        private fun schedulePeriodicRefresh(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                30, TimeUnit.MINUTES, 15, TimeUnit.MINUTES
            )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag("periodic")
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        private fun createViews(
            ctx: Context,
            report: DailyReport?,
            date: String,
            url: String,
            refreshFailed: Boolean
        ): RemoteViews {
            val showCharts = SettingsManager.showCharts.value
            val showQuadrant = SettingsManager.showQuadrantChart.value
            val showPie = SettingsManager.showPieChart.value

            val views = RemoteViews(ctx.packageName, R.layout.widget_layout)
            views.setViewVisibility(R.id.widget_refresh_progress, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_refresh, android.view.View.VISIBLE)

            views.setTextViewText(R.id.widget_date, date.ifBlank { "Latest" })
            val keywords = when {
                report != null -> "\u4eca\u65e5\u5173\u952e\u8bcd\uff1a" +
                    report.keywords.ifBlank { "Daily security highlights" }
                refreshFailed -> "Update failed. Tap refresh to retry."
                else -> "Tap refresh to load the latest report."
            }
            views.setTextViewText(R.id.widget_keywords, keywords)

            val ri = Intent(ctx, CyberSecDailyWidgetReceiver::class.java).apply { action = ACTION_REFRESH }
            views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(
                ctx, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            val settingsIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(R.id.widget_settings, PendingIntent.getActivity(
                ctx, 100, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

            if (report != null) {
                try {
                    val showAny = showCharts && report.charts.isNotEmpty()
                    val c0 = report.charts.getOrNull(0); val c1 = report.charts.getOrNull(1)
                    val s0 = showAny && showQuadrant && c0 != null
                    val s1 = showAny && showPie && c1 != null
                    if (s0 || s1) {
                        views.setViewVisibility(R.id.widget_charts_section, android.view.View.VISIBLE)
                        if (s0) {
                            renderChart(c0!!, 400, 180)?.let {
                                BitmapFactory.decodeByteArray(it, 0, it.size)?.let { views.setImageViewBitmap(R.id.widget_chart_img_1, it) }
                            }
                            views.setViewVisibility(R.id.widget_chart_img_1, android.view.View.VISIBLE)
                        } else views.setViewVisibility(R.id.widget_chart_img_1, android.view.View.GONE)
                        if (s1) {
                            renderChart(c1!!, 400, 180)?.let {
                                BitmapFactory.decodeByteArray(it, 0, it.size)?.let { views.setImageViewBitmap(R.id.widget_chart_img_2, it) }
                            }
                            views.setViewVisibility(R.id.widget_chart_img_2, android.view.View.VISIBLE)
                        } else views.setViewVisibility(R.id.widget_chart_img_2, android.view.View.GONE)
                    } else views.setViewVisibility(R.id.widget_charts_section, android.view.View.GONE)
                } catch (_: Exception) {
                    views.setViewVisibility(R.id.widget_charts_section, android.view.View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.widget_charts_section, android.view.View.GONE)
            }

            val footer = when {
                date.isBlank() -> "Tap to open report archive"
                refreshFailed && report != null -> "Update failed · Tap to read saved report"
                else -> "Tap to read full report"
            }
            views.setTextViewText(R.id.widget_footer, footer)
            val openReport = PendingIntent.getActivity(
                ctx, url.hashCode(), Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openReport)
            views.setOnClickPendingIntent(R.id.widget_footer, openReport)

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
