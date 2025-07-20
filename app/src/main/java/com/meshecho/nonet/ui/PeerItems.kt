package com.meshecho.nonet.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun PeerItem(
    peerID: String,
    displayName: String,
    signalStrength: Int,
    isSelected: Boolean,
    isFavorite: Boolean,
    hasUnreadDM: Boolean,
    unreadCount: Int,
    colorScheme: ColorScheme,
    onItemClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 12.dp),
        tonalElevation = if (isSelected) 6.dp else 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            // Signal strength indicator
            SignalStrengthBars(signalStrength)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.bodyLarge)
                Text(peerID.take(8), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Unread message badge
            if (hasUnreadDM) {
                BadgedBox(
                    badge = {
                        Badge {
                            Text(unreadCount.toString())
                        }
                    }
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Unread")
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Favorite icon toggle
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFavorite) Color.Red else colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SignalStrengthBars(strength: Int) {
    val filledBars = (strength.coerceIn(0, 5))
    Row {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .height((8 + index * 4).dp)
                    .width(4.dp)
                    .padding(horizontal = 1.dp)
                    .background(
                        if (index < filledBars) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

