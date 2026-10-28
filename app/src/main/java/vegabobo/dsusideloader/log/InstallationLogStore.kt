package vegabobo.dsusideloader.log

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Installation log shared by the home screen (progress + bottom sheet) and the
 * Log tab.
 *
 * The store keeps the last [MAX_LINES] lines, so a long installation cannot
 * grow the state (and the recompositions) without bound.
 */
@Singleton
class InstallationLogStore @Inject constructor() {

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private var raw = ""

    /** Full text of the current log, used by the share/save actions. */
    fun text(): String = raw

    fun update(logs: String) {
        raw = logs
        _lines.value = logs
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
            .takeLast(MAX_LINES)
    }

    fun clear() {
        raw = ""
        _lines.value = emptyList()
    }

    fun isEmpty(): Boolean = _lines.value.isEmpty()

    private companion object {
        const val MAX_LINES = 500
    }
}
