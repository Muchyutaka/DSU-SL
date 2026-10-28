package vegabobo.dsusideloader.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallHistoryTest {

    private fun entry(
        timestamp: Long,
        result: InstallResult = InstallResult.SUCCESS,
        errorSummary: String = "",
    ) = InstallHistoryEntry(
        timestamp = timestamp,
        fileName = "system-2023-10-12.img",
        userdataGb = 4,
        imageSizeBytes = 2_147_483_648,
        result = result,
        errorSummary = errorSummary,
    )

    @Test
    fun newestEntryComesFirst() {
        val history = InstallHistoryOps.addEntry(listOf(entry(1)), entry(2))
        assertEquals(listOf(2L, 1L), history.map { it.timestamp })
    }

    @Test
    fun historyIsBounded() {
        var history = emptyList<InstallHistoryEntry>()
        repeat(40) { history = InstallHistoryOps.addEntry(history, entry(it.toLong())) }
        assertEquals(InstallHistoryOps.MAX_HISTORY_ENTRIES, history.size)
        assertEquals(39L, history.first().timestamp)
    }

    @Test
    fun errorSummaryIsBoundedAndSingleLine() {
        val longError = "E gsid: " + "x".repeat(500) + "\nsecond line"
        val saved = InstallHistoryOps.addEntry(emptyList(), entry(1, errorSummary = longError))
        val summary = saved.first().errorSummary
        assertTrue(summary.length <= InstallHistoryOps.MAX_ERROR_SUMMARY_LENGTH + 1)
        assertFalse(summary.contains("\n"))
    }

    @Test
    fun summarizeHandlesMissingInput() {
        assertEquals("", InstallHistoryOps.summarize(null))
        assertEquals("", InstallHistoryOps.summarize("   \n  "))
        assertEquals("boom", InstallHistoryOps.summarize("\n  boom \n"))
    }

    @Test
    fun presetsDeduplicateSameConfiguration() {
        val first = InstallPreset(id = "a", userdataGb = 4, imageSizeBytes = -1)
        val duplicate = InstallPreset(id = "b", userdataGb = 4, imageSizeBytes = -1)
        val other = InstallPreset(id = "c", userdataGb = 8, imageSizeBytes = -1)

        var presets = InstallHistoryOps.addPreset(emptyList(), first)
        presets = InstallHistoryOps.addPreset(presets, other)
        presets = InstallHistoryOps.addPreset(presets, duplicate)

        assertEquals(2, presets.size)
        assertEquals(listOf("b", "c"), presets.map { it.id })
    }

    @Test
    fun presetsAreBoundedAndRemovable() {
        var presets = emptyList<InstallPreset>()
        repeat(15) {
            presets = InstallHistoryOps.addPreset(
                presets,
                InstallPreset(id = "id$it", userdataGb = it.toLong(), imageSizeBytes = -1),
            )
        }
        assertEquals(InstallHistoryOps.MAX_PRESETS, presets.size)

        val removed = InstallHistoryOps.removePreset(presets, "id14")
        assertEquals(InstallHistoryOps.MAX_PRESETS - 1, removed.size)
        assertFalse(removed.any { it.id == "id14" })
    }

    @Test
    fun jsonRoundTrip() {
        val entries = listOf(entry(1), entry(2, result = InstallResult.ERROR, errorSummary = "boom"))
        val decoded = InstallHistoryCodec.decodeHistory(InstallHistoryCodec.encodeHistory(entries))
        assertEquals(entries, decoded)
    }

    @Test
    fun corruptedJsonDecodesToEmptyList() {
        assertEquals(emptyList<InstallHistoryEntry>(), InstallHistoryCodec.decodeHistory("{not json"))
        assertEquals(emptyList<InstallHistoryEntry>(), InstallHistoryCodec.decodeHistory(null))
        assertEquals(emptyList<InstallPreset>(), InstallHistoryCodec.decodePresets("[]"))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val raw = """{"entries":[{"timestamp":5,"futureField":"value"}]}"""
        val decoded = InstallHistoryCodec.decodeHistory(raw)
        assertEquals(1, decoded.size)
        assertEquals(5L, decoded.first().timestamp)
    }
}
