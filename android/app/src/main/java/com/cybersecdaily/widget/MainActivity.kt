package com.cybersecdaily.widget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsManager.init(this)
        setContent {
            CyberSecDailyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val baseUrl = "https://unclecheng-li.github.io/cybersecurity-daily"
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val showSeverity by SettingsManager.showSeverity.collectAsState()
    val showCritical by SettingsManager.showCritical.collectAsState()
    val showHigh by SettingsManager.showHigh.collectAsState()
    val showMedium by SettingsManager.showMedium.collectAsState()
    val showLow by SettingsManager.showLow.collectAsState()
    val showCharts by SettingsManager.showCharts.collectAsState()
    val showQuadrantChart by SettingsManager.showQuadrantChart.collectAsState()
    val showPieChart by SettingsManager.showPieChart.collectAsState()
    val headlineCount by SettingsManager.headlineCount.collectAsState()

    var report by remember { mutableStateOf<DailyReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshingWidget by remember { mutableStateOf(false) }

    fun loadReport() {
        scope.launch {
            isLoading = true
            report = withContext(Dispatchers.IO) { ReportFetcher.fetchLatest() }
            isLoading = false
        }
    }

    fun refreshWidget() {
        scope.launch {
            isRefreshingWidget = true
            CyberSecDailyWidgetReceiver.triggerRefresh(context)
            // Brief delay to let the Worker do its job, then clear refreshing state
            kotlinx.coroutines.delay(500)
            isRefreshingWidget = false
        }
    }

    LaunchedEffect(Unit) { loadReport() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a1a))
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = "\u7f51\u7edc\u5b89\u5168\u65e5\u62a5\u901f\u9012",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFc9a227),
            letterSpacing = 4.sp
        )
        Text(
            text = "CYBERSECURITY DAILY EXPRESS",
            fontSize = 12.sp,
            color = Color(0xFF888888),
            letterSpacing = 6.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ---- Today keywords banner ----
        val kw = report?.keywords ?: ""
        if (kw.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC41E3A))
            ) {
                Text(
                    text = "\u4eca\u65e5\u5173\u952e\u8bcd\uff1a$kw",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    lineHeight = 20.sp,
                    maxLines = 3
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        } else if (isLoading) {
            Text(
                text = "\u52a0\u8f7d\u4e2d...",
                color = Color(0xFF888888),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ---- Settings card ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2d2d2d))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "\u5c0f\u7ec4\u4ef6\u8bbe\u7f6e",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFc9a227)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ---- Severity master toggle ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\u663e\u793a\u6f0f\u6d1e\u8bc4\u7ea7", color = Color(0xFFe8e0d0), fontSize = 14.sp)
                        Text(
                            "\u5728\u6587\u7ae0\u6807\u9898\u524d\u663e\u793a\u4e25\u91cd/\u9ad8\u5371\u7b49\u6807\u7b7e",
                            color = Color(0xFF888888), fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = showSeverity,
                        onCheckedChange = { SettingsManager.setShowSeverity(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFc9a227),
                            checkedTrackColor = Color(0xFFc9a227).copy(alpha = 0.3f)
                        )
                    )
                }

                // ---- Severity sub-toggles ----
                AnimatedVisibility(
                    visible = showSeverity,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                        SeveritySubToggle(
                            label = "\u4e25\u91cd",
                            desc = "\u4ec5\u663e\u793a\u4e25\u91cd\u7ea7\u522b\u6f0f\u6d1e",
                            color = Color(0xFFC41E3A),
                            checked = showCritical,
                            onChange = { SettingsManager.setShowCritical(it) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SeveritySubToggle(
                            label = "\u9ad8\u5371",
                            desc = "\u4ec5\u663e\u793a\u9ad8\u5371\u7ea7\u522b\u6f0f\u6d1e",
                            color = Color(0xFFE85D04),
                            checked = showHigh,
                            onChange = { SettingsManager.setShowHigh(it) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SeveritySubToggle(
                            label = "\u4e2d\u5371",
                            desc = "\u4ec5\u663e\u793a\u4e2d\u5371\u7ea7\u522b\u6f0f\u6d1e",
                            color = Color(0xFFC9A227),
                            checked = showMedium,
                            onChange = { SettingsManager.setShowMedium(it) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SeveritySubToggle(
                            label = "\u4f4e\u5371",
                            desc = "\u4ec5\u663e\u793a\u4f4e\u5371\u7ea7\u522b\u6f0f\u6d1e",
                            color = Color(0xFF888888),
                            checked = showLow,
                            onChange = { SettingsManager.setShowLow(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ---- Charts master toggle ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\u663e\u793a\u98ce\u9669\u56fe\u8868", color = Color(0xFFe8e0d0), fontSize = 14.sp)
                        Text("\u5728\u5c0f\u7ec4\u4ef6\u4e2d\u663e\u793a\u98ce\u9669\u5206\u6790\u56fe\u8868", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                    Switch(
                        checked = showCharts,
                        onCheckedChange = { SettingsManager.setShowCharts(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFc9a227),
                            checkedTrackColor = Color(0xFFc9a227).copy(alpha = 0.3f)
                        )
                    )
                }

                // ---- Chart sub-toggles ----
                AnimatedVisibility(
                    visible = showCharts,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\u6f0f\u6d1e\u4e25\u91cd\u6027\u8c61\u9650\u56fe", color = Color(0xFFe8e0d0), fontSize = 13.sp)
                                Text("\u6f0f\u6d1e\u4e25\u91cd\u6027 \u00d7 \u5229\u7528\u72b6\u6001", color = Color(0xFF888888), fontSize = 10.sp)
                            }
                            Switch(
                                checked = showQuadrantChart,
                                onCheckedChange = { SettingsManager.setShowQuadrantChart(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE85D04), checkedTrackColor = Color(0xFFE85D04).copy(alpha = 0.3f))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\u5b89\u5168\u4e8b\u4ef6\u7c7b\u578b\u5206\u5e03", color = Color(0xFFe8e0d0), fontSize = 13.sp)
                                Text("\u5b89\u5168\u4e8b\u4ef6\u7c7b\u578b\u5206\u5e03\u997c\u56fe", color = Color(0xFF888888), fontSize = 10.sp)
                            }
                            Switch(
                                checked = showPieChart,
                                onCheckedChange = { SettingsManager.setShowPieChart(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE85D04), checkedTrackColor = Color(0xFFE85D04).copy(alpha = 0.3f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ---- Headline count ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\u6587\u7ae0\u6807\u9898\u6570\u91cf", color = Color(0xFFe8e0d0), fontSize = 14.sp)
                        Text("\u5f53\u524d: $headlineCount \u6761", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1", color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                        Slider(
                            value = headlineCount.toFloat(),
                            onValueChange = { SettingsManager.setHeadlineCount(it.toInt()) },
                            valueRange = 1f..10f, steps = 8,
                            modifier = Modifier.width(120.dp),
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFc9a227), activeTrackColor = Color(0xFFc9a227))
                        )
                        Text("10", color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---- Preview ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2d2d2d))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("\u9884\u89c8\u6548\u679c", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFc9a227))
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showSeverity && showCritical) {
                        Text(
                            text = "\u4e25\u91cd", color = Color(0xFFC41E3A), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color(0xFFC41E3A).copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("CVE-2026-XXXXX \u793a\u4f8b\u6f0f\u6d1e\u6807\u9898", color = Color(0xFFe8e0d0), fontSize = 13.sp, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showSeverity && showHigh) {
                        Text(
                            text = "\u9ad8\u5371", color = Color(0xFFE85D04), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color(0xFFE85D04).copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("\u53e6\u4e00\u4e2a\u5b89\u5168\u4e8b\u4ef6\u793a\u4f8b\u6807\u9898", color = Color(0xFFe8e0d0), fontSize = 13.sp, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---- Action buttons ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { loadReport(); refreshWidget() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE85D04)),
                enabled = !isLoading && !isRefreshingWidget
            ) {
                Text(
                    if (isRefreshingWidget) "\u5237\u65b0\u4e2d..." else "\u5237\u65b0\u5c0f\u7ec4\u4ef6",
                    color = Color.White, fontSize = 13.sp
                )
            }
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(baseUrl))) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFc9a227))
            ) {
                Text("\u6253\u5f00\u65e5\u62a5\u9996\u9875", color = Color(0xFF1a1a1a), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "AI\u57fa \u00b7 \u7f51\u7edc\u5b89\u5168\u65e5\u62a5", color = Color(0xFF666666), fontSize = 12.sp)
        Text(text = "v1.0.6", color = Color(0xFF555555), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SeveritySubToggle(label: String, desc: String, color: Color, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Color(0xFF888888), fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = color, checkedTrackColor = color.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun CyberSecDailyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFc9a227),
            secondary = Color(0xFFc41e3a),
            background = Color(0xFF1a1a1a),
            surface = Color(0xFF2d2d2d),
            onPrimary = Color(0xFF1a1a1a),
            onBackground = Color(0xFFe8e0d0),
            onSurface = Color(0xFFe8e0d0)
        ),
        content = content
    )
}
