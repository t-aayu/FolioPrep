package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorErrorText = androidx.compose.ui.graphics.Color(0xFF410E0B)

private val LightEditorialColorScheme = lightColorScheme(
    primary = ForestSage,
    onPrimary = PaperIvory,
    primaryContainer = SoftSageContainer,
    onPrimaryContainer = OnSoftSage,
    secondary = CharcoalInk,
    onSecondary = PaperIvory,
    secondaryContainer = WarmParchment,
    onSecondaryContainer = CharcoalInk,
    tertiary = WarmTerracotta,
    onTertiary = PaperIvory,
    tertiaryContainer = SoftTerracottaContainer,
    onTertiaryContainer = OnSoftTerracotta,
    background = PaperIvory,
    onBackground = CharcoalInk,
    surface = CreamCard,
    onSurface = CharcoalInk,
    surfaceVariant = ArchivalSand,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = CreamCard,
    surfaceContainerLow = PaperIvory,
    surfaceContainer = ArchivalSand,
    surfaceContainerHigh = WarmParchment,
    outline = StrongStoneOutline,
    outlineVariant = WarmStoneOutline,
    error = EditorialCrimson,
    onError = PaperIvory,
    errorContainer = SoftCrimsonContainer,
    onErrorContainer = ColorErrorText
)

private val DarkEditorialColorScheme = darkColorScheme(
    primary = MutedSageDark,
    onPrimary = OnDarkSage,
    primaryContainer = DarkSageContainer,
    onPrimaryContainer = SoftSageContainer,
    secondary = ParchmentText,
    onSecondary = EspressoInk,
    secondaryContainer = WarmCharcoalSurface,
    onSecondaryContainer = ParchmentText,
    tertiary = TerracottaLight,
    onTertiary = EspressoInk,
    tertiaryContainer = DarkTerracottaContainer,
    onTertiaryContainer = SoftTerracottaContainer,
    background = EspressoInk,
    onBackground = ParchmentText,
    surface = UmberCard,
    onSurface = ParchmentText,
    surfaceVariant = WarmCharcoalSurface,
    onSurfaceVariant = WarmAshText,
    surfaceContainerLowest = EspressoInk,
    surfaceContainerLow = UmberCard,
    surfaceContainer = WarmCharcoalSurface,
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF35312C),
    outline = DarkStoneOutline,
    outlineVariant = DarkStoneOutline,
    error = androidx.compose.ui.graphics.Color(0xFFF2B8B5),
    onError = androidx.compose.ui.graphics.Color(0xFF601410),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF8C1D18),
    onErrorContainer = SoftCrimsonContainer
)

val FolioPrepShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FolioPrepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkEditorialColorScheme else LightEditorialColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FolioPrepTypography,
        shapes = FolioPrepShapes,
        content = content
    )
}
