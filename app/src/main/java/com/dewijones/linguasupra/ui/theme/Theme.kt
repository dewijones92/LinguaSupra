package com.dewijones.linguasupra.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightFallback = lightColorScheme(
    primary = Palette.PrimaryLight,
    onPrimary = Palette.OnPrimaryLight,
    primaryContainer = Palette.PrimaryContainerLight,
    onPrimaryContainer = Palette.OnPrimaryContainerLight,
    secondary = Palette.SecondaryLight,
    onSecondary = Palette.OnSecondaryLight,
    secondaryContainer = Palette.SecondaryContainerLight,
    onSecondaryContainer = Palette.OnSecondaryContainerLight,
    tertiary = Palette.TertiaryLight,
    onTertiary = Palette.OnTertiaryLight,
    tertiaryContainer = Palette.TertiaryContainerLight,
    onTertiaryContainer = Palette.OnTertiaryContainerLight,
    background = Palette.BackgroundLight,
    onBackground = Palette.OnBackgroundLight,
    surface = Palette.SurfaceLight,
    onSurface = Palette.OnSurfaceLight,
    surfaceVariant = Palette.SurfaceVariantLight,
    onSurfaceVariant = Palette.OnSurfaceVariantLight,
    outline = Palette.OutlineLight,
)

private val DarkFallback = darkColorScheme(
    primary = Palette.PrimaryDark,
    onPrimary = Palette.OnPrimaryDark,
    primaryContainer = Palette.PrimaryContainerDark,
    onPrimaryContainer = Palette.OnPrimaryContainerDark,
    secondary = Palette.SecondaryDark,
    onSecondary = Palette.OnSecondaryDark,
    secondaryContainer = Palette.SecondaryContainerDark,
    onSecondaryContainer = Palette.OnSecondaryContainerDark,
    tertiary = Palette.TertiaryDark,
    onTertiary = Palette.OnTertiaryDark,
    tertiaryContainer = Palette.TertiaryContainerDark,
    onTertiaryContainer = Palette.OnTertiaryContainerDark,
    background = Palette.BackgroundDark,
    onBackground = Palette.OnBackgroundDark,
    surface = Palette.SurfaceDark,
    onSurface = Palette.OnSurfaceDark,
    surfaceVariant = Palette.SurfaceVariantDark,
    onSurfaceVariant = Palette.OnSurfaceVariantDark,
    outline = Palette.OutlineDark,
)

@Composable
fun LinguaSupraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(colorScheme = scheme, typography = LinguaTypography, content = content)
}
