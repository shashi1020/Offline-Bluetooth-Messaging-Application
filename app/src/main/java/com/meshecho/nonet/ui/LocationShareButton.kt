package com.meshecho.nonet.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun LocationShareButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.LocationOn, contentDescription = "Send Location")
    }
}