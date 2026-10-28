package vegabobo.dsusideloader.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Subset of the GitHub Releases API (`/repos/{owner}/{repo}/releases/latest`)
 * this app needs. Unknown fields are ignored so GitHub can add new ones.
 */
@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

/** Information about a release, independent of the JSON shape it came from. */
data class ReleaseInfo(
    val tag: String,
    val title: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkName: String,
    val checksumsUrl: String,
    val version: ReleaseVersion,
)

object ReleaseParser {

    private const val CHECKSUMS_ASSET = "SHA256SUMS"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Parses the body of the GitHub releases API. Throws on malformed input. */
    fun parseRelease(body: String): GitHubRelease =
        json.decodeFromString(GitHubRelease.serializer(), body)

    /**
     * Converts an API response into the app model.
     *
     * The APK to download is the tagged release build when present, otherwise
     * any other APK asset. Drafts and pre-releases are rejected: offering an
     * unfinished build to every user would be worse than not updating at all.
     */
    fun toReleaseInfo(release: GitHubRelease): ReleaseInfo? {
        if (release.draft || release.prerelease) return null

        val version = ReleaseVersion.parse(release.tagName) ?: return null
        val apk = preferredApk(release.assets) ?: return null
        val checksums = release.assets.firstOrNull { it.name == CHECKSUMS_ASSET }

        return ReleaseInfo(
            tag = release.tagName,
            title = release.name.ifEmpty { release.tagName },
            notes = release.body,
            pageUrl = release.htmlUrl,
            apkUrl = apk.downloadUrl,
            apkName = apk.name,
            checksumsUrl = checksums?.downloadUrl ?: "",
            version = version,
        )
    }

    /** Prefers the `-release.apk` asset, then any other APK. */
    fun preferredApk(assets: List<GitHubAsset>): GitHubAsset? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        return apks.firstOrNull { it.name.contains("-release", ignoreCase = true) }
            ?: apks.firstOrNull()
    }

    /**
     * Parses a `sha256sum` style file (`<hash>  <file>` per line) into a map of
     * file name to lower case hex digest.
     */
    fun parseChecksums(text: String): Map<String, String> {
        val result = HashMap<String, String>()
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            val parts = trimmed.split(Regex("\\s+"), limit = 2)
            if (parts.size != 2) return@forEach
            val hash = parts[0].lowercase()
            if (hash.length != 64) return@forEach
            val name = parts[1].trim().removePrefix("*").substringAfterLast('/')
            result[name] = hash
        }
        return result
    }

    /** Checksum of [apkName] inside a `SHA256SUMS` file, when listed. */
    fun checksumFor(checksums: Map<String, String>, apkName: String): String? =
        checksums[apkName.substringAfterLast('/')]
}
