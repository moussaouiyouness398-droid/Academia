package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DefaultDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218),
    surface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFF49454F),
    onPrimary = Color(0xFF381E72),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFDF8FD),
    surface = Color(0xFFFEF7FF),
    surfaceVariant = Color(0xFFF3EDF7),
    onPrimary = Color.White,
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF21005D),
    tertiaryContainer = Color(0xFFD3E3FD),
    onTertiaryContainer = Color(0xFF041E49),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFCAC4D0).copy(alpha = 0.5f),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColorHex: String = "#6750A4",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeColorHex) {
        // Cosmic Blue Theme
        "#1E88E5" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF90CAF9),
                    secondary = Color(0xFFB0BEC5),
                    tertiary = Color(0xFF80DEEA),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B),
                    onPrimary = Color(0xFF0D47A1),
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFE2E8F0)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF1E88E5),
                    secondary = Color(0xFF546E7A),
                    tertiary = Color(0xFF00ACC1),
                    background = Color(0xFFF8FAFC),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color.White,
                    onBackground = Color(0xFF0F172A),
                    onSurface = Color(0xFF1E293B)
                )
            }
        }
        // Emerald Green Theme
        "#2E7D32" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFA5D6A7),
                    secondary = Color(0xFFB0C4DE),
                    tertiary = Color(0xFF80CBC4),
                    background = Color(0xFF0F1E16),
                    surface = Color(0xFF1B2E24),
                    onPrimary = Color(0xFF1B5E20),
                    onBackground = Color(0xFFE8F5E9),
                    onSurface = Color(0xFFC8E6C9)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF2E7D32),
                    secondary = Color(0xFF4E7055),
                    tertiary = Color(0xFF00796B),
                    background = Color(0xFFF4FBF4),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color.White,
                    onBackground = Color(0xFF0F1E16),
                    onSurface = Color(0xFF1B2E24)
                )
            }
        }
        // Sunset Orange Theme
        "#E65100" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFFCC80),
                    secondary = Color(0xFFD7CCC8),
                    tertiary = Color(0xFFFFAB91),
                    background = Color(0xFF1F1612),
                    surface = Color(0xFF2E211A),
                    onPrimary = Color(0xFFE65100),
                    onBackground = Color(0xFFFFF3E0),
                    onSurface = Color(0xFFFFE0B2)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFE65100),
                    secondary = Color(0xFF8D6E63),
                    tertiary = Color(0xFFFF7043),
                    background = Color(0xFFFFFAF8),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color.White,
                    onBackground = Color(0xFF1F1612),
                    onSurface = Color(0xFF2E211A)
                )
            }
        }
        // Default Imperial Purple Theme (#6750A4)
        else -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFD0BCFF),
                    secondary = Color(0xFFCCC2DC),
                    tertiary = Color(0xFFEFB8C8),
                    background = Color(0xFF141218),
                    surface = Color(0xFF1D1B20),
                    surfaceVariant = Color(0xFF49454F),
                    onPrimary = Color(0xFF381E72),
                    onBackground = Color(0xFFE6E1E5),
                    onSurface = Color(0xFFE6E1E5),
                    onSurfaceVariant = Color(0xFFCAC4D0),
                    primaryContainer = Color(0xFF4F378B),
                    onPrimaryContainer = Color(0xFFEADDFF),
                    secondaryContainer = Color(0xFF4A4458),
                    onSecondaryContainer = Color(0xFFE8DEF8),
                    tertiaryContainer = Color(0xFF633B48),
                    onTertiaryContainer = Color(0xFFFFD8E4),
                    outline = Color(0xFF938F99),
                    outlineVariant = Color(0xFF49454F)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF6750A4),
                    secondary = Color(0xFF625B71),
                    tertiary = Color(0xFF7D5260),
                    background = Color(0xFFFDF8FD),
                    surface = Color(0xFFFEF7FF),
                    surfaceVariant = Color(0xFFF3EDF7),
                    onPrimary = Color.White,
                    onBackground = Color(0xFF1D1B20),
                    onSurface = Color(0xFF1D1B20),
                    onSurfaceVariant = Color(0xFF49454F),
                    primaryContainer = Color(0xFFEADDFF),
                    onPrimaryContainer = Color(0xFF21005D),
                    secondaryContainer = Color(0xFFE8DEF8),
                    onSecondaryContainer = Color(0xFF21005D),
                    tertiaryContainer = Color(0xFFD3E3FD),
                    onTertiaryContainer = Color(0xFF041E49),
                    outline = Color(0xFFCAC4D0),
                    outlineVariant = Color(0xFFCAC4D0).copy(alpha = 0.5f),
                    error = Color(0xFFB3261E),
                    errorContainer = Color(0xFFF9DEDC),
                    onErrorContainer = Color(0xFF410002)
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
