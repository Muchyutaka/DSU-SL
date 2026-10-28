package vegabobo.dsusideloader.diagnostics

/**
 * Size bound helpers for diagnostic reports.
 *
 * Pure Kotlin so the bounds can be unit tested: a report must never grow with the
 * amount of log lines the device produced.
 */
object ReportFormatting {

    const val DEFAULT_MAX_SECTION_CHARS = 32_000
    const val DEFAULT_MAX_LINES = 400
    const val NOT_AVAILABLE = "not available"

    /**
     * Keeps the beginning of [text] (application logs are more useful at the
     * start) and appends a note when it was cut.
     */
    fun truncate(
        text: String?,
        maxChars: Int = DEFAULT_MAX_SECTION_CHARS,
        placeholder: String = NOT_AVAILABLE,
    ): String {
        if (text.isNullOrBlank()) {
            return placeholder
        }
        if (text.length <= maxChars) {
            return text
        }
        val omitted = text.length - maxChars
        return text.take(maxChars) + "\n… (truncated, $omitted characters omitted)"
    }

    /**
     * Keeps the last [maxLines] lines of [text], which is what matters for a
     * logcat excerpt.
     */
    fun truncateLines(
        text: String?,
        maxLines: Int = DEFAULT_MAX_LINES,
        placeholder: String = NOT_AVAILABLE,
    ): String {
        if (text.isNullOrBlank()) {
            return placeholder
        }
        val lines = text.lines()
        if (lines.size <= maxLines) {
            return text
        }
        val omitted = lines.size - maxLines
        return lines.takeLast(maxLines).joinToString("\n") + "\n… (truncated, $omitted lines omitted)"
    }
}
