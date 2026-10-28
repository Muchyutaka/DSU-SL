package vegabobo.dsusideloader.update

/**
 * Version of a release tag, compared numerically part by part.
 *
 * Tags in this repository look like `v2.05`, `v2.5.1` or `2.05-beta`, so the
 * comparison cannot rely on string ordering ("2.10" < "2.9" as text).
 */
data class ReleaseVersion(val parts: List<Int>) : Comparable<ReleaseVersion> {

    override fun compareTo(other: ReleaseVersion): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (index in 0 until size) {
            val left = parts.getOrElse(index) { 0 }
            val right = other.parts.getOrElse(index) { 0 }
            if (left != right) {
                return left.compareTo(right)
            }
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    companion object {
        /**
         * Parses `v2.05`, `2.05`, `release-2.5.1`, `2.05-beta` into [ReleaseVersion].
         * Everything after the first non numeric separator is ignored, which is
         * what pre-release suffixes and build metadata need.
         */
        fun parse(raw: String?): ReleaseVersion? {
            val text = raw?.trim()?.lowercase() ?: return null
            if (text.isEmpty()) return null

            val digits = StringBuilder()
            var started = false
            for (character in text) {
                when {
                    character.isDigit() -> {
                        digits.append(character)
                        started = true
                    }

                    character == '.' && started -> digits.append('.')
                    // Anything else (v, -, +, letters) ends the version.
                    started -> return buildOrNull(digits.toString())
                }
            }
            return buildOrNull(digits.toString())
        }

        private fun buildOrNull(text: String): ReleaseVersion? {
            val trimmed = text.trim('.')
            if (trimmed.isEmpty()) return null
            val parts = trimmed.split('.').map { it.toIntOrNull() ?: return null }
            if (parts.isEmpty()) return null
            return ReleaseVersion(parts)
        }
    }
}

/** True when this app version is newer than [other]. */
fun ReleaseVersion.isNewerThan(other: ReleaseVersion?): Boolean =
    other != null && this > other
