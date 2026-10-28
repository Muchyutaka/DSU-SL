package vegabobo.dsusideloader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {

    @Test
    fun `parses the tag shapes this repository uses`() {
        assertEquals(ReleaseVersion(listOf(2, 5)), ReleaseVersion.parse("v2.05"))
        assertEquals(ReleaseVersion(listOf(2, 5)), ReleaseVersion.parse("2.05"))
        assertEquals(ReleaseVersion(listOf(2, 5, 1)), ReleaseVersion.parse("v2.5.1"))
        assertEquals(ReleaseVersion(listOf(2, 5)), ReleaseVersion.parse("release-v2.05"))
        assertEquals(ReleaseVersion(listOf(2, 5)), ReleaseVersion.parse("2.05-beta"))
        assertEquals(ReleaseVersion(listOf(2, 5)), ReleaseVersion.parse(" V2.05 "))
    }

    @Test
    fun `rejects values without a version`() {
        assertNull(ReleaseVersion.parse(null))
        assertNull(ReleaseVersion.parse(""))
        assertNull(ReleaseVersion.parse("latest"))
        assertNull(ReleaseVersion.parse("v"))
    }

    @Test
    fun `comparison is numeric and not lexicographic`() {
        // "2.10" < "2.9" as text, but 2.10 is the newer version.
        assertTrue(ReleaseVersion.parse("2.10")!! > ReleaseVersion.parse("2.9")!!)
        assertTrue(ReleaseVersion.parse("2.05")!! > ReleaseVersion.parse("2.4")!!)
        assertTrue(ReleaseVersion.parse("3.0")!! > ReleaseVersion.parse("2.99")!!)
        assertTrue(ReleaseVersion.parse("2.05.1")!! > ReleaseVersion.parse("2.05")!!)
    }

    @Test
    fun `missing parts count as zero`() {
        // Equal when compared, even though the data classes differ ("2.5" has
        // two parts, "2.5.0" three).
        assertEquals(0, ReleaseVersion.parse("2.5")!!.compareTo(ReleaseVersion.parse("2.5.0")!!))
        assertFalse(ReleaseVersion.parse("2.5.0")!! > ReleaseVersion.parse("2.5")!!)
        assertFalse(ReleaseVersion.parse("2.5")!! > ReleaseVersion.parse("2.5.0")!!)
    }

    @Test
    fun `isNewerThan guards against an unknown installed version`() {
        val installed = ReleaseVersion.parse("2.05")
        assertTrue(ReleaseVersion.parse("2.06")!!.isNewerThan(installed))
        assertFalse(ReleaseVersion.parse("2.05")!!.isNewerThan(installed))
        assertFalse(ReleaseVersion.parse("2.04")!!.isNewerThan(installed))
        assertFalse(ReleaseVersion.parse("2.06")!!.isNewerThan(null))
        assertTrue(ReleaseVersion.parse("2.05")!!.isNewerThan(ReleaseVersion(listOf(0))))
    }

    @Test
    fun `a version string from an API tag compares with the build version name`() {
        // BuildConfig.VERSION_NAME is "2.05" in this release.
        val installed = ReleaseVersion.parse("2.05")
        val releaseTag = ReleaseVersion.parse("v2.05")

        assertEquals(0, installed!!.compareTo(releaseTag!!))
        assertFalse(releaseTag.isNewerThan(installed))
    }
}
