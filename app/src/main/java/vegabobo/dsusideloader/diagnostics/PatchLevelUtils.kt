package vegabobo.dsusideloader.diagnostics

/**
 * A calendar date used to represent a security patch level (for instance
 * "2023-08-05").
 */
data class PatchLevel(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<PatchLevel> {

    override fun compareTo(other: PatchLevel): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toIsoString(): String = "%04d-%02d-%02d".format(year, month, day)
}

enum class PatchLevelComparison {
    /** One of the patch levels is missing or could not be parsed reliably. */
    UNKNOWN,

    /** The image looks older than the device security patch level. */
    GSI_OLDER,

    /** The image looks the same age or newer than the device. */
    GSI_SAME_OR_NEWER,
}

/**
 * Pure helpers to read and compare security patch levels.
 *
 * The device value comes from [android.os.Build.VERSION.SECURITY_PATCH] (e.g.
 * "2023-08-05"). The image value is *estimated* from its file name, because a
 * GSI does not expose its patch level without mounting/parsing the image. When
 * the name carries no date, the level is reported as unknown instead of being
 * guessed.
 */
object PatchLevelUtils {

    const val MIN_YEAR = 2015
    const val MAX_YEAR = 2100

    private val DASHED_DATE_REGEX = Regex("(?<!\\d)(\\d{4})[-/.](\\d{2})[-/.](\\d{2})(?!\\d)")
    private val COMPACT_DATE_REGEX = Regex("(?<!\\d)(\\d{8})(?!\\d)")

    /**
     * Parses a security patch level string as reported by the platform or by
     * the user. Accepts "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd" and "yyyyMMdd".
     */
    fun parse(raw: String?): PatchLevel? {
        if (raw.isNullOrBlank()) {
            return null
        }
        val value = raw.trim()
        DASHED_DATE_REGEX.find(value)?.let { match ->
            val (year, month, day) = match.destructured
            return createOrNull(year.toIntOrNull(), month.toIntOrNull(), day.toIntOrNull())
        }
        COMPACT_DATE_REGEX.find(value)?.let { match ->
            val digits = match.groupValues[1]
            return createOrNull(
                digits.substring(0, 4).toIntOrNull(),
                digits.substring(4, 6).toIntOrNull(),
                digits.substring(6, 8).toIntOrNull(),
            )
        }
        return null
    }

    /**
     * Estimates the security patch level of an image from its file name, for
     * instance "lineage-20.0-20231012-UNOFFICIAL-arm64_ab.img" or
     * "system-2023-10-12.img".
     *
     * @return the parsed date, or null when the file name carries no reliable
     * date.
     */
    fun parseFromFileName(fileName: String?): PatchLevel? {
        if (fileName.isNullOrBlank()) {
            return null
        }
        DASHED_DATE_REGEX.find(fileName)?.let { match ->
            val (year, month, day) = match.destructured
            createOrNull(year.toIntOrNull(), month.toIntOrNull(), day.toIntOrNull())
                ?.let { return it }
        }
        for (match in COMPACT_DATE_REGEX.findAll(fileName)) {
            val digits = match.groupValues[1]
            val parsed = createOrNull(
                digits.substring(0, 4).toIntOrNull(),
                digits.substring(4, 6).toIntOrNull(),
                digits.substring(6, 8).toIntOrNull(),
            )
            if (parsed != null) {
                return parsed
            }
        }
        return null
    }

    /**
     * Compares an estimated image patch level with the device one.
     *
     * The comparison is done per month: security patch levels are monthly, and
     * the day differs between vendors (the 1st, the 5th, ...), so a day level
     * comparison would report a false "older" warning for the same month.
     */
    fun compare(device: PatchLevel?, image: PatchLevel?): PatchLevelComparison {
        if (device == null || image == null) {
            return PatchLevelComparison.UNKNOWN
        }
        val deviceMonth = device.year * 100 + device.month
        val imageMonth = image.year * 100 + image.month
        return if (imageMonth < deviceMonth) {
            PatchLevelComparison.GSI_OLDER
        } else {
            PatchLevelComparison.GSI_SAME_OR_NEWER
        }
    }

    private fun createOrNull(year: Int?, month: Int?, day: Int?): PatchLevel? {
        if (year == null || month == null || day == null) {
            return null
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            return null
        }
        if (month !in 1..12 || day !in 1..31) {
            return null
        }
        return PatchLevel(year, month, day)
    }
}
