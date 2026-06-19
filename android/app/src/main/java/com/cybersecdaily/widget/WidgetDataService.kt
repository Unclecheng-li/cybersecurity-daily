package com.cybersecdaily.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WidgetDataService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetDataFactory(applicationContext, intent)
    }

    inner class WidgetDataFactory(
        private val context: Context,
        intent: Intent
    ) : RemoteViewsService.RemoteViewsFactory {

        private val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        private var articles: List<ArticleItem> = emptyList()
        private var showSeverity: Boolean = true

        override fun onCreate() {
            // Initialize
        }

        override fun onDataSetChanged() {
            SettingsManager.init(context)
            articles = DataHolder.currentArticles
            showSeverity = SettingsManager.showSeverity.value
        }

        override fun onDestroy() {}

        override fun getCount(): Int = articles.size

        override fun getViewAt(position: Int): RemoteViews {
            val article = articles[position]
            val views = RemoteViews(context.packageName, R.layout.widget_article_item)

            if (showSeverity && article.severity != Severity.NONE && SettingsManager.isSeverityVisible(article.severity)) {
                views.setViewVisibility(R.id.widget_sev_item, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_sev_item, article.severity.label)
                views.setInt(R.id.widget_sev_item, "setBackgroundColor", article.severity.color)
            } else {
                views.setViewVisibility(R.id.widget_sev_item, android.view.View.GONE)
            }

            views.setTextViewText(R.id.widget_headline_item, article.title)

            // Set click intent for each article
            val fillInIntent = Intent()
            views.setOnClickFillInIntent(R.id.widget_article_row, fillInIntent)

            return views
        }

        override fun getLoadingView(): RemoteViews? {
            val views = RemoteViews(context.packageName, R.layout.widget_article_item)
            views.setTextViewText(R.id.widget_headline_item, "\u52a0\u8f7d\u4e2d...")
            views.setViewVisibility(R.id.widget_sev_item, android.view.View.GONE)
            return views
        }

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}

/** Simple singleton data holder for passing report data to the widget ListView adapter */
object DataHolder {
    var currentArticles: List<ArticleItem> = emptyList()
    var currentReport: DailyReport? = null
}
