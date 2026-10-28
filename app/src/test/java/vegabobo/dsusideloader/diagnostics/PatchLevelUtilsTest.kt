package vegabobo.dsusideloader.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatchLevelUtilsTest {

    @Test
    fun parsesDevicePatchLevelFormats() {
        val expected = PatchLevel(2023, 8, 5)
        assertEquals(expected, PatchLevelUtils.parse("2023-08-05"))
        assertEquals(expected, PatchLevelUtils.parse("2023-08-05 00:00:00"))
        assertEquals(expected, PatchLevelUtils.parse("2023/08/05"))
        assertEquals(expected, PatchLevelUtils.parse("2023.08.05"))
        assertEquals(expected, PatchLevelUtils.parse("20230805"))
    }

    @Test
    fun rejectsUnparseableDevicePatchLevel() {
        assertNull(PatchLevelUtils.parse(null))
        assertNull(PatchLevelUtils.parse(""))
        assertNull(PatchLevelUtils.parse("unknown"))
        assertNull(PatchLevelUtils.parse("2023-13-05"))
        assertNull(PatchLevelUtils.parse("2023-08-32"))
        assertNull(PatchLevelUtils.parse("1999-08-05"))
    }

    @Test
    fun parsesCompactDateFromFileName() {
        assertEquals(
            PatchLevel(2023, 10, 12),
            PatchLevelUtils.parseFromFileName("lineage-20.0-20231012-UNOFFICIAL-arm64_ab.img"),
        )
    }

    @Test
    fun parsesDashedDateFromFileName() {
        assertEquals(
            PatchLevel(2023, 10, 12),
            PatchLevelUtils.parseFromFileName("system-2023-10-12.img.gz"),
        )
    }

    @Test
    fun returnsNullWhenFileNameHasNoDate() {
        assertNull(PatchLevelUtils.parseFromFileName(null))
        assertNull(PatchLevelUtils.parseFromFileName(""))
        assertNull(PatchLevelUtils.parseFromFileName("system.img"))
        assertNull(PatchLevelUtils.parseFromFileName("system-roar-arm64-ab-vndklite-gapps.img"))
        // Not a valid month, so it must not be treated as a date.
        assertNull(PatchLevelUtils.parseFromFileName("build-20239999.img"))
    }

    @Test
    fun comparesPatchLevels() {
        val device = PatchLevel(2023, 8, 5)
        assertEquals(
            PatchLevelComparison.GSI_OLDER,
            PatchLevelUtils.compare(device, PatchLevel(2023, 7, 1)),
        )
        // Same month, different day: security patch levels are monthly, so this
        // must not be reported as older.
        assertEquals(
            PatchLevelComparison.GSI_SAME_OR_NEWER,
            PatchLevelUtils.compare(device, PatchLevel(2023, 8, 1)),
        )
        // Previous month with a later day is still older.
        assertEquals(
            PatchLevelComparison.GSI_OLDER,
            PatchLevelUtils.compare(PatchLevel(2023, 8, 1), PatchLevel(2023, 7, 31)),
        )
        assertEquals(
            PatchLevelComparison.GSI_SAME_OR_NEWER,
            PatchLevelUtils.compare(device, PatchLevel(2024, 1, 1)),
        )
    }

    @Test
    fun comparisonIsUnknownWhenOneSideIsMissing() {
        val device = PatchLevel(2023, 8, 5)
        assertEquals(PatchLevelComparison.UNKNOWN, PatchLevelUtils.compare(null, device))
        assertEquals(PatchLevelComparison.UNKNOWN, PatchLevelUtils.compare(device, null))
        assertEquals(PatchLevelComparison.UNKNOWN, PatchLevelUtils.compare(null, null))
    }

    @Test
    fun formatsIsoString() {
        assertEquals("2023-08-05", PatchLevel(2023, 8, 5).toIsoString())
    }
}
