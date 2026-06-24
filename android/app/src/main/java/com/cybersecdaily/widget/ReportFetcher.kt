package com.cybersecdaily.widget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

object ReportFetcher {

    private const val BASE_URL = "https://unclecheng-li.github.io/cybersecurity-daily"
    private const val INDEX_URL = "$BASE_URL/index.html"
    private const val DAILY_URL = "$BASE_URL/daily"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchLatest(): DailyReport = withContext(Dispatchers.IO) {
        try {
            val latestDate = fetchLatestDateOnly()
            val dailyHtml = fetchDailyPage(latestDate)
            parseDailyHtml(dailyHtml, latestDate)
        } catch (e: Exception) {
            DailyReport.error("获取失败：${e.localizedMessage ?: "未知错误"}")
        }
    }

    fun fetchLatestDateOnly(): String {
        val request = Request.Builder().url(INDEX_URL).build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("index request failed (${response.code})")
            response.body?.string() ?: throw Exception("empty index page")
        }

        val regex = Regex(""""date"\s*:\s*"(\d{4}-\d{2}-\d{2})"""")
        regex.find(body)?.groupValues?.get(1)?.let { return it }
        Regex("""(\d{4}-\d{2}-\d{2})""").find(body)?.groupValues?.get(1)?.let { return it }
        throw Exception("无法解析最新日期")
    }

    private fun fetchDailyPage(date: String): String {
        val url = "$DAILY_URL/$date.html"
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("日报不存在 ($date)")
            response.body?.string() ?: throw Exception("empty daily page")
        }
    }

    private fun parseDailyHtml(html: String, date: String): DailyReport {
        val doc = Jsoup.parse(html)

        val keywords = doc.selectFirst(".keywords-banner")
            ?.ownText()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?: ""

        val dateCN = doc.selectFirst(".edition-date")?.text()?.trim() ?: date
        val editionNumber = doc.selectFirst(".edition-number")?.text()?.trim() ?: ""
        val mainHeadline = doc.selectFirst(".headline-section .main-headline")?.text()?.trim() ?: ""

        // Parse articles with severity
        val articles = doc.select(".articles-grid .article-card").map { card ->
            val title = card.selectFirst(".article-title")?.text()?.trim() ?: ""
            val sevElem = card.selectFirst(".severity-badge")
            val severity = if (sevElem != null) {
                val cssClass = sevElem.className()
                Severity.fromCssClass(cssClass)
            } else {
                Severity.NONE
            }
            val isFeatured = card.hasClass("featured")
            ArticleItem(title = title, severity = severity, isFeatured = isFeatured)
        }.filter { it.title.isNotBlank() }

        // Quick news (no severity, treated as NONE)
        val quickItems = doc.select(".quick-news .quick-item").map { item ->
            val title = item.selectFirst("h4")?.text()?.trim()
                ?: item.selectFirst("p")?.text()?.trim()
                ?: item.text().trim()
            ArticleItem(title = title, severity = Severity.NONE)
        }.filter { it.title.isNotBlank() }

        // Chart titles (for backward compat)
        val chartTitles = doc.select(".charts-grid .chart-container .chart-title")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        // Parse Mermaid chart data
        val charts = mutableListOf<ChartData>()
        val chartContainers = doc.select(".charts-grid .chart-container")
        for (container in chartContainers) {
            val title = container.selectFirst(".chart-title")?.text()?.trim() ?: continue
            val mermaidEl = container.selectFirst(".mermaid")
            if (mermaidEl != null) {
                val mermaidCode = mermaidEl.wholeText().trim()
                if (mermaidCode.isNotBlank()) {
                    val chartData = ChartData.parseMermaid(title, mermaidCode)
                    if (chartData != null) charts.add(chartData)
                }
            }
        }

        return DailyReport(
            date = date,
            dateCN = dateCN,
            editionNumber = editionNumber,
            keywords = keywords,
            mainHeadline = mainHeadline,
            articles = articles + quickItems,
            chartTitles = chartTitles,
            charts = charts
        )
    }
}
