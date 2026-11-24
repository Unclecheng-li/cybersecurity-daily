package com.cybersecdaily.widget

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.IOException

internal class WidgetReportCache(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "widget-report.html"))

    fun read(): DailyReport? = synchronized(fileLock) {
        try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
                val date = reader.readLine() ?: return@synchronized null
                if (!ReportFetcher.isReportDate(date)) return@synchronized null
                ReportFetcher.parseDailyHtml(reader.readText(), date)
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
    }

    fun save(date: String, html: String) = synchronized(fileLock) {
        try {
            val stream = file.startWrite()
            try {
                stream.write("$date\n$html".toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (e: IOException) {
                file.failWrite(stream)
                throw e
            }
        } catch (e: IOException) {
            Log.w("WidgetReportCache", "Could not save the widget report", e)
        }
    }

    private companion object {
        val fileLock = Any()
    }
}
