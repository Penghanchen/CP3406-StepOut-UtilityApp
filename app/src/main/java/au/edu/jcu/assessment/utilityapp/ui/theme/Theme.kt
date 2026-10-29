package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFFA6412E),
    onPrimary = Color(0xFFFFFAF5),
    primaryContainer = Color(0xFFF3D581),
    onPrimaryContainer = Color(0xFF3B2A0C),
    secondary = Color(0xFF705C50),
    onSecondary = Color(0xFFFFFAF5),
    secondaryContainer = Color(0xFFEEDFD2),
    onSecondaryContainer = Color(0xFF3F3029),
    tertiary = Color(0xFF71581F),
    onTertiary = Color(0xFFFFFAF5),
    tertiaryContainer = Color(0xFFF8E8B8),
    onTertiaryContainer = Color(0xFF49350B),
    background = Color(0xFFF7F3EC),
    onBackground = Color(0xFF272420),
    surface = Color(0xFFF7F3EC),
    onSurface = Color(0xFF272420),
    surfaceVariant = Color(0xFFECE3D8),
    onSurfaceVariant = Color(0xFF665A50),
    surfaceDim = Color(0xFFE1D8CE),
    surfaceBright = Color(0xFFFFFBF5),
    surfaceContainerLowest = Color(0xFFFFFCF7),
    surfaceContainerLow = Color(0xFFF2ECE3),
    surfaceContainer = Color(0xFFECE4D9),
    surfaceContainerHigh = Color(0xFFE6DCD0),
    surfaceContainerHighest = Color(0xFFDED2C4),
    surfaceTint = Color(0xFFA6412E),
    outline = Color(0xFF84786D),
    outlineVariant = Color(0xFFD5CABC),
    inverseSurface = Color(0xFF342E29),
    inverseOnSurface = Color(0xFFF7EEE2),
    inversePrimary = Color(0xFFFFAC94),
    error = Color(0xFFA72D25),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF5A1711),
    scrim = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA38B),
    onPrimary = Color(0xFF571F13),
    primaryContainer = Color(0xFF51412A),
    onPrimaryContainer = Color(0xFFFFE8B4),
    secondary = Color(0xFFDCC2AE),
    onSecondary = Color(0xFF3F3029),
    secondaryContainer = Color(0xFF4C3A31),
    onSecondaryContainer = Color(0xFFF2DDCC),
    tertiary = Color(0xFFE7C778),
    onTertiary = Color(0xFF3D2E0A),
    tertiaryContainer = Color(0xFF57441B),
    onTertiaryContainer = Color(0xFFFFE8A6),
    background = Color(0xFF211D1A),
    onBackground = Color(0xFFF3EADF),
    surface = Color(0xFF211D1A),
    onSurface = Color(0xFFF3EADF),
    surfaceVariant = Color(0xFF4A4037),
    onSurfaceVariant = Color(0xFFD3C3B3),
    surfaceDim = Color(0xFF191613),
    surfaceBright = Color(0xFF413831),
    surfaceContainerLowest = Color(0xFF171411),
    surfaceContainerLow = Color(0xFF27211C),
    surfaceContainer = Color(0xFF2E261F),
    surfaceContainerHigh = Color(0xFF382E26),
    surfaceContainerHighest = Color(0xFF43372D),
    surfaceTint = Color(0xFFFFA38B),
    outline = Color(0xFFA89889),
    outlineVariant = Color(0xFF5B4D41),
    inverseSurface = Color(0xFFF0E5D8),
    inverseOnSurface = Color(0xFF352D26),
    inversePrimary = Color(0xFFA6412E),
    error = Color(0xFFFFB4A6),
    onError = Color(0xFF5C1710),
    errorContainer = Color(0xFF782820),
    onErrorContainer = Color(0xFFFFDAD3),
    scrim = Color(0xFF000000)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MobileTech_UtilityAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
