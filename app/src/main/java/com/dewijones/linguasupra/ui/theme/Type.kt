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
 * Used for body, labels, and most titles. OFL-licensed.
 */
private val Fredoka = FontFamily(
    Font(R.font.fredoka, weight = FontWeight.Normal),
    Font(R.font.fredoka, weight = FontWeight.Medium),
    Font(R.font.fredoka, weight = FontWeight.SemiBold),
    Font(R.font.fredoka, weight = FontWeight.Bold),
)

/**
 * Bagel Fat One — chunky, sticker-book display face. Used for the app title and
 * the daily greeting only, so headlines feel bold without overpowering the rest.
 * OFL-licensed.
 */
private val BagelFatOne = FontFamily(Font(R.font.bagel_fat_one, weight = FontWeight.Normal))

/**
 * Display headlines should be available outside Typography too (for the app title,
 * which uses titleLarge by default in TopAppBar but wants the bigger display face).
 */
internal val DisplayBagel = TextStyle(fontFamily = BagelFatOne, letterSpacing = 0.sp)

internal val LinguaTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = BagelFatOne, letterSpacing = 0.sp),
        displayMedium = displayMedium.copy(fontFamily = BagelFatOne, letterSpacing = 0.sp),
        displaySmall = displaySmall.copy(fontFamily = BagelFatOne, letterSpacing = 0.sp),
        headlineLarge = headlineLarge.copy(fontFamily = BagelFatOne, letterSpacing = 0.sp),
        headlineMedium = headlineMedium.copy(fontFamily = BagelFatOne, letterSpacing = 0.sp),
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
