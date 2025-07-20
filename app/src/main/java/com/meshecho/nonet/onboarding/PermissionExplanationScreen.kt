package com.meshecho.nonet.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Permission explanation screen shown before requesting permissions
 * Explains why Echo needs each permission and reassures users about privacy
 */
@Composable
fun PermissionExplanationScreen(
    permissionCategories: List<PermissionCategory>,
    onContinue: () -> Unit
) {
    // Define your custom ColorScheme here or get it from your AppTheme
    // For demonstration, I'm defining a sample lightColorScheme.
    // In a real app, this would typically be provided by your MaterialTheme.
    val customLightColorScheme = lightColorScheme(
        primary = Color(0xFF673AB7), // Deep Purple 500
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1C4E9), // Deep Purple 100
        onPrimaryContainer = Color(0xFF21005D),

        secondary = Color(0xFF03A9F4), // Light Blue 500
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFB3E5FC), // Light Blue 100
        onSecondaryContainer = Color(0xFF001F2A),

        tertiary = Color(0xFFFFC107), // Amber 500 (for warnings/highlights)
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFFFFECB3), // Amber 100
        onTertiaryContainer = Color(0xFF2B1900),

        background = Color(0xFFF8F8F8), // Slightly off-white background
        onBackground = Color(0xFF1C1B1F),

        surface = Color.White, // Main surface color
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC), // Used for cards/containers
        onSurfaceVariant = Color(0xFF49454F),
        surfaceContainer = Color(0xFFF3EDF7), // Elevated surface for cards
        surfaceContainerLow = Color(0xFFF7F2FA), // Slightly lower elevation
        surfaceContainerHigh = Color(0xFFEDE7F2), // Slightly higher elevation

        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002)
    )

    // Use this colorScheme if you're not already providing one via MaterialTheme
    // For this example, we'll override the local colorScheme.
    val colorScheme = customLightColorScheme
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 96.dp) // Increased space for the fixed button
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp) // Increased spacing between sections
        ) {
            Spacer(modifier = Modifier.height(32.dp)) // More top padding
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to Echo",
                    style = MaterialTheme.typography.displaySmall.copy( // Larger, more impactful title
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold, // Even bolder
                        color = colorScheme.primary // Uses new primary color
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp)) // More space

                Text(
                    text = "messaging over Bluetooth",
                    style = MaterialTheme.typography.titleMedium.copy( // More prominent subtitle
                        fontFamily = FontFamily.Monospace,
                        color = colorScheme.onSurfaceVariant // Uses new onSurfaceVariant color
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Privacy assurance section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainerLow // Uses new surfaceContainerLow color
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Increased elevation
                shape = MaterialTheme.shapes.large // Rounded corners for the card
            ) {
                Column(
                    modifier = Modifier.padding(20.dp), // Increased padding
                    verticalArrangement = Arrangement.spacedBy(12.dp) // More space between items
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing
                    ) {
                        Icon( // Using an Icon instead of emoji for better visual consistency
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy Protected",
                            tint = colorScheme.primary, // Uses new primary color
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Your Privacy is Protected",
                            style = MaterialTheme.typography.titleMedium.copy( // More prominent title
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface // Uses new onSurface color
                            )
                        )
                    }

                    Text(
                        text = "• No servers, no internet required, no data logging\n" +
                                "• Location permission is only used by Android for Bluetooth scanning\n" +
                                "• Your messages stay on your device and peer devices only",
                        style = MaterialTheme.typography.bodyMedium.copy( // Slightly larger body text
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onSurfaceVariant // Uses new onSurfaceVariant color
                        )
                    )
                }
            }

            Text(
                text = "To work properly, Echo needs these permissions:",
                style = MaterialTheme.typography.titleSmall.copy( // More prominent instruction text
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface // Uses new onSurface color
                )
            )

            // Permission categories
            permissionCategories.forEach { category ->
                PermissionCategoryCard(
                    category = category,
                    colorScheme = colorScheme // Pass the new colorScheme
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // Final spacing before the button area
        }

        // Fixed button at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = colorScheme.surface, // Uses new surface color
            tonalElevation = 8.dp // Use tonalElevation for a subtle lift
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp) // More vertical padding
                    .height(56.dp), // Fixed height for a robust button
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary // Uses new primary color
                ),
                shape = MaterialTheme.shapes.medium // Standard button shape
            ) {
                Text(
                    text = "Grant Permissions",
                    style = MaterialTheme.typography.titleMedium.copy( // Larger button text
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun PermissionCategoryCard(
    category: PermissionCategory,
    colorScheme: ColorScheme // Accept colorScheme as parameter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer // Uses new surfaceContainer color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Increased elevation
        shape = MaterialTheme.shapes.medium // Standard rounded corners
    ) {
        Column(
            modifier = Modifier.padding(20.dp), // Increased padding
            verticalArrangement = Arrangement.spacedBy(12.dp) // More space between items
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing for icon and text
            ) {
                // Using a Box to apply size directly to the Text for emoji
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) { // Larger emoji container
                    Text(
                        text = getPermissionEmoji(category.type),
                        style = MaterialTheme.typography.headlineSmall, // Larger emoji
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = category.type.nameValue,
                    style = MaterialTheme.typography.titleLarge.copy( // Larger and bolder title for category
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface // Uses new onSurface color
                    )
                )
            }

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyLarge.copy( // Larger body text
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurfaceVariant, // Uses new onSurfaceVariant color
                    lineHeight = 22.sp // Increased line height for readability
                )
            )

            if (category.type == PermissionType.PRECISE_LOCATION) {
                // Extra emphasis for location permission
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon( // Using an Icon for warning
                        imageVector = Icons.Default.Info, // Or Warning if you prefer
                        contentDescription = "Warning",
                        tint = colorScheme.tertiary, // Uses new tertiary color for warning
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Echo does NOT use GPS or track location",
                        style = MaterialTheme.typography.bodyMedium.copy( // Body medium for emphasis
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.tertiary // Uses new tertiary color
                        )
                    )
                }
            }
        }
    }
}

// These functions remain unchanged as per the requirement
private fun getPermissionEmoji(permissionType: PermissionType): String {
    return when (permissionType) {
        PermissionType.NEARBY_DEVICES -> "📱"
        PermissionType.PRECISE_LOCATION -> "📍"
        PermissionType.NOTIFICATIONS -> "🔔"
        PermissionType.OTHER -> "🔧"
    }
}

private fun getPermissionIconColor(permissionType: PermissionType): Color {
    // This function is no longer directly used for tinting the emoji,
    // as Material Theme colors are used for the Icon, but kept for consistency.
    // If you plan to use this for tinting Text emojis, ensure contrast.
    return when (permissionType) {
        PermissionType.NEARBY_DEVICES -> Color(0xFF2196F3) // Blue
        PermissionType.PRECISE_LOCATION -> Color(0xFFFF9800) // Orange
        PermissionType.NOTIFICATIONS -> Color(0xFF4CAF50) // Green
        PermissionType.OTHER -> Color(0xFF9C27B0) // Purple
    }
}