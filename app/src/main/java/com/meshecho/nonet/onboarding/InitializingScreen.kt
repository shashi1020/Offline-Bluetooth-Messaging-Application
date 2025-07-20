package com.meshecho.nonet.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Loading screen shown during app initialization after permissions are granted
 */
@Composable
fun InitializingScreen() {
    val colorScheme = MaterialTheme.colorScheme

    // Animated rotation for the loading indicator
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Animated dots for loading text
    val dotCount = 3
    val animationDelay = 300
    val dots = (0 until dotCount).map { index ->
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animationDelay * dotCount),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(animationDelay * index)
            ),
            label = "dot_$index"
        )
        alpha
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(48.dp), // Increased spacing for a cleaner look
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App title
            Text(
                text = "Echo",
                style = MaterialTheme.typography.displaySmall.copy( // Larger, more impactful title
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold, // Even bolder
                    color = colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )

            // Loading indicator
            Box(
                modifier = Modifier.size(72.dp), // Slightly larger indicator
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle),
                    color = colorScheme.primary,
                    strokeWidth = 4.dp // Slightly thicker stroke
                )
            }

            // Loading text with animated dots
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Initializing mesh network",
                    style = MaterialTheme.typography.titleMedium.copy( // More prominent loading text
                        fontFamily = FontFamily.Monospace,
                        color = colorScheme.onSurfaceVariant // Use onSurfaceVariant for secondary text
                    )
                )

                // Animated dots
                dots.forEach { alpha ->
                    Text(
                        text = ".",
                        style = MaterialTheme.typography.titleMedium.copy( // Match style of main text
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        )
                    )
                }
            }

            // Status message Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainerLow // Use a subtle surface container color
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Increased elevation
                shape = MaterialTheme.shapes.large // Rounded corners for the card
            ) {
                Column(
                    modifier = Modifier.padding(24.dp), // Increased padding inside card
                    verticalArrangement = Arrangement.spacedBy(12.dp), // More space between texts
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Setting up Bluetooth mesh networking...",
                        style = MaterialTheme.typography.bodyLarge.copy( // Larger body text
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onSurface // Primary onSurface color
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "This should only take a few seconds",
                        style = MaterialTheme.typography.bodyMedium.copy( // Body medium for secondary info
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onSurfaceVariant // Muted color for secondary info
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Error screen shown if initialization fails
 */
@Composable
fun InitializationErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp), // Increased spacing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.errorContainer // Use Material 3 error container color
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // More prominent elevation
                shape = MaterialTheme.shapes.extraLarge // Very rounded corners for the error icon card
            ) {
                Icon( // Using an Icon instead of just emoji for better integration
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = colorScheme.onErrorContainer, // Use Material 3 on error container color for icon
                    modifier = Modifier
                        .padding(20.dp) // Generous padding
                        .size(60.dp) // Larger icon size
                )
            }

            Text(
                text = "Setup Not Complete",
                style = MaterialTheme.typography.headlineMedium.copy( // More prominent headline
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error // Use error color for title
                ),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceContainerLow // Subtle background for error message
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Increased elevation
                shape = MaterialTheme.shapes.medium // Standard rounded corners
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyLarge.copy( // Larger body text for message
                        fontFamily = FontFamily.Monospace,
                        color = colorScheme.onSurface // Standard onSurface color
                    ),
                    modifier = Modifier.padding(24.dp), // Increased padding
                    textAlign = TextAlign.Center
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing between buttons
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // Fixed height for consistent button size
                ) {
                    Text(
                        text = "Try Again",
                        style = MaterialTheme.typography.titleMedium.copy( // Larger button text
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // Fixed height for consistent button size
                ) {
                    Text(
                        text = "Open Settings",
                        style = MaterialTheme.typography.titleMedium.copy( // Larger button text
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}