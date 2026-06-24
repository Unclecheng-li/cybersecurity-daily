package com.cybersecdaily.widget

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsManager {

    private const val PREFS_NAME = "cybersec_daily_settings"
    private const val KEY_SHOW_SEVERITY = "show_severity"
    private const val KEY_SHOW_CRITICAL = "show_critical"
    private const val KEY_SHOW_HIGH = "show_high"
    private const val KEY_SHOW_MEDIUM = "show_medium"
    private const val KEY_SHOW_LOW = "show_low"
    private const val KEY_SHOW_CHARTS = "show_charts"
    private const val KEY_SHOW_QUADRANT = "show_quadrant"
    private const val KEY_SHOW_PIE = "show_pie"
    private const val KEY_HEADLINE_COUNT = "headline_count"
    private const val KEY_LAST_KNOWN_DATE = "last_known_date"

    private val _showSeverity = MutableStateFlow(true)
    val showSeverity: StateFlow<Boolean> = _showSeverity.asStateFlow()

    private val _showCritical = MutableStateFlow(true)
    val showCritical: StateFlow<Boolean> = _showCritical.asStateFlow()

    private val _showHigh = MutableStateFlow(true)
    val showHigh: StateFlow<Boolean> = _showHigh.asStateFlow()

    private val _showMedium = MutableStateFlow(true)
    val showMedium: StateFlow<Boolean> = _showMedium.asStateFlow()

    private val _showLow = MutableStateFlow(true)
    val showLow: StateFlow<Boolean> = _showLow.asStateFlow()

    private val _showCharts = MutableStateFlow(true)
    val showCharts: StateFlow<Boolean> = _showCharts.asStateFlow()

    private val _showQuadrantChart = MutableStateFlow(true)
    val showQuadrantChart: StateFlow<Boolean> = _showQuadrantChart.asStateFlow()

    private val _showPieChart = MutableStateFlow(true)
    val showPieChart: StateFlow<Boolean> = _showPieChart.asStateFlow()

    private val _headlineCount = MutableStateFlow(5)
    val headlineCount: StateFlow<Int> = _headlineCount.asStateFlow()

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _showSeverity.value = prefs.getBoolean(KEY_SHOW_SEVERITY, true)
        _showCritical.value = prefs.getBoolean(KEY_SHOW_CRITICAL, true)
        _showHigh.value = prefs.getBoolean(KEY_SHOW_HIGH, true)
        _showMedium.value = prefs.getBoolean(KEY_SHOW_MEDIUM, true)
        _showLow.value = prefs.getBoolean(KEY_SHOW_LOW, true)
        _showCharts.value = prefs.getBoolean(KEY_SHOW_CHARTS, true)
        _showQuadrantChart.value = prefs.getBoolean(KEY_SHOW_QUADRANT, true)
        _showPieChart.value = prefs.getBoolean(KEY_SHOW_PIE, true)
        _headlineCount.value = prefs.getInt(KEY_HEADLINE_COUNT, 5)
    }

    var lastKnownDate: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_LAST_KNOWN_DATE, "") ?: "" else ""
        set(value) {
            if (::prefs.isInitialized) prefs.edit().putString(KEY_LAST_KNOWN_DATE, value).apply()
        }

    fun setShowSeverity(value: Boolean) {
        _showSeverity.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SEVERITY, value).apply()
    }

    fun setShowCritical(value: Boolean) {
        _showCritical.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_CRITICAL, value).apply()
    }

    fun setShowHigh(value: Boolean) {
        _showHigh.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_HIGH, value).apply()
    }

    fun setShowMedium(value: Boolean) {
        _showMedium.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_MEDIUM, value).apply()
    }

    fun setShowLow(value: Boolean) {
        _showLow.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_LOW, value).apply()
    }

    fun setShowCharts(value: Boolean) {
        _showCharts.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_CHARTS, value).apply()
    }

    fun setShowQuadrantChart(value: Boolean) {
        _showQuadrantChart.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_QUADRANT, value).apply()
    }

    fun setShowPieChart(value: Boolean) {
        _showPieChart.value = value
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_PIE, value).apply()
    }

    fun setHeadlineCount(value: Int) {
        _headlineCount.value = value.coerceIn(1, 10)
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_HEADLINE_COUNT, value.coerceIn(1, 10)).apply()
    }

    /** Check if a severity level should be displayed */
    fun isSeverityVisible(severity: Severity): Boolean {
        if (!showSeverity.value) return false
        if (severity == Severity.NONE) return true // Quick news always visible
        return when (severity) {
            Severity.CRITICAL -> showCritical.value
            Severity.HIGH -> showHigh.value
            Severity.MEDIUM -> showMedium.value
            Severity.LOW -> showLow.value
            Severity.NONE -> true
        }
    }
}
