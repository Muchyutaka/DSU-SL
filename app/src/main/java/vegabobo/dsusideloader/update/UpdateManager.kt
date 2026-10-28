package vegabobo.dsusideloader.update

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vegabobo.dsusideloader.BuildConfig
import vegabobo.dsusideloader.preferences.AppPrefs
import vegabobo.dsusideloader.util.DataStoreUtils

/**
 * Detects and fetches app updates from this repository's GitHub releases.
 *
 * - the detection is a single request to `releases/latest`;
 * - the download goes to app private storage, is verified against the release
 *   `SHA256SUMS` asset when present and then handed to the system installer;
 * - nothing is downloaded without the user tapping "Download", and no data is
 *   sent anywhere (GitHub only sees an anonymous API request).
 *
 * The state is a singleton so the home banner, Settings and About all show the
 * same thing.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Application,
    private val dataStore: DataStore<Preferences>,
) {

    private val tag = this.javaClass.simpleName
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = ReleaseClient(
        userAgent = "DSU-Sideloader/${BuildConfig.VERSION_NAME}",
    )

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Tag of the release the user dismissed, so the banner does not reappear. */
    private val _dismissedTag = MutableStateFlow("")
    val dismissedTag: StateFlow<String> = _dismissedTag.asStateFlow()

    private var downloadJob: Job? = null

    val currentVersion: String = BuildConfig.VERSION_NAME

    init {
        scope.launch {
            _dismissedTag.value = DataStoreUtils.readStringPref(
                dataStore,
                AppPrefs.UPDATE_DISMISSED_TAG,
                "",
            )
        }
    }

    /**
     * Automatic check, at most once every [CHECK_INTERVAL_MS].
     *
     * @param force ignores the throttle, used by the manual "check" buttons.
     */
    fun check(force: Boolean = false) {
        if (_state.value.isBusy()) {
            return
        }
        scope.launch {
            val lastCheck = DataStoreUtils.readStringPref(
                dataStore,
                AppPrefs.UPDATE_LAST_CHECK,
                "",
            ).toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            if (!force && now - lastCheck < CHECK_INTERVAL_MS) {
                Log.d(tag, "Update check skipped, last one was ${(now - lastCheck) / 1000}s ago")
                return@launch
            }

            _state.value = UpdateState.Checking
            val result = runCatching { client.latestRelease() }
            DataStoreUtils.updateStringPref(
                dataStore,
                AppPrefs.UPDATE_LAST_CHECK,
                now.toString(),
            )

            result.onSuccess { release ->
                val info = release?.let { ReleaseParser.toReleaseInfo(it) }
                applyRelease(info)
            }.onFailure { error ->
                Log.w(tag, "Update check failed", error)
                _state.value = UpdateState.Failed(
                    (error as? ReleaseClientException)?.error ?: UpdateError.NETWORK,
                )
            }
        }
    }

    private fun applyRelease(info: ReleaseInfo?) {
        if (info == null) {
            // No usable release published (or only drafts/pre-releases).
            _state.value = UpdateState.UpToDate(currentVersion)
            return
        }
        val installed = ReleaseVersion.parse(currentVersion)
        _state.value = if (info.version > (installed ?: ReleaseVersion(listOf(0)))) {
            UpdateState.Available(info)
        } else {
            UpdateState.UpToDate(currentVersion)
        }
        Log.d(tag, "Latest release ${info.tag}, installed $currentVersion -> ${_state.value::class.simpleName}")
    }

    /** Downloads the release APK, verifies it and moves to [UpdateState.ReadyToInstall]. */
    fun download(release: ReleaseInfo) {
        if (downloadJob?.isActive == true) {
            return
        }
        _state.value = UpdateState.Downloading(release, 0f, 0L, 0L)
        downloadJob = scope.launch {
            try {
                val file = fetchApk(release)
                _state.value = UpdateState.ReadyToInstall(release, file.absolutePath)
            } catch (e: ReleaseClientException) {
                Log.w(tag, "Update download failed", e)
                _state.value = UpdateState.Failed(e.error)
            } catch (e: Exception) {
                Log.w(tag, "Update download failed", e)
                _state.value = UpdateState.Failed(UpdateError.DOWNLOAD_FAILED)
            }
        }
    }

    private suspend fun fetchApk(release: ReleaseInfo): File = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, UPDATES_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw ReleaseClientException(UpdateError.DOWNLOAD_FAILED, "no update directory")
        }
        // Only keep the file being downloaded, stale APKs waste space.
        directory.listFiles()?.forEach { it.delete() }

        val file = File(directory, release.apkName.ifEmpty { "update.apk" })
        val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "DSU-Sideloader/${BuildConfig.VERSION_NAME}")
        }

        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw ReleaseClientException(UpdateError.DOWNLOAD_FAILED, "HTTP $code")
        }
        val total = connection.contentLengthLong

        val digest = MessageDigest.getInstance("SHA-256")
        connection.inputStream.use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var written = 0L
                var lastPercent = -1
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    written += read
                    val progress = if (total > 0) written.toFloat() / total else 0f
                    val percent = (progress * 100).toInt()
                    if (percent != lastPercent) {
                        lastPercent = percent
                        _state.value = UpdateState.Downloading(release, progress, written, total)
                    }
                }
            }
        }
        connection.disconnect()

        verifyChecksum(release, file, digest.digest().joinToString("") { "%02x".format(it) })
        file
    }

    /**
     * Compares the downloaded file with the release `SHA256SUMS`. When the
     * release does not publish checksums the file is accepted (the signature
     * check of the system installer still applies) but it is logged.
     */
    private fun verifyChecksum(release: ReleaseInfo, file: File, actual: String) {
        if (release.checksumsUrl.isEmpty()) {
            Log.i(tag, "Release ${release.tag} publishes no SHA256SUMS, skipping verification")
            return
        }
        val expected = try {
            val text = client.getText(release.checksumsUrl)
            ReleaseParser.checksumFor(ReleaseParser.parseChecksums(text), release.apkName)
        } catch (e: Exception) {
            Log.w(tag, "Could not download SHA256SUMS", e)
            null
        }

        if (expected == null) {
            Log.i(tag, "SHA256SUMS does not list ${release.apkName}, skipping verification")
            return
        }
        if (!expected.equals(actual, ignoreCase = true)) {
            file.delete()
            throw ReleaseClientException(
                UpdateError.CHECKSUM_MISMATCH,
                "expected $expected, got $actual",
            )
        }
        Log.i(tag, "Update APK checksum verified")
    }

    /** Opens the system installer for the downloaded APK. */
    fun install(): Boolean {
        val state = _state.value
        if (state !is UpdateState.ReadyToInstall) {
            return false
        }
        val file = File(state.apkPath)
        if (!file.exists()) {
            _state.value = UpdateState.Failed(UpdateError.DOWNLOAD_FAILED)
            return false
        }
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.Failed(UpdateError.INSTALL_NOT_ALLOWED)
            return false
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(tag, "Could not start the installer", e)
            _state.value = UpdateState.Failed(UpdateError.INSTALL_NOT_ALLOWED)
            false
        }
    }

    /** Opens the Android setting that allows installing unknown apps. */
    fun openInstallPermissionSettings() {
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(tag, "Could not open the unknown app sources setting", e)
        }
    }

    /** Opens the release page of the available update in the browser. */
    fun openReleasePage() {
        val url = _state.value.releaseOrNull()?.pageUrl ?: ReleaseClient.GITHUB_RELEASES_PAGE
        openUrl(url)
    }

    fun openAllReleases() = openUrl(ReleaseClient.GITHUB_RELEASES_PAGE)

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(tag, "Could not open $url", e)
        }
    }

    /** Hides the banner until a newer release than [tag] is published. */
    fun dismiss(tag: String) {
        _dismissedTag.value = tag
        scope.launch {
            DataStoreUtils.updateStringPref(dataStore, AppPrefs.UPDATE_DISMISSED_TAG, tag)
        }
    }

    /** Whether the home banner should be shown for the current state. */
    fun shouldShowBanner(): Boolean {
        val release = _state.value.releaseOrNull() ?: return false
        return release.tag != _dismissedTag.value
    }

    /** Puts the state back to idle, used after a failure is acknowledged. */
    fun clearError() {
        if (_state.value is UpdateState.Failed) {
            _state.value = UpdateState.Idle
        }
    }

    private companion object {
        const val UPDATES_DIRECTORY = "updates"
        const val BUFFER_SIZE = 64 * 1024
        const val TIMEOUT_MS = 30_000

        /** Six hours: often enough to notice a release, rare enough for the API. */
        const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
