package vegabobo.dsusideloader.ui.screen.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Strict checksum mode may only block an installation while the user actually
 * enabled verification.
 */
class ChecksumGatingTest {

    private fun blockedState(
        enabled: Boolean,
        strict: Boolean = true,
        expected: String = "abc",
        mismatch: Boolean = true,
    ) = InstallationCardState(
        checksumVerificationEnabled = enabled,
        strictChecksum = strict,
        expectedChecksum = expected,
        checksumMismatch = mismatch,
        checksum = ChecksumState(status = ChecksumStatus.READY, value = "def"),
    )

    @Test
    fun `verification is opt-in by default`() {
        assertFalse(InstallationCardState().checksumVerificationEnabled)
    }

    @Test
    fun `nothing blocks the install when verification is off`() {
        assertFalse(blockedState(enabled = false).isInstallBlocked())
    }

    @Test
    fun `strict mode blocks a mismatch when verification is on`() {
        assertTrue(blockedState(enabled = true).isInstallBlocked())
    }

    @Test
    fun `strict mode allows a matching checksum`() {
        assertFalse(blockedState(enabled = true, mismatch = false).isInstallBlocked())
    }

    @Test
    fun `strict mode without an expected checksum never blocks`() {
        assertFalse(blockedState(enabled = true, expected = "").isInstallBlocked())
    }
}
