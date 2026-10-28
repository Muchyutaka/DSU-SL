package vegabobo.dsusideloader.ui.screen.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vegabobo.dsusideloader.preparation.InstallationStep

class HomeStageTest {

    private fun state(
        step: InstallationStep = InstallationStep.NOT_INSTALLING,
        text: String = "",
        isInstallable: Boolean = false,
    ) = InstallationCardState(
        installationStep = step,
        text = text,
        isInstallable = isInstallable,
    )

    @Test
    fun `empty when nothing is selected`() {
        assertEquals(HomeStage.EMPTY, state().stage())
    }

    @Test
    fun `selected once a file name or an installable image is present`() {
        assertEquals(HomeStage.SELECTED, state(text = "system.img").stage())
        assertEquals(HomeStage.SELECTED, state(isInstallable = true).stage())
    }

    @Test
    fun `installing for the intermediate preparation and installation steps`() {
        listOf(
            InstallationStep.PROCESSING,
            InstallationStep.COPYING_FILE,
            InstallationStep.DECOMPRESSING_XZ,
            InstallationStep.COMPRESSING_TO_GZ,
            InstallationStep.DECOMPRESSING_GZIP,
            InstallationStep.EXTRACTING_FILE,
            InstallationStep.WAITING_USER_CONFIRMATION,
            InstallationStep.PROCESSING_LOG_READABLE,
            InstallationStep.INSTALLING,
            InstallationStep.INSTALLING_ROOTED,
            InstallationStep.CREATING_PARTITION,
            InstallationStep.DISCARD_CURRENT_GSI,
        ).forEach { step ->
            assertEquals("step $step", HomeStage.INSTALLING, state(step).stage())
        }
    }

    @Test
    fun `result for success errors and states that need the user`() {
        listOf(
            InstallationStep.INSTALL_SUCCESS,
            InstallationStep.INSTALL_SUCCESS_REBOOT_DYN_OS,
            InstallationStep.DSU_ALREADY_INSTALLED,
            InstallationStep.DSU_ALREADY_RUNNING_DYN_OS,
            InstallationStep.REQUIRES_ADB_CMD_TO_CONTINUE,
            InstallationStep.ERROR,
            InstallationStep.ERROR_SELINUX,
            InstallationStep.ERROR_NO_AVAIL_STORAGE,
        ).forEach { step ->
            assertEquals("step $step", HomeStage.RESULT, state(step).stage())
        }
    }

    @Test
    fun `every step is classified exactly once`() {
        val resultSteps = InstallationStep.values().filter { it.isResultStep() }

        assertTrue(resultSteps.isNotEmpty())
        assertFalse(InstallationStep.NOT_INSTALLING.isResultStep())
        assertFalse(InstallationStep.INSTALLING.isResultStep())
        assertFalse(InstallationStep.PROCESSING.isResultStep())

        // Errors must always be terminal, otherwise the hero would show a
        // spinner forever after a failure.
        InstallationStep.values()
            .filter { it.name.startsWith("ERROR") }
            .forEach { assertTrue("$it", it.isResultStep()) }
    }
}
