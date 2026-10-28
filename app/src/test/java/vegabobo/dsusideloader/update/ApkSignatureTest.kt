package vegabobo.dsusideloader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkSignatureTest {

    private val debugKey = byteArrayOf(1, 2, 3, 4)
    private val releaseKey = byteArrayOf(9, 8, 7, 6)

    @Test
    fun `the same certificate is accepted`() {
        assertTrue(ApkSignature.sameSigner(listOf(debugKey), listOf(debugKey)))
    }

    @Test
    fun `a different certificate is rejected`() {
        assertFalse(ApkSignature.sameSigner(listOf(debugKey), listOf(releaseKey)))
    }

    @Test
    fun `unknown signatures do not block the installer`() {
        assertTrue(ApkSignature.sameSigner(null, listOf(debugKey)))
        assertTrue(ApkSignature.sameSigner(listOf(debugKey), null))
        assertTrue(ApkSignature.sameSigner(emptyList(), listOf(debugKey)))
    }

    @Test
    fun `the certificate order does not matter`() {
        assertTrue(
            ApkSignature.sameSigner(
                listOf(debugKey, releaseKey),
                listOf(releaseKey, debugKey),
            ),
        )
    }

    @Test
    fun `fingerprints are stable and distinguish keys`() {
        assertEquals(ApkSignature.fingerprint(debugKey), ApkSignature.fingerprint(debugKey))
        assertNotEquals(ApkSignature.fingerprint(debugKey), ApkSignature.fingerprint(releaseKey))
        assertEquals(64, ApkSignature.fingerprint(debugKey).length)
    }
}
