package vegabobo.dsusideloader.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallationLogStoreTest {

    @Test
    fun `blank lines are dropped`() {
        val store = InstallationLogStore()
        store.update("first\n\n   \nsecond\n")

        assertEquals(listOf("first", "second"), store.lines.value)
    }

    @Test
    fun `text returns the raw log`() {
        val store = InstallationLogStore()
        store.update("a\nb")

        assertEquals("a\nb", store.text())
    }

    @Test
    fun `only the last 500 lines are kept`() {
        val store = InstallationLogStore()
        val lines = (1..600).map { "line $it" }
        store.update(lines.joinToString("\n"))

        assertEquals(500, store.lines.value.size)
        assertEquals("line 101", store.lines.value.first())
        assertEquals("line 600", store.lines.value.last())
    }

    @Test
    fun `clear empties lines and text`() {
        val store = InstallationLogStore()
        store.update("a\nb")
        assertFalse(store.isEmpty())

        store.clear()

        assertTrue(store.isEmpty())
        assertEquals(emptyList<String>(), store.lines.value)
        assertEquals("", store.text())
    }

    @Test
    fun `a new update replaces the previous lines`() {
        val store = InstallationLogStore()
        store.update("old")
        store.update("new")

        assertEquals(listOf("new"), store.lines.value)
    }
}
