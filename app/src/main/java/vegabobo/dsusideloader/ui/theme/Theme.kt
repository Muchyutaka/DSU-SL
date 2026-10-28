package vegabobo.dsusideloader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00504B),
    onPrimaryContainer = Color(0xFF9CF2E6),
    secondary = TealGrey80,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFD0E8E3),
    tertiary = Sand80,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF364A47),
    onTertiaryContainer = Color(0xFFCDE8E3),
    background = InkDark,
    onBackground = OnInkDark,
    surface = InkDark,
    onSurface = OnInkDark,
    surfaceVariant = InkSurfaceDark,
    onSurfaceVariant = OnInkVariantDark,
    outline = Color(0xFF899491),
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF2E6),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = TealGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E3),
    onSecondaryContainer = Color(0xFF051F1C),
    tertiary = Sand40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCE8E3),
    onTertiaryContainer = Color(0xFF00201D),
    background = CreamLight,
    onBackground = OnCreamLight,
    surface = CreamLight,
    onSurface = OnCreamLight,
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

/** Rounded, Material 3 pill friendly shape scale used by the app. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Forces a pure black background on a dark scheme.
 *
 * Accent colors (primary, tertiary, containers) are left untouched, so Material
 * You colors keep driving the cards, badges and buttons while the app
 * background stays true black. Foreground colors are inherited from the
 * platform scheme, so contrast is preserved.
 *
 * @param flattenSurfaces also paints the surfaces black (AMOLED switch).
 */
private fun ColorScheme.toBlackBackgroundScheme(flattenSurfaces: Boolean): ColorScheme =
    if (flattenSurfaces) {
        copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color.Black)
    } else {
        copy(background = Color.Black)
    }

@Composable
fun DSUHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Same branch as Reply (android/compose-samples): dynamic color when the
    // platform supports it, static palette otherwise.
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    // Dark mode always uses a true black background; the AMOLED switch also
    // flattens the surfaces (used by the settings screen).
    val colorScheme = if (isDark) {
        baseColorScheme.toBlackBackgroundScheme(flattenSurfaces = amoledDark)
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
