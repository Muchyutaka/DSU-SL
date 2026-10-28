package vegabobo.dsusideloader.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.util.motionDuration

/**
 * Short launch animation drawn above the real UI.
 *
 * The logo fades in in the centre, stays for a moment and then fades out while
 * sliding left off the screen. [onFinished] is called once the animation is
 * over, the caller then removes the splash from composition.
 *
 * When the system asks for reduced motion the whole thing is skipped (the
 * splash is still shown, just without the fade/slide).
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(0f) }
    val fadeInMillis = motionDuration(420)
    val holdMillis = motionDuration(320)
    val fadeOutMillis = motionDuration(260)
    val slideMillis = motionDuration(340)
    val motionEnabled = fadeInMillis > 0

    LaunchedEffect(Unit) {
        if (!motionEnabled) {
            // Reduced motion: no fade, no slide, just get out of the way.
            delay(220)
            onFinished()
            return@LaunchedEffect
        }
        fade.animateTo(1f, tween(fadeInMillis, easing = FastOutSlowInEasing))
        delay(holdMillis.toLong())
        coroutineScope {
            launch {
                fade.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(fadeOutMillis, easing = FastOutSlowInEasing),
                )
            }
            launch {
                slide.animateTo(
                    targetValue = -1f,
                    animationSpec = tween(slideMillis, easing = FastOutSlowInEasing),
                )
            }
        }
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer { translationX = slide.value * size.width }
            .alpha(fade.value),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_icon_mini),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(28.dp)),
        )
        Text(
            text = stringResource(id = R.string.app_name),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}
