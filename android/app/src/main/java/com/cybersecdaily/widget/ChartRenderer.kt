package com.cybersecdaily.widget

import android.graphics.*
import java.io.ByteArrayOutputStream

object ChartRenderer {

    private val bgColor = 0xFF1a1a1a.toInt()
    private val goldColor = 0xFFc9a227.toInt()
    private val redColor = 0xFFC41E3A.toInt()
    private val orangeColor = 0xFFE85D04.toInt()
    private val textPrimary = 0xFFe8e0d0.toInt()
    private val textSecondary = 0xFF999999.toInt()
    private val gridColor = 0xFF3a3a3a.toInt()
    private val plotBgColor = 0xFF1e1e1e.toInt()
    private val pointColors = intArrayOf(
        0xFFC41E3A.toInt(), 0xFFE85D04.toInt(), 0xFFc9a227.toInt(),
        0xFF4A90D9.toInt(), 0xFF50C878.toInt()
    )
    private val pieColors = pointColors

    fun renderQuadrantChart(chart: ChartData.QuadrantChart, width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        // Title
        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = goldColor; it.textSize = 22f; it.typeface = Typeface.DEFAULT_BOLD
            it.textAlign = Paint.Align.CENTER
        }
        canvas.drawText(chart.title, width / 2f, 22f, titleP)

        // Plot area
        val plotLeft = 52f; val plotTop = 34f
        val plotRight = width - 16f; val plotBottom = height - 22f
        val plotW = plotRight - plotLeft; val plotH = plotBottom - plotTop
        val cx = plotLeft + plotW / 2f; val cy = plotTop + plotH / 2f

        // Background
        val bgP = Paint().also { it.color = plotBgColor }
        canvas.drawRect(plotLeft, plotTop, plotRight, plotBottom, bgP)

        // Grid
        val gridP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = gridColor; it.strokeWidth = 0.5f; it.style = Paint.Style.STROKE
        }
        for (i in 1..3) {
            val gx = plotLeft + plotW * i / 4f; val gy = plotTop + plotH * i / 4f
            canvas.drawLine(gx, plotTop, gx, plotBottom, gridP)
            canvas.drawLine(plotLeft, gy, plotRight, gy, gridP)
        }

        // Axes
        val axisP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = 0xFF555555.toInt(); it.strokeWidth = 1.5f; it.style = Paint.Style.STROKE
        }
        canvas.drawLine(cx, plotTop, cx, plotBottom, axisP)
        canvas.drawLine(plotLeft, cy, plotRight, cy, axisP)

        // Axis end labels
        val alLowP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = textSecondary; it.textSize = 12f; it.textAlign = Paint.Align.CENTER
        }
        canvas.drawText("\u4f4e", plotLeft + 12f, plotBottom + 14f, alLowP)
        canvas.drawText("\u9ad8", plotRight - 12f, plotBottom + 14f, alLowP)

        val alLeftP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = textSecondary; it.textSize = 12f; it.textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("\u4f4e", plotLeft - 6f, plotBottom + 8f, alLeftP)
        canvas.drawText("\u9ad8", plotLeft - 6f, plotTop + 4f, alLeftP)

        // Quadrant labels
        val qP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = 0x66e8e0d0.toInt(); it.textSize = 12f; it.textAlign = Paint.Align.CENTER
        }
        if (chart.quadrants.size >= 4) {
            canvas.drawText(chart.quadrants[0], plotLeft + plotW * 0.25f, cy - 10f, qP)
            canvas.drawText(chart.quadrants[1], plotLeft + plotW * 0.75f, cy - 10f, qP)
            canvas.drawText(chart.quadrants[2], plotLeft + plotW * 0.25f, plotBottom - 4f, qP)
            canvas.drawText(chart.quadrants[3], plotLeft + plotW * 0.75f, plotBottom - 4f, qP)
        }

        // Data points
        val dotRadius = 7f
        val labelPositions = mutableListOf<RectF>()

        for ((idx, pt) in chart.points.withIndex()) {
            val px = plotLeft + pt.x * plotW
            val py = plotBottom - pt.y * plotH
            val color = pointColors[idx % pointColors.size]

            // Dot fill
            val dotP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = color; it.style = Paint.Style.FILL }
            canvas.drawCircle(px, py, dotRadius, dotP)
            // Dot border
            val borderP = Paint(Paint.ANTI_ALIAS_FLAG).also {
                it.color = 0xFFFFFFFF.toInt(); it.style = Paint.Style.STROKE; it.strokeWidth = 1.5f
            }
            canvas.drawCircle(px, py, dotRadius, borderP)

            // Label placement with collision avoidance
            val shortLabel = pt.label.take(16)
            val labelP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = textPrimary; it.textSize = 11f }
            val labelW = labelP.measureText(shortLabel) + 4f
            val labelH = 14f

            val candidates = listOf(
                RectF(px + 12f, py - 7f, px + 12f + labelW, py + 7f),
                RectF(px - 12f - labelW, py - 7f, px - 12f, py + 7f),
                RectF(px - labelW / 2f, py - 18f - labelH, px + labelW / 2f, py - 6f),
                RectF(px - labelW / 2f, py + 12f, px + labelW / 2f, py + 12f + labelH)
            )

            var chosen: RectF = candidates[0]
            for (cand in candidates) {
                var overlaps = false
                for (existing in labelPositions) {
                    if (RectF.intersects(existing, cand)) { overlaps = true; break }
                }
                if (!overlaps) { chosen = cand; break }
            }

            labelPositions.add(chosen)
            canvas.drawText(shortLabel, chosen.left + 2f, chosen.bottom - 2f, labelP)
        }

        return bitmapToPng(bitmap)
    }

    fun renderPieChart(chart: ChartData.PieChart, width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = goldColor; it.textSize = 22f; it.typeface = Typeface.DEFAULT_BOLD
            it.textAlign = Paint.Align.CENTER
        }
        canvas.drawText(chart.title, width / 2f, 22f, titleP)

        val total = chart.slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) return bitmapToPng(bitmap)

        val cx = width * 0.38f; val cy = height / 2f + 6f
        val radius = minOf(width, height) * 0.28f
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val legendStartX = cx + radius + 16f
        var legendY = cy - radius + 12f
        val legendP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.textSize = 12f }

        var startAngle = -90f
        for ((idx, slice) in chart.slices.withIndex()) {
            val sweep = (slice.value / total) * 360f
            val color = pieColors[idx % pieColors.size]

            val sliceP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = color; it.style = Paint.Style.FILL }
            canvas.drawArc(oval, startAngle, sweep, true, sliceP)

            val borderP = Paint(Paint.ANTI_ALIAS_FLAG).also {
                it.color = bgColor; it.style = Paint.Style.STROKE; it.strokeWidth = 1.5f
            }
            canvas.drawArc(oval, startAngle, sweep, true, borderP)

            val pct = ((slice.value / total) * 100).toInt()
            val lbl = "${slice.label.take(8)} $pct%"
            if (legendY < height - 14f) {
                legendP.color = color
                val dotP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = color; it.style = Paint.Style.FILL }
                canvas.drawCircle(legendStartX - 6f, legendY - 4f, 4f, dotP)
                canvas.drawText(lbl, legendStartX + 4f, legendY, legendP)
                legendY += 20f
            }
            startAngle += sweep
        }
        return bitmapToPng(bitmap)
    }

    fun renderFlowchart(chart: ChartData.FlowChart, width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = goldColor; it.textSize = 22f; it.typeface = Typeface.DEFAULT_BOLD
            it.textAlign = Paint.Align.CENTER
        }
        canvas.drawText(chart.title, width / 2f, 22f, titleP)

        val nodes = parseFlowchartNodes(chart.mermaidCode)
        if (nodes.isEmpty()) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = textSecondary; it.textSize = 14f; it.textAlign = Paint.Align.CENTER }
            canvas.drawText("(flowchart)", width / 2f, height / 2f, p)
            return bitmapToPng(bitmap)
        }

        val nodeTextP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = textPrimary; it.textSize = 12f; it.textAlign = Paint.Align.CENTER }
        val arrowP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = orangeColor; it.strokeWidth = 1.5f; it.style = Paint.Style.STROKE }
        val nodeW = 120f; val nodeH = 30f; val startY = 44f
        val spacing = minOf((height - startY - 12f) / nodes.size.coerceAtLeast(1), 42f)

        for ((idx, node) in nodes.withIndex()) {
            val nx = (width - nodeW) / 2f; val ny = startY + idx * spacing
            val nodeColor = when { idx == 0 -> redColor; idx == nodes.size - 1 -> orangeColor; else -> 0xFF333333.toInt() }
            val rectP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = nodeColor; it.style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(nx, ny, nx + nodeW, ny + nodeH), 6f, 6f, rectP)
            canvas.drawText(node.take(14), nx + nodeW / 2f, ny + nodeH / 2f + 4f, nodeTextP)
            if (idx < nodes.size - 1) {
                val ax = nx + nodeW / 2f
                canvas.drawLine(ax, ny + nodeH, ax, ny + spacing, arrowP)
            }
        }
        return bitmapToPng(bitmap)
    }

    fun renderTimeline(chart: ChartData.Timeline, width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = goldColor; it.textSize = 22f; it.typeface = Typeface.DEFAULT_BOLD
            it.textAlign = Paint.Align.CENTER
        }
        canvas.drawText(chart.title, width / 2f, 22f, titleP)

        val entries = parseTimelineEntries(chart.mermaidCode)
        val entryP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = textPrimary; it.textSize = 12f }
        val dotP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = goldColor; it.style = Paint.Style.FILL }
        val lineP = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = gridColor; it.strokeWidth = 1.5f }

        val lineX = 32f; val lineTop = 44f
        canvas.drawLine(lineX, lineTop, lineX, height - 14f, lineP)

        val maxEntries = minOf(entries.size, 5)
        val spacing = (height - 56f) / maxEntries.coerceAtLeast(1)
        for ((idx, entry) in entries.take(maxEntries).withIndex()) {
            val y = lineTop + idx * spacing + 16f
            canvas.drawCircle(lineX, y, 5f, dotP)
            canvas.drawText(entry.take(22), lineX + 16f, y + 4f, entryP)
        }
        return bitmapToPng(bitmap)
    }

    private fun parseFlowchartNodes(code: String): List<String> {
        val nodes = mutableListOf<String>()
        Regex("""(\w+)\[([^\]]+)\]""").findAll(code).forEach { nodes.add(it.groupValues[2]) }
        return nodes
    }

    private fun parseTimelineEntries(code: String): List<String> {
        val entries = mutableListOf<String>()
        for (line in code.lines()) {
            val t = line.trim()
            if (t.startsWith(":")) entries.add(t.removePrefix(":").trim())
            else if (":" in t && !t.startsWith("timeline") && !t.startsWith("title")) {
                val parts = t.split(":", limit = 2)
                if (parts.size == 2 && parts[1].trim().isNotBlank()) entries.add(parts[1].trim())
            }
        }
        return entries
    }

    private fun bitmapToPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }
}
