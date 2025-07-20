// ui.theme/Theme.kt (example)
package com.meshecho.nonet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color


// Define a fresh, modern Light Color Scheme
val ModernChatLightColorScheme = lightColorScheme(
    primary = Color(0xFF00796B), // Dark Teal - Main accent color for buttons, active states
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB), // Lighter Teal for subtle backgrounds (e.g., received bubble)
    onPrimaryContainer = Color(0xFF004D40),

    secondary = Color(0xFF536DFE), // Indigo - Complementary accent for sent messages, FAB
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5CAE9), // Light Indigo for sent message bubbles
    onSecondaryContainer = Color(0xFF1A237E),

    tertiary = Color(0xFFFFAB40), // Amber - For alerts, warnings, or distinct elements (e.g., read status)
    onTertiary = Color.Black,

    background = Color(0xFFF0F2F5), // Very light grey - Main chat background
    onBackground = Color(0xFF212121), // Dark text on background

    surface = Color.White, // Default surface for cards, input field, etc.
    onSurface = Color(0xFF212121), // Dark text on surface
    surfaceVariant = Color(0xFFE0E0E0), // Light grey for dividers, subtle elements
    onSurfaceVariant = Color(0xFF616161),

    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C)
)

// Define your light and dark color schemes here
private val DarkColorScheme = darkColorScheme(
    // ... your dark colors
)

private val LightColorScheme = lightColorScheme(
    // ... your light colors
)



// Define your WhatsApp-like Light Color Scheme
val WhatsAppLightColorScheme = lightColorScheme(
    primary = Color(0xFF075E54), // Dark Teal - WhatsApp's primary green
    onPrimary = Color.White,
    primaryContainer = Color(0xFF128C7E), // Teal - lighter shade for accents
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF25D366), // Bright Green - for FAB or key actions
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCF8C6), // Very light green - for sent message bubbles
    onSecondaryContainer = Color.Black,

    tertiary = Color(0xFF34B7F1), // Light Blue - for timestamps/read receipts
    onTertiary = Color.White,

    background = Color(0xFFECE5DD), // Light background for chat area
    onBackground = Color(0xFF1C1B1F), // Dark text on background

    surface = Color.White, // Default surface for app bars, cards
    onSurface = Color(0xFF1C1B1F), // Dark text on surface
    surfaceVariant = Color(0xFFE0E0E0), // Light grey for dividers, borders
    onSurfaceVariant = Color(0xFF49454F),

    // Specific colors for chat bubbles (not standard Material3 roles, used custom)
    // You might map these to secondaryContainer/onSecondaryContainer for sent,
    // and surface/onSurface for received, as done below.

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun BitchatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, // This is where you apply the scheme
        typography = Typography, // Assuming you have a Typography object
        content = content
    )
}