package vegabobo.dsusideloader.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin HTTP client for the GitHub Releases API and the release assets.
 *
 * No third party library is used: `HttpURLConnection` is enough for two GET
 * requests and keeps the app free of extra dependencies.
 */
class ReleaseClient(
    private val latestReleaseUrl: String = GITHUB_LATEST_RELEASE,
    private val userAgent: String,
) {

    /** GET a text resource, throwing [ReleaseClientException] on any problem. */
    fun getText(url: String): String {
        val connection = open(url)
        return try {
            val code = connection.responseCode
            if (code == HTTP_RATE_LIMITED || code == HTTP_FORBIDDEN) {
                val remaining = connection.getHeaderField("X-RateLimit-Remaining")
                if (code == HTTP_RATE_LIMITED || remaining == "0") {
                    throw ReleaseClientException(UpdateError.RATE_LIMITED)
                }
                throw ReleaseClientException(UpdateError.NETWORK)
            }
            if (code != HTTP_OK) {
                throw ReleaseClientException(UpdateError.NETWORK, "HTTP $code")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: ReleaseClientException) {
            throw e
        } catch (e: IOException) {
            throw ReleaseClientException(UpdateError.NETWORK, e.message)
        } finally {
            connection.disconnect()
        }
    }

    /** Latest published release, or null when GitHub reports there is none. */
    fun latestRelease(): GitHubRelease? {
        val body = try {
            getText(latestReleaseUrl)
        } catch (e: ReleaseClientException) {
            if (e.error == UpdateError.NETWORK && e.detail == "HTTP 404") {
                return null
            }
            throw e
        }
        return try {
            ReleaseParser.parseRelease(body)
        } catch (e: Exception) {
            throw ReleaseClientException(UpdateError.PARSE_ERROR, e.message)
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw ReleaseClientException(UpdateError.NETWORK, e.message)
        }
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", userAgent)
        return connection
    }

    companion object {
        const val OWNER = "Muchyutaka"
        const val REPOSITORY = "DSU-SL"
        const val GITHUB_LATEST_RELEASE =
            "https://api.github.com/repos/$OWNER/$REPOSITORY/releases/latest"
        const val GITHUB_RELEASES_PAGE = "https://github.com/$OWNER/$REPOSITORY/releases"

        private const val HTTP_OK = 200
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_RATE_LIMITED = 429
        private const val TIMEOUT_MS = 15_000
    }
}

/** Failure of a release request, mapped to a user facing string by the UI. */
class ReleaseClientException(
    val error: UpdateError,
    val detail: String? = null,
) : Exception(detail ?: error.name)

/** Everything that can go wrong while checking, downloading or installing. */
enum class UpdateError {
    /** No connection, DNS failure, timeout, non success HTTP status. */
    NETWORK,

    /** GitHub API rate limit reached (60 requests per hour per IP). */
    RATE_LIMITED,

    /** The response could not be parsed. */
    PARSE_ERROR,

    /** The release has no APK asset. */
    NO_APK,

    /** The APK download failed or was interrupted. */
    DOWNLOAD_FAILED,

    /** The published SHA-256 does not match the downloaded file. */
    CHECKSUM_MISMATCH,

    /** Android does not allow this app to install packages. */
    INSTALL_NOT_ALLOWED,

    /**
     * The downloaded APK is signed with a different key than the installed
     * app. The system installer rejects that with
     * `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ("package conflicts with an existing
     * package"), so it is detected before the installer is opened.
     */
    SIGNATURE_MISMATCH,
}
