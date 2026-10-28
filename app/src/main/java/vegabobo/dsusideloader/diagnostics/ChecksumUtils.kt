package vegabobo.dsusideloader.diagnostics

/**
 * Pure helpers to handle SHA-256 checksums.
 *
 * Kept free of Android APIs so it can be covered by JVM unit tests.
 */
object ChecksumUtils {

    const val SHA256_HEX_LENGTH = 64

    private const val SHA256_PREFIX = "sha256:"
    private val SHA256_REGEX = Regex("^[0-9a-f]{$SHA256_HEX_LENGTH}$")
    private val WHITESPACE_REGEX = Regex("\\s+")

    /**
     * Extracts a canonical (lowercase, no spaces) SHA-256 hash from user input.
     *
     * Accepts the hash alone, the hash with a "sha256:" prefix and the output of
     * tools like `sha256sum` (which appends "  filename" or " *filename").
     *
     * @return the 64 characters hash, or an empty string when the input does not
     * contain a valid SHA-256 hash.
     */
    fun canonical(raw: String?): String {
        if (raw.isNullOrBlank()) {
            return ""
        }
        val cleaned = raw.trim()
            .lowercase()
            .removePrefix(SHA256_PREFIX)
            .trim()
        return cleaned.split(WHITESPACE_REGEX)
            .map { it.removePrefix("*") }
            .firstOrNull { SHA256_REGEX.matches(it) }
            ?: ""
    }

    fun isValid(raw: String?): Boolean = canonical(raw).isNotEmpty()

    fun matches(expected: String?, actual: String?): Boolean {
        val expectedHash = canonical(expected)
        val actualHash = canonical(actual)
        return expectedHash.isNotEmpty() && expectedHash == actualHash
    }

    /**
     * Abbreviates a hash for compact UI usage, returning the input unchanged when
     * it is not a valid SHA-256 hash.
     */
    fun abbreviate(raw: String?, prefixLength: Int = 12, suffixLength: Int = 8): String {
        val hash = canonical(raw)
        if (hash.isEmpty()) {
            return raw.orEmpty()
        }
        if (hash.length <= prefixLength + suffixLength) {
            return hash
        }
        return hash.take(prefixLength) + "…" + hash.takeLast(suffixLength)
    }
}
