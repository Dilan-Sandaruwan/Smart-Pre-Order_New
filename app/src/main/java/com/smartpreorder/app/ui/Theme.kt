package com.smartpreorder.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartpreorder.app.R

// Brand palette (from the SmartPreOrder design system)
val BrandOrange = Color(0xFFFF6B00)
val BrandAmber = Color(0xFFFFB347)
val BrandDark = Color(0xFF0F1419)
val BrandLight = Color(0xFFF8F7F4)
val BrandSuccess = Color(0xFF22C55E)
val BrandError = Color(0xFFEF4444)
val BrandSurface = Color(0xFF171E26)
val BrandSurfaceHigh = Color(0xFF1E2731)

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_bold, FontWeight.ExtraBold),
)

private val PremiumDark = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1D08),
    onPrimaryContainer = BrandAmber,
    secondary = BrandSuccess,
    onSecondary = Color(0xFF04210F),
    secondaryContainer = Color(0xFF3A1D08),
    onSecondaryContainer = BrandAmber,
    tertiary = BrandAmber,
    onTertiary = Color(0xFF2B1600),
    background = BrandDark,
    onBackground = BrandLight,
    surface = BrandDark,
    onSurface = BrandLight,
    surfaceVariant = BrandSurfaceHigh,
    onSurfaceVariant = Color(0xFFB4BCC6),
    surfaceContainerLowest = Color(0xFF0B1014),
    surfaceContainerLow = Color(0xFF131A21),
    surfaceContainer = BrandSurface,
    surfaceContainerHigh = BrandSurfaceHigh,
    surfaceContainerHighest = BrandSurfaceHigh,
    outline = Color(0xFF8B95A1),
    outlineVariant = Color(0xFF2B3541),
    error = BrandError,
    onError = Color.White,
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = Color(0xFFFFB4B4),
)

private fun poppinsTypography(): Typography {
    val b = Typography()
    fun TextStyle.p() = copy(fontFamily = Poppins)
    return Typography(
        displayLarge = b.displayLarge.p(), displayMedium = b.displayMedium.p(), displaySmall = b.displaySmall.p(),
        headlineLarge = b.headlineLarge.p().copy(fontWeight = FontWeight.Bold),
        headlineMedium = b.headlineMedium.p().copy(fontWeight = FontWeight.Bold),
        headlineSmall = b.headlineSmall.p().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = b.titleLarge.p().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = b.titleMedium.p().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = b.titleSmall.p().copy(fontWeight = FontWeight.Medium),
        bodyLarge = b.bodyLarge.p(), bodyMedium = b.bodyMedium.p(), bodySmall = b.bodySmall.p(),
        labelLarge = b.labelLarge.p().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = b.labelMedium.p().copy(fontWeight = FontWeight.Medium),
        labelSmall = b.labelSmall.p().copy(fontWeight = FontWeight.Medium),
    )
}

private val PremiumShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun SmartPreOrderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremiumDark,
        typography = poppinsTypography(),
        shapes = PremiumShapes,
        content = content
    )
}
