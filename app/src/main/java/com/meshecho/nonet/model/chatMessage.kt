package com.meshecho.nonet.model


import java.util.Date

data class ChatMessage(
    val sender: String,
    val content: String,
    val timestamp: Date,
    val isMine: Boolean
)
