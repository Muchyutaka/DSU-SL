package vegabobo.dsusideloader.ui.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether decorative animations should run.
 *
 * Compose already scales its animations with the system animator duration
 * scale, but a scale of 0 (animations turned off in the developer options or
 * accessibility settings) must also stop looping animations such as the status
 * pulse, which are not driven by the motion duration scale.
 *
 * The value is read once per composition: changing it requires the activity to
 * be recreated, which Android does on its own for developer options changes.
 */
@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        } catch (_: Exception) {
            true
        }
    }
}

/**
 * Duration to use for a [androidx.compose.animation.core.tween], or 0 when the
 * user disabled animations so state changes snap instead of animating.
 */
@Composable
fun motionDuration(defaultMillis: Int): Int = if (rememberMotionEnabled()) defaultMillis else 0
