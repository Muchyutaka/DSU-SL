package vegabobo.dsusideloader.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecksumUtilsTest {

    private val hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    @Test
    fun canonicalAcceptsPlainHash() {
        assertEquals(hash, ChecksumUtils.canonical(hash))
    }

    @Test
    fun canonicalLowercasesAndTrims() {
        assertEquals(hash, ChecksumUtils.canonical("  ${hash.uppercase()}  "))
    }

    @Test
    fun canonicalAcceptsSha256Prefix() {
        assertEquals(hash, ChecksumUtils.canonical("sha256:$hash"))
    }

    @Test
    fun canonicalAcceptsSha256sumOutput() {
        assertEquals(hash, ChecksumUtils.canonical("$hash  system.img"))
        assertEquals(hash, ChecksumUtils.canonical("$hash *system.img"))
    }

    @Test
    fun canonicalRejectsInvalidInput() {
        assertEquals("", ChecksumUtils.canonical(null))
        assertEquals("", ChecksumUtils.canonical(""))
        assertEquals("", ChecksumUtils.canonical("   "))
        assertEquals("", ChecksumUtils.canonical("not-a-hash"))
        assertEquals("", ChecksumUtils.canonical(hash.dropLast(1)))
        assertEquals("", ChecksumUtils.canonical(hash + "0"))
        assertEquals("", ChecksumUtils.canonical(hash.dropLast(1) + "z"))
    }

    @Test
    fun isValidMatchesCanonical() {
        assertTrue(ChecksumUtils.isValid(hash))
        assertFalse(ChecksumUtils.isValid("deadbeef"))
    }

    @Test
    fun matchesIsCaseInsensitiveAndIgnoresFileSuffix() {
        assertTrue(ChecksumUtils.matches(hash.uppercase(), hash))
        assertTrue(ChecksumUtils.matches("$hash  system.img", hash))
        assertFalse(ChecksumUtils.matches(hash.dropLast(1) + "0", hash))
    }

    @Test
    fun matchesIsFalseWhenExpectedIsMissing() {
        assertFalse(ChecksumUtils.matches("", hash))
        assertFalse(ChecksumUtils.matches(null, hash))
        assertFalse(ChecksumUtils.matches(hash, null))
    }

    @Test
    fun abbreviateKeepsShortInputAndShortensValidHash() {
        val abbreviated = ChecksumUtils.abbreviate(hash)
        assertEquals("e3b0c44298fc…7852b855", abbreviated)
        assertEquals("nope", ChecksumUtils.abbreviate("nope"))
        assertEquals("", ChecksumUtils.abbreviate(null))
    }
}
