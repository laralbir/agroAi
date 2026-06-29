package com.laralnet.agroai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.laralnet.agroai.ui.screens.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = SageGreen40,
    onPrimary = Color.White,
    primaryContainer = SageGreen95,
    onPrimaryContainer = SageGreen10,
    secondary = SageGrey50,
    onSecondary = Color.White,
    secondaryContainer = SageGrey90,
    onSecondaryContainer = SageGrey10,
    tertiary = WarmAmber40,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmber90,
    onTertiaryContainer = WarmAmber30,
    error = ErrorRed40,
    onError = Color.White,
    errorContainer = ErrorRed90,
    onErrorContainer = ErrorRed10,
    background = WarmGrey99,
    onBackground = WarmGrey10,
    surface = WarmGrey99,
    onSurface = WarmGrey10,
    surfaceVariant = SageGreen95,
    onSurfaceVariant = SageGrey30,
    outline = SageGrey60
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen80,
    onPrimary = SageGreen10,
    primaryContainer = SageGreen20,
    onPrimaryContainer = SageGreen90,
    secondary = SageGrey80,
    onSecondary = SageGrey10,
    secondaryContainer = SageGrey20,
    onSecondaryContainer = SageGrey80,
    tertiary = WarmAmber80,
    onTertiary = WarmAmber30,
    tertiaryContainer = Color(0xFF4A3200),
    onTertiaryContainer = WarmAmber90,
    error = ErrorRed80,
    onError = ErrorRed20,
    errorContainer = ErrorRed30,
    onErrorContainer = ErrorRed90,
    background = Color(0xFF111511),
    onBackground = WarmGrey90,
    surface = Color(0xFF111511),
    onSurface = WarmGrey90,
    surfaceVariant = Color(0xFF202D21),
    onSurfaceVariant = SageGrey80,
    outline = SageGrey60
)

@Composable
fun AgroAITheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AgroAITypography,
        content = content
    )
}
