package vegabobo.dsusideloader.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vegabobo.dsusideloader.util.OperationMode

class AccessMethodTest {

    @Test
    fun `preference values round trip`() {
        AccessMethod.values().forEach { method ->
            assertEquals(method, AccessMethod.fromPreferenceValue(method.toPreferenceValue()))
        }
    }

    @Test
    fun `preference values are case and space insensitive`() {
        assertEquals(AccessMethod.SHIZUKU, AccessMethod.fromPreferenceValue("  Shizuku "))
        assertEquals(AccessMethod.ROOT, AccessMethod.fromPreferenceValue("ROOT"))
        assertEquals(AccessMethod.ADB, AccessMethod.fromPreferenceValue("adb"))
    }

    @Test
    fun `unknown or missing preference values are rejected`() {
        assertNull(AccessMethod.fromPreferenceValue(null))
        assertNull(AccessMethod.fromPreferenceValue(""))
        assertNull(AccessMethod.fromPreferenceValue("magisk"))
    }

    @Test
    fun `methods map to their operation mode`() {
        assertEquals(OperationMode.ADB, AccessMethod.ADB.toOperationMode())
        assertEquals(OperationMode.SHIZUKU, AccessMethod.SHIZUKU.toOperationMode())
        assertEquals(OperationMode.ROOT, AccessMethod.ROOT.toOperationMode())
    }

    @Test
    fun `operation modes map back to the method that produces them`() {
        assertEquals(AccessMethod.ADB, AccessMethod.fromOperationMode(OperationMode.ADB))
        assertEquals(AccessMethod.SHIZUKU, AccessMethod.fromOperationMode(OperationMode.SHIZUKU))
        assertEquals(AccessMethod.ROOT, AccessMethod.fromOperationMode(OperationMode.ROOT))
        // System app installs behave like root for the purpose of the UI.
        assertEquals(AccessMethod.ROOT, AccessMethod.fromOperationMode(OperationMode.SYSTEM))
        assertEquals(
            AccessMethod.ROOT,
            AccessMethod.fromOperationMode(OperationMode.SYSTEM_AND_ROOT),
        )
    }

    @Test
    fun `only a verified method can continue`() {
        assertTrue(AccessStatus.AVAILABLE.canContinue())
        assertFalse(AccessStatus.NOT_AUTHORIZED.canContinue())
        assertFalse(AccessStatus.NOT_INSTALLED.canContinue())
        assertFalse(AccessStatus.NOT_RUNNING.canContinue())
        assertFalse(AccessStatus.UNAVAILABLE.canContinue())
        assertFalse(AccessStatus.CHECKING.canContinue())
        assertFalse(AccessStatus.UNKNOWN.canContinue())
    }

    @Test
    fun `only fixable states are actionable and only availability is positive`() {
        assertTrue(AccessStatus.NOT_AUTHORIZED.isActionable())
        assertTrue(AccessStatus.NOT_INSTALLED.isActionable())
        assertTrue(AccessStatus.NOT_RUNNING.isActionable())
        assertFalse(AccessStatus.AVAILABLE.isActionable())
        assertFalse(AccessStatus.UNAVAILABLE.isActionable())
        assertFalse(AccessStatus.UNKNOWN.isActionable())

        assertTrue(AccessStatus.AVAILABLE.isPositive())
        assertFalse(AccessStatus.NOT_AUTHORIZED.isPositive())
        assertTrue(AccessStatus.UNAVAILABLE.isNegative())
        assertFalse(AccessStatus.AVAILABLE.isNegative())
    }

    @Test
    fun `reasons report success only for verified states`() {
        assertTrue(AccessReason.ROOT_GRANTED.isSuccess())
        assertTrue(AccessReason.SHIZUKU_AVAILABLE.isSuccess())
        assertTrue(AccessReason.ADB_READY.isSuccess())
        assertTrue(AccessReason.SYSTEM_PRIVILEGES.isSuccess())

        assertFalse(AccessReason.ROOT_DENIED.isSuccess())
        assertFalse(AccessReason.SHIZUKU_NOT_INSTALLED.isSuccess())
        assertFalse(AccessReason.ADB_DEBUGGING_DISABLED.isSuccess())
        assertFalse(AccessReason.NOT_CHECKED.isSuccess())
    }

    @Test
    fun `check result exposes the gating of its status`() {
        assertTrue(AccessCheckResult.available(AccessReason.ROOT_GRANTED).canContinue())
        assertFalse(AccessCheckResult.checking().canContinue())
        assertFalse(AccessCheckResult.unknown().canContinue())
        assertFalse(
            AccessCheckResult.failed(
                AccessStatus.NOT_AUTHORIZED,
                AccessReason.SHIZUKU_NOT_AUTHORIZED,
            ).canContinue(),
        )
    }
}
