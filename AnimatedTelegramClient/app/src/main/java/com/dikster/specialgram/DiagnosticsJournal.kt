package com.dikster.specialgram

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Private, bounded diagnostic journal. It deliberately stores no chat/user IDs,
 * names, message text, phone numbers, or raw server error descriptions.
 */
object DiagnosticsJournal {
    private const val FILE_NAME = "specialgram-diagnostics.log"
    private const val MAX_LINES = 300

    @Synchronized
    fun record(context: Context, request: String, code: Int, message: String = "") {
        runCatching {
            val safeRequest = request.filter { it.isLetterOrDigit() || it == '_' }.take(64).ifBlank { "UnknownRequest" }
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            val existing = if (file.exists()) file.readLines(Charsets.UTF_8) else emptyList()
            val safeMessage = message
                .replace(Regex("\\b\\d{5,}\\b"), "[number]")
                .replace(Regex("[\\r\\n|]"), " ")
                .filter { it.isLetterOrDigit() || it.isWhitespace() || it in "._:-[]" }
                .take(120)
                .trim()
            val detail = if (safeMessage.isBlank()) "" else " | message=$safeMessage"
            val updated = (existing + "$timestamp | TDLib | $safeRequest | code=$code$detail").takeLast(MAX_LINES)
            file.writeText(updated.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
        }
    }

    @Synchronized
    fun read(context: Context): String = runCatching {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (file.exists()) file.readText(Charsets.UTF_8).takeLast(64 * 1024) else ""
    }.getOrDefault("")

    @Synchronized
    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, FILE_NAME).delete() }
    }
}
