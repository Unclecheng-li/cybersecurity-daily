package com.cybersecdaily.widget

data class DailyReport(
    val date: String = "",
    val dateCN: String = "",
    val editionNumber: String = "",
    val keywords: String = "",
    val mainHeadline: String = "",
    val articles: List<ArticleItem> = emptyList(),
    val chartTitles: List<String> = emptyList(),
    val charts: List<ChartData> = emptyList(),
    val error: String? = null
) {
    val headlines: List<String> get() = articles.map { it.title }
    val hasContent: Boolean get() = keywords.isNotEmpty() || articles.isNotEmpty()
    val hasCharts: Boolean get() = charts.isNotEmpty()

    companion object {
        fun error(msg: String) = DailyReport(error = msg)
    }
}
