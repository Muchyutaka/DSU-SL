package vegabobo.dsusideloader.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportFormattingTest {

    @Test
    fun truncateKeepsShortInputUnchanged() {
        val text = "E gsid: free space 24% is below the minimum threshold of 40%"
        assertEquals(text, ReportFormatting.truncate(text))
    }

    @Test
    fun truncateUsesPlaceholderForMissingInput() {
        assertEquals(ReportFormatting.NOT_AVAILABLE, ReportFormatting.truncate(null))
        assertEquals(ReportFormatting.NOT_AVAILABLE, ReportFormatting.truncate(""))
        assertEquals(ReportFormatting.NOT_AVAILABLE, ReportFormatting.truncate("   "))
    }

    @Test
    fun truncateBoundsTheSection() {
        val text = "a".repeat(100)
        val truncated = ReportFormatting.truncate(text, maxChars = 10)
        assertTrue(truncated.startsWith("a".repeat(10)))
        assertTrue(truncated.contains("90 characters omitted"))
        assertTrue(truncated.length < text.length + 40)
    }

    @Test
    fun truncateLinesKeepsTheTail() {
        val text = (1..10).joinToString("\n") { "line$it" }
        val truncated = ReportFormatting.truncateLines(text, maxLines = 3)
        assertTrue(truncated.startsWith("line8"))
        assertTrue(truncated.contains("line10"))
        assertTrue(truncated.contains("7 lines omitted"))
        assertFalse(truncated.contains("line1\n"))
    }

    @Test
    fun truncateLinesKeepsShortInputUnchanged() {
        val text = "line1\nline2"
        assertEquals(text, ReportFormatting.truncateLines(text, maxLines = 3))
        assertEquals(ReportFormatting.NOT_AVAILABLE, ReportFormatting.truncateLines(null))
    }
}
