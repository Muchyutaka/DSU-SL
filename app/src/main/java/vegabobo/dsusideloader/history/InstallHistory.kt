package vegabobo.dsusideloader.history

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class InstallResult {
    SUCCESS,
    ERROR,
    CANCELED,
}

/**
 * One installation attempt.
 *
 * Only metadata is stored: no image file, no uri and no user data of the
 * installed system.
 */
@Serializable
data class InstallHistoryEntry(
    val timestamp: Long = 0L,
    val fileName: String = "",
    val userdataGb: Long = 0L,
    val imageSizeBytes: Long = -1L,
    val result: InstallResult = InstallResult.ERROR,
    val errorSummary: String = "",
)

/**
 * A reusable installation configuration.
 */
@Serializable
data class InstallPreset(
    val id: String = "",
    val createdAt: Long = 0L,
    val userdataGb: Long = 0L,
    val imageSizeBytes: Long = -1L,
)

@Serializable
private data class HistoryPayload(val entries: List<InstallHistoryEntry> = emptyList())

@Serializable
private data class PresetsPayload(val presets: List<InstallPreset> = emptyList())

/**
 * Bounded list manipulation, pure so it can be unit tested.
 */
object InstallHistoryOps {

    const val MAX_HISTORY_ENTRIES = 25
    const val MAX_PRESETS = 10
    const val MAX_ERROR_SUMMARY_LENGTH = 200

    fun addEntry(
        current: List<InstallHistoryEntry>,
        entry: InstallHistoryEntry,
        maxEntries: Int = MAX_HISTORY_ENTRIES,
    ): List<InstallHistoryEntry> {
        val sanitized = entry.copy(errorSummary = summarize(entry.errorSummary))
        return (listOf(sanitized) + current).take(maxEntries)
    }

    fun addPreset(
        current: List<InstallPreset>,
        preset: InstallPreset,
        maxPresets: Int = MAX_PRESETS,
    ): List<InstallPreset> {
        val withoutSameConfiguration = current.filterNot {
            it.userdataGb == preset.userdataGb && it.imageSizeBytes == preset.imageSizeBytes
        }
        return (listOf(preset) + withoutSameConfiguration).take(maxPresets)
    }

    fun removePreset(current: List<InstallPreset>, id: String): List<InstallPreset> =
        current.filterNot { it.id == id }

    /**
     * Keeps the first non blank line of an error, bounded to
     * [MAX_ERROR_SUMMARY_LENGTH] characters.
     */
    fun summarize(rawError: String?, maxLength: Int = MAX_ERROR_SUMMARY_LENGTH): String {
        if (rawError.isNullOrBlank()) {
            return ""
        }
        val firstLine = rawError.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return ""
        return if (firstLine.length <= maxLength) {
            firstLine
        } else {
            firstLine.take(maxLength) + "…"
        }
    }

    fun presetFromEntry(entry: InstallHistoryEntry, id: String, createdAt: Long): InstallPreset =
        InstallPreset(
            id = id,
            createdAt = createdAt,
            userdataGb = entry.userdataGb,
            imageSizeBytes = entry.imageSizeBytes,
        )
}

/**
 * JSON codec used by [InstallHistoryStore].
 *
 * Unknown keys are ignored so a future version can add fields without breaking
 * older installs, and corrupted data decodes to an empty list instead of
 * crashing the app.
 */
object InstallHistoryCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encodeHistory(entries: List<InstallHistoryEntry>): String =
        json.encodeToString(HistoryPayload.serializer(), HistoryPayload(entries))

    fun decodeHistory(raw: String?): List<InstallHistoryEntry> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            json.decodeFromString(HistoryPayload.serializer(), raw).entries
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun encodePresets(presets: List<InstallPreset>): String =
        json.encodeToString(PresetsPayload.serializer(), PresetsPayload(presets))

    fun decodePresets(raw: String?): List<InstallPreset> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            json.decodeFromString(PresetsPayload.serializer(), raw).presets
        } catch (e: Exception) {
            emptyList()
        }
    }
}
