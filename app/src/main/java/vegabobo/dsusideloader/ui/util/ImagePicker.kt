package vegabobo.dsusideloader.ui.util

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The system file picker used to choose the GSI image.
 *
 * Shared by the installation card and the home screen primary action so both
 * open the exact same picker with the same MIME types.
 */
object ImagePicker {

    private val MIME_TYPES = arrayOf(
        "application/gzip",
        "application/x-gzip",
        "application/x-xz",
        "application/zip",
        "application/octet-stream",
    )

    fun createIntent(): Intent {
        val chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
        chooseFile.type = "*/*"
        chooseFile.putExtra(Intent.EXTRA_MIME_TYPES, MIME_TYPES)
        return Intent.createChooser(chooseFile, "")
    }
}

/** Thin wrapper so call sites only need `picker.launch()`. */
class ImagePickerLauncher(
    private val launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    private val intent: Intent,
) {
    fun launch() {
        launcher.launch(intent)
    }
}

@Composable
fun rememberImagePickerLauncher(onResult: (Uri) -> Unit): ImagePickerLauncher {
    val launcher = launcherAcResult(onResult)
    val intent = remember { ImagePicker.createIntent() }
    return remember(launcher, intent) { ImagePickerLauncher(launcher, intent) }
}
