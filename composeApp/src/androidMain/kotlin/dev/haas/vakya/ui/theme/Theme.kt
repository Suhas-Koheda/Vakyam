package dev.haas.vakya.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import vakya.composeapp.generated.resources.*
import vakya.composeapp.generated.resources.Res
import vakya.composeapp.generated.resources.jetbrainsmono_regular
import vakya.composeapp.generated.resources.jetbrainsmono_bold

private val LightColorScheme = lightColorScheme(
    primary = Crimson,
    onPrimary = White,
    primaryContainer = Blush,
    onPrimaryContainer = Abyss, // Using a dark color for contrast
    secondary = Violet,
    onSecondary = White,
    secondaryContainer = Lavender,
    onSecondaryContainer = Abyss,
    tertiary = Blush,
    onTertiary = White,
    background = Petal,
    onBackground = Slate,
    surface = White,
    onSurface = Slate,
    surfaceVariant = Petal,
    onSurfaceVariant = Slate,
    outline = Slate
)

private val DarkColorScheme = darkColorScheme(
    primary = Ember,
    onPrimary = Void,
    primaryContainer = Dusk,
    onPrimaryContainer = White, // More contrast on dark dusk
    secondary = Amethyst,
    onSecondary = Void,
    secondaryContainer = Dusk,
    onSecondaryContainer = White,
    tertiary = Coral,
    onTertiary = Void,
    background = Void,
    onBackground = Mauve,
    surface = Void,
    onSurface = Mauve,
    surfaceVariant = Abyss,
    onSurfaceVariant = Mauve,
    outline = Dusk
)

@Composable
fun VakyaTypography(): Typography {
    val jetbrainsMono = FontFamily(
        Font(Res.font.jetbrainsmono_regular, FontWeight.Normal),
        Font(Res.font.jetbrainsmono_bold, FontWeight.Bold)
    )

    val defaultTypography = Typography()
    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = jetbrainsMono),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = jetbrainsMono),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = jetbrainsMono),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = jetbrainsMono),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = jetbrainsMono),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = jetbrainsMono),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = jetbrainsMono),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = jetbrainsMono),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = jetbrainsMono),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = jetbrainsMono),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = jetbrainsMono),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = jetbrainsMono),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = jetbrainsMono),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = jetbrainsMono),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = jetbrainsMono)
    )
}

@Composable
fun VakyaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VakyaTypography(),
        content = content
    )
}
