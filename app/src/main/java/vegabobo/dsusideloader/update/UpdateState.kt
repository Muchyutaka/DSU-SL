package vegabobo.dsusideloader.update

/** What the update UI should show. */
sealed interface UpdateState {

    /** Nothing happened yet (or the automatic check is throttled). */
    object Idle : UpdateState

    object Checking : UpdateState

    /** The installed build is the newest one. */
    data class UpToDate(val currentVersion: String) : UpdateState

    /** A newer release exists and can be downloaded. */
    data class Available(val release: ReleaseInfo) : UpdateState

    data class Downloading(
        val release: ReleaseInfo,
        val progress: Float,
        val bytesWritten: Long,
        val totalBytes: Long,
    ) : UpdateState

    /** The APK is downloaded and verified, ready for the system installer. */
    data class ReadyToInstall(val release: ReleaseInfo, val apkPath: String) : UpdateState

    data class Failed(val error: UpdateError) : UpdateState

    fun isBusy(): Boolean = this is Checking || this is Downloading

    fun releaseOrNull(): ReleaseInfo? = when (this) {
        is Available -> release
        is Downloading -> release
        is ReadyToInstall -> release
        else -> null
    }
}
