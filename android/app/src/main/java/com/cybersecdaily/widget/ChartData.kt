package com.cybersecdaily.widget

/**
 * Parsed Mermaid chart data for rendering.
 */
sealed class ChartData {
    abstract val title: String
    
    data class QuadrantChart(
        override val title: String,
        val xAxisLabel: String,
        val yAxisLabel: String,
        val quadrants: List<String>,
        val points: List<QuadrantPoint>
    ) : ChartData()
    
    data class PieChart(
        override val title: String,
        val slices: List<PieSlice>
    ) : ChartData()
    
    data class FlowChart(
        override val title: String,
        val mermaidCode: String
    ) : ChartData()
    
    data class Timeline(
        override val title: String,
        val mermaidCode: String
    ) : ChartData()
    
    data class QuadrantPoint(
        val label: String,
        val x: Float,   // 0.0 - 1.0
        val y: Float    // 0.0 - 1.0
    )
    
    data class PieSlice(
        val label: String,
        val value: Float
    )
    
    companion object {
        fun parseMermaid(title: String, mermaidCode: String): ChartData? {
            val trimmed = mermaidCode.trim()
            return when {
                trimmed.startsWith("quadrantChart") -> parseQuadrant(title, trimmed)
                trimmed.startsWith("pie") -> parsePie(title, trimmed)
                trimmed.startsWith("flowchart") -> FlowChart(title, trimmed)
                trimmed.startsWith("timeline") -> Timeline(title, trimmed)
                else -> null
            }
        }
        
        private fun parseQuadrant(title: String, code: String): QuadrantChart {
            val lines = code.lines().map { it.trim() }.filter { it.isNotBlank() }
            var xLabel = ""
            var yLabel = ""
            val quadrants = mutableListOf<String>()
            val points = mutableListOf<QuadrantPoint>()
            
            for (line in lines) {
                when {
                    line.startsWith("x-axis") -> {
                        xLabel = line.removePrefix("x-axis").trim().removeSurrounding("\"")
                    }
                    line.startsWith("y-axis") -> {
                        yLabel = line.removePrefix("y-axis").trim().removeSurrounding("\"")
                    }
                    line.startsWith("quadrant-") -> {
                        quadrants.add(line.substringAfter(" ").trim().removeSurrounding("\""))
                    }
                    line.startsWith("\"") -> {
                        // "Label": [x, y]
                        val labelEnd = line.indexOf("\"", 1)
                        if (labelEnd > 0) {
                            val label = line.substring(1, labelEnd)
                            val bracketContent = line.substringAfter("[").substringBefore("]")
                            val parts = bracketContent.split(",").map { it.trim().toFloatOrNull() ?: 0f }
                            if (parts.size >= 2) {
                                points.add(QuadrantPoint(label, parts[0], parts[1]))
                            }
                        }
                    }
                }
            }
            return QuadrantChart(title, xLabel, yLabel, quadrants, points)
        }
        
        private fun parsePie(title: String, code: String): PieChart {
            val slices = mutableListOf<PieSlice>()
            val lines = code.lines().map { it.trim() }.filter { it.isNotBlank() }
            
            for (line in lines) {
                if (line.startsWith("pie") || line.startsWith("title")) continue
                // Format: "label" : value
                val colonIdx = line.lastIndexOf(":")
                if (colonIdx > 0) {
                    val label = line.substring(0, colonIdx).trim().removeSurrounding("\"")
                    val value = line.substring(colonIdx + 1).trim().toFloatOrNull() ?: 0f
                    slices.add(PieSlice(label, value))
                }
            }
            return PieChart(title, slices)
        }
    }
}
