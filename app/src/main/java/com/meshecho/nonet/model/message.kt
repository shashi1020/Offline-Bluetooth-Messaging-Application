package com.meshecho.nonet.model

import java.util.Date

data class Message(
    val sender: String,
    val content: String,
    val timestamp: Date
)
