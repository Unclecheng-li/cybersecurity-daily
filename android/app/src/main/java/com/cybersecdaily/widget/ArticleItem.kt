package com.cybersecdaily.widget

enum class Severity(val label: String, val color: Int) {
    CRITICAL("严重", 0xFFC41E3A.toInt()),
    HIGH("高危", 0xFFE85D04.toInt()),
    MEDIUM("中危", 0xFFC9A227.toInt()),
    LOW("低危", 0xFF888888.toInt()),
    NONE("", 0xFF888888.toInt());

    companion object {
        fun fromCssClass(cssClass: String): Severity = when {
            cssClass.contains("critical", ignoreCase = true) -> CRITICAL
            cssClass.contains("high", ignoreCase = true) -> HIGH
            cssClass.contains("medium", ignoreCase = true) -> MEDIUM
            cssClass.contains("low", ignoreCase = true) -> LOW
            else -> NONE
        }

        fun fromLabel(label: String): Severity = when {
            label.contains("严重") || label.contains("critical", ignoreCase = true) -> CRITICAL
            label.contains("高危") || label.contains("high", ignoreCase = true) -> HIGH
            label.contains("中危") || label.contains("medium", ignoreCase = true) -> MEDIUM
            label.contains("低危") || label.contains("低") || label.contains("low", ignoreCase = true) -> LOW
            else -> NONE
        }
    }
}

data class ArticleItem(
    val title: String,
    val severity: Severity = Severity.NONE,
    val isFeatured: Boolean = false
)
