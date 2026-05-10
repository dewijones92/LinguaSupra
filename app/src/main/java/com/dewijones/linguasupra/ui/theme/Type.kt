package com.dewijones.linguasupra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dewijones.linguasupra.R

/**
 * Fredoka — a friendly rounded sans, bundled as a variable TTF (weight + width axes).
 * OFL-licensed. Compose synthesises the requested weights from the variable font.
 */
private val Fredoka = FontFamily(
    Font(R.font.fredoka, weight = FontWeight.Normal),
    Font(R.font.fredoka, weight = FontWeight.Medium),
    Font(R.font.fredoka, weight = FontWeight.SemiBold),
    Font(R.font.fredoka, weight = FontWeight.Bold),
)

internal val LinguaTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
        displaySmall = displaySmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.Medium),
        titleSmall = titleSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(fontFamily = Fredoka),
        bodyMedium = bodyMedium.copy(fontFamily = Fredoka),
        bodySmall = bodySmall.copy(fontFamily = Fredoka),
        labelLarge = labelLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.Medium),
    )
}

internal val MonoStyle: TextStyle = TextStyle(fontFamily = FontFamily.Monospace)
