package com.meshecho.nonet.model


data class EchoPacket(
    val type: String,         // e.g., "group_message"
    val destination: String,  // peerID
    val payload: String,      // actual message content
    val sender: String,       // sender's peerID
    val groupId: String? = null // used only in group messages
)