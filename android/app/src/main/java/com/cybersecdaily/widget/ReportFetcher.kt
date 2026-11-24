package com.cybersecdaily.widget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

object ReportFetcher {

    private const val BASE_URL = "https://unclecheng-li.github.io/cybersecurity-daily"
    private const val INDEX_URL = "$BASE_URL/index.html"
    private const val DAILY_URL = "$BASE_URL/daily"
    private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
    private val keywordsLabel = Regex("^(?:今日|本期)?关键词\\s*[:：]\\s*")

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchLatest(): DailyReport = withContext(Dispatchers.IO) {
        try {
            val date = fetchLatestDateOnly()
            parseDailyHtml(fetchDailyPage(date), date)
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
        regex.findAll(body).map { it.groupValues[1] }.firstOrNull(::isReportDate)?.let { return it }
        datePattern.findAll(body).map { it.value }.firstOrNull(::isReportDate)?.let { return it }
        throw Exception("无法解析最新日期")
    }

    internal fun isReportDate(date: String): Boolean {
        if (!datePattern.matches(date)) return false
        return try {
            LocalDate.parse(date)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    internal fun reportUrl(date: String): String =
        if (isReportDate(date)) "$DAILY_URL/$date.html" else BASE_URL

    internal fun fetchDailyPage(date: String): String {
        require(isReportDate(date))
        val request = Request.Builder().url(reportUrl(date)).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("日报不存在 ($date)")
            response.body?.string() ?: throw Exception("empty daily page")
        }
    }

    internal fun parseDailyHtml(html: String, date: String): DailyReport {
        val doc = Jsoup.parse(html)

        val keywords = doc.selectFirst(".keywords-banner")
            ?.text()
            .orEmpty()
            .replaceFirst(keywordsLabel, "")
            .trim()

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

        val report = DailyReport(
            date = date,
            dateCN = dateCN,
            editionNumber = editionNumber,
            keywords = keywords,
            mainHeadline = mainHeadline,
            articles = articles + quickItems,
            chartTitles = chartTitles,
            charts = charts
        )
        check(report.hasContent) { "日报内容为空 ($date)" }
        return report
    }
}
