package vegabobo.dsusideloader.util

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/**
 * Computes the SHA-256 checksum of a content uri.
 *
 * The computation is streamed (the whole image is never loaded in memory) and
 * can be cancelled, so it is safe to run on multi gigabyte images.
 */
object FileChecksum {

    private const val BUFFER_SIZE = 256 * 1024
    private const val HEX_CHARS = "0123456789abcdef"

    /**
     * @param totalBytes size of the stream when known, used to report progress.
     * @param isCancelled polled between reads, cancelling throws
     * [CancellationException].
     * @param onProgress called with values between 0 and 1.
     * @return the lowercase hexadecimal SHA-256 of the stream.
     */
    fun computeSha256(
        contentResolver: ContentResolver,
        uri: Uri,
        totalBytes: Long = -1L,
        isCancelled: () -> Boolean = { false },
        onProgress: (Float) -> Unit = {},
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream for $uri")

        var lastReportedPercent = -1
        inputStream.use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var readTotal = 0L
            while (true) {
                if (isCancelled()) {
                    throw CancellationException("SHA-256 computation cancelled")
                }
                val read = input.read(buffer)
                if (read == -1) {
                    break
                }
                digest.update(buffer, 0, read)
                readTotal += read

                if (totalBytes > 0) {
                    val percent = ((readTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                    if (percent != lastReportedPercent) {
                        lastReportedPercent = percent
                        onProgress(percent / 100F)
                    }
                }
            }
        }
        onProgress(1F)
        return toHex(digest.digest())
    }

    fun toHex(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            builder.append(HEX_CHARS[value ushr 4])
            builder.append(HEX_CHARS[value and 0x0F])
        }
        return builder.toString()
    }
}
