package vegabobo.dsusideloader.update

import java.security.MessageDigest

/**
 * Signing certificate comparison used before handing an update APK to the
 * system installer.
 *
 * Android only installs an update when the new APK is signed with the same key
 * as the installed app; otherwise the installer fails with
 * `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ("package conflicts with an existing
 * package"). Comparing the certificates beforehand lets the app explain the
 * problem instead of letting that error surface unexplained.
 *
 * Pure and side effect free so it can be unit tested without Android.
 */
object ApkSignature {

    /**
     * Whether [candidate] (the downloaded APK) may replace [installed] (this
     * app).
     *
     * When either side is unknown (null or empty) the answer is `true`: the
     * check is a pre-flight convenience, the system installer stays the
     * authority.
     */
    fun sameSigner(installed: List<ByteArray>?, candidate: List<ByteArray>?): Boolean {
        if (installed.isNullOrEmpty() || candidate.isNullOrEmpty()) {
            return true
        }
        return installed.map(::sha256).sorted() == candidate.map(::sha256).sorted()
    }

    /** Short, comparable fingerprint of one certificate. */
    fun fingerprint(signature: ByteArray): String = sha256(signature)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
