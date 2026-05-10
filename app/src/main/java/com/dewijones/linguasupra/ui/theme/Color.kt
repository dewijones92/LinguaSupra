package com.dewijones.linguasupra.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Warm-coral / sunny-yellow palette used when dynamic colour is unavailable
 * (pre-S devices and themes the user hasn't enabled).
 */
internal object Palette {
    // Light
    val PrimaryLight = Color(0xFFE5572E)           // coral
    val OnPrimaryLight = Color.White
    val PrimaryContainerLight = Color(0xFFFFD7C8)
    val OnPrimaryContainerLight = Color(0xFF3B0E00)

    val SecondaryLight = Color(0xFFFFB300)         // sunny yellow
    val OnSecondaryLight = Color(0xFF2A1A00)
    val SecondaryContainerLight = Color(0xFFFFE6B3)
    val OnSecondaryContainerLight = Color(0xFF2A1A00)

    val TertiaryLight = Color(0xFF4DA46A)          // mint
    val OnTertiaryLight = Color.White
    val TertiaryContainerLight = Color(0xFFB8E8C4)
    val OnTertiaryContainerLight = Color(0xFF052712)

    val BackgroundLight = Color(0xFFFFF8F4)
    val OnBackgroundLight = Color(0xFF20140E)
    val SurfaceLight = Color(0xFFFFF8F4)
    val OnSurfaceLight = Color(0xFF20140E)
    val SurfaceVariantLight = Color(0xFFF5DACA)
    val OnSurfaceVariantLight = Color(0xFF53433A)
    val OutlineLight = Color(0xFF867368)

    // Dark
    val PrimaryDark = Color(0xFFFFB59A)
    val OnPrimaryDark = Color(0xFF5C1B00)
    val PrimaryContainerDark = Color(0xFF822B0E)
    val OnPrimaryContainerDark = Color(0xFFFFD7C8)

    val SecondaryDark = Color(0xFFFFD480)
    val OnSecondaryDark = Color(0xFF422D00)
    val SecondaryContainerDark = Color(0xFF5E4200)
    val OnSecondaryContainerDark = Color(0xFFFFE6B3)

    val TertiaryDark = Color(0xFF9CD3A8)
    val OnTertiaryDark = Color(0xFF103E20)
    val TertiaryContainerDark = Color(0xFF2C5739)
    val OnTertiaryContainerDark = Color(0xFFB8E8C4)

    val BackgroundDark = Color(0xFF181210)
    val OnBackgroundDark = Color(0xFFEDDDD3)
    val SurfaceDark = Color(0xFF181210)
    val OnSurfaceDark = Color(0xFFEDDDD3)
    val SurfaceVariantDark = Color(0xFF53433A)
    val OnSurfaceVariantDark = Color(0xFFD8C2B6)
    val OutlineDark = Color(0xFFA08D81)
}
