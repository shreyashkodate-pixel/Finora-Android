package com.finora.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FiscalPrimaryDark,
    onPrimary = FiscalOnPrimaryDark,
    primaryContainer = FiscalPrimaryContainerDark,
    onPrimaryContainer = FiscalOnPrimaryContainerDark,
    secondary = FiscalSecondaryDark,
    onSecondary = FiscalOnSecondaryDark,
    secondaryContainer = FiscalSecondaryContainerDark,
    onSecondaryContainer = FiscalOnSecondaryContainerDark,
    tertiary = FiscalTertiaryDark,
    onTertiary = FiscalOnTertiaryDark,
    tertiaryContainer = FiscalTertiaryContainerDark,
    onTertiaryContainer = FiscalOnTertiaryContainerDark,
    error = FiscalErrorDark,
    onError = FiscalOnErrorDark,
    errorContainer = FiscalErrorContainerDark,
    onErrorContainer = FiscalOnErrorContainerDark,
    background = FiscalBackgroundDark,
    onBackground = FiscalOnBackgroundDark,
    surface = FiscalSurfaceDark,
    onSurface = FiscalOnSurfaceDark,
    surfaceVariant = FiscalSurfaceVariantDark,
    onSurfaceVariant = FiscalOnSurfaceVariantDark,
    outline = FiscalOutlineDark,
    outlineVariant = FiscalOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = FiscalPrimaryLight,
    onPrimary = FiscalOnPrimaryLight,
    primaryContainer = FiscalPrimaryContainerLight,
    onPrimaryContainer = FiscalOnPrimaryContainerLight,
    secondary = FiscalSecondaryLight,
    onSecondary = FiscalOnSecondaryLight,
    secondaryContainer = FiscalSecondaryContainerLight,
    onSecondaryContainer = FiscalOnSecondaryContainerLight,
    tertiary = FiscalTertiaryLight,
    onTertiary = FiscalOnTertiaryLight,
    tertiaryContainer = FiscalTertiaryContainerLight,
    onTertiaryContainer = FiscalOnTertiaryContainerLight,
    error = FiscalErrorLight,
    onError = FiscalOnErrorLight,
    errorContainer = FiscalErrorContainerLight,
    onErrorContainer = FiscalOnErrorContainerLight,
    background = FiscalBackgroundLight,
    onBackground = FiscalOnBackgroundLight,
    surface = FiscalSurfaceLight,
    onSurface = FiscalOnSurfaceLight,
    surfaceVariant = FiscalSurfaceVariantLight,
    onSurfaceVariant = FiscalOnSurfaceVariantLight,
    outline = FiscalOutlineLight,
    outlineVariant = FiscalOutlineVariantLight
)

@Composable
fun FinoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
