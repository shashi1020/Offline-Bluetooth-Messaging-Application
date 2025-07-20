package com.example.meshecho.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.meshecho.nonet.model.NavItem


object NavItem {
    val bottomNavItems = listOf(
        NavItem("groups", Icons.Default.Groups, "Groups"),
        NavItem("chats", Icons.Filled.Chat, "chat"),
        NavItem("media", Icons.Default.Photo, "Media"),
        NavItem("settings", Icons.Default.Settings, "Settings")
    )
}
