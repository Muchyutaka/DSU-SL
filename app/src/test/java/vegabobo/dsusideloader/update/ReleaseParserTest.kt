package vegabobo.dsusideloader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {

    private val apiResponse = """
        {
          "url": "https://api.github.com/repos/Muchyutaka/DSU-SL/releases/1",
          "html_url": "https://github.com/Muchyutaka/DSU-SL/releases/tag/v2.05",
          "id": 1,
          "tag_name": "v2.05",
          "name": "v2.05",
          "draft": false,
          "prerelease": false,
          "created_at": "2026-10-28T10:00:00Z",
          "published_at": "2026-10-28T10:39:12Z",
          "body": "Automated build for v2.05.\n\n### How to install\n1. Android 10 or newer.",
          "assets": [
            {
              "name": "SHA256SUMS",
              "browser_download_url": "https://github.com/Muchyutaka/DSU-SL/releases/download/v2.05/SHA256SUMS",
              "size": 198
            },
            {
              "name": "dsu-sideloader-2.05-debug.apk",
              "browser_download_url": "https://github.com/Muchyutaka/DSU-SL/releases/download/v2.05/dsu-sideloader-2.05-debug.apk",
              "size": 58397638
            },
            {
              "name": "dsu-sideloader-2.05-release.apk",
              "browser_download_url": "https://github.com/Muchyutaka/DSU-SL/releases/download/v2.05/dsu-sideloader-2.05-release.apk",
              "size": 5272714
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses the release fields the app uses`() {
        val release = ReleaseParser.parseRelease(apiResponse)

        assertEquals("v2.05", release.tagName)
        assertEquals("https://github.com/Muchyutaka/DSU-SL/releases/tag/v2.05", release.htmlUrl)
        assertEquals(3, release.assets.size)
        assertFalse(release.draft)
        assertFalse(release.prerelease)
    }

    @Test
    fun `prefers the release apk over the debug apk`() {
        val release = ReleaseParser.parseRelease(apiResponse)
        val apk = ReleaseParser.preferredApk(release.assets)

        assertEquals("dsu-sideloader-2.05-release.apk", apk?.name)
    }

    @Test
    fun `falls back to any apk when there is no release apk`() {
        val assets = listOf(
            GitHubAsset("SHA256SUMS", "https://example.com/SHA256SUMS"),
            GitHubAsset("app-debug.apk", "https://example.com/app-debug.apk"),
        )

        assertEquals("app-debug.apk", ReleaseParser.preferredApk(assets)?.name)
        assertEquals(null, ReleaseParser.preferredApk(listOf(GitHubAsset("notes.txt", "u"))))
    }

    @Test
    fun `builds the app model with the checksums url`() {
        val info = ReleaseParser.toReleaseInfo(ReleaseParser.parseRelease(apiResponse))

        assertEquals("v2.05", info?.tag)
        assertEquals("dsu-sideloader-2.05-release.apk", info?.apkName)
        assertEquals(
            "https://github.com/Muchyutaka/DSU-SL/releases/download/v2.05/dsu-sideloader-2.05-release.apk",
            info?.apkUrl,
        )
        assertTrue(info?.checksumsUrl?.endsWith("SHA256SUMS") == true)
        assertEquals(ReleaseVersion(listOf(2, 5)), info?.version)
    }

    @Test
    fun `drafts prereleases and releases without apk are ignored`() {
        val draft = ReleaseParser.parseRelease(apiResponse).copy(draft = true)
        val prerelease = ReleaseParser.parseRelease(apiResponse).copy(prerelease = true)
        val noApk = ReleaseParser.parseRelease(apiResponse).copy(
            assets = listOf(GitHubAsset("SHA256SUMS", "https://example.com/SHA256SUMS")),
        )

        assertNull(ReleaseParser.toReleaseInfo(draft))
        assertNull(ReleaseParser.toReleaseInfo(prerelease))
        assertNull(ReleaseParser.toReleaseInfo(noApk))
    }

    @Test
    fun `checksums file is parsed like sha256sum output`() {
        val text = """
            9540268e2253c6e9cf9b9470b6e064b46255411e15b5e8dd247470842e9929b0  ./dsu-sideloader-2.05-debug.apk
            44A9F159A4B8CC79D9D7303A3DC26BBA28C33C3606AB99ECE6E80097D70289BC  dsu-sideloader-2.05-release.apk
            not-a-hash  ignored.apk
        """.trimIndent()

        val checksums = ReleaseParser.parseChecksums(text)

        assertEquals(2, checksums.size)
        assertEquals(
            "9540268e2253c6e9cf9b9470b6e064b46255411e15b5e8dd247470842e9929b0",
            ReleaseParser.checksumFor(checksums, "dsu-sideloader-2.05-debug.apk"),
        )
        // Upper case digests are normalized, the path prefix is dropped.
        assertEquals(
            "44a9f159a4b8cc79d9d7303a3dc26bba28c33c3606ab99ece6e80097d70289bc",
            ReleaseParser.checksumFor(checksums, "dsu-sideloader-2.05-release.apk"),
        )
        assertNull(ReleaseParser.checksumFor(checksums, "other.apk"))
    }
}
