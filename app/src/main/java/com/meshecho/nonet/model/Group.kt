package com.meshecho.nonet.model


data class Group(
    val groupId: String,                  // Unique ID for the group (e.g., UUID)
    val groupName: String,                // Display name of the group
    val members: Set<String>,             // Set of peer fingerprints (including self)
    val createdAt: Long = System.currentTimeMillis() // Optional timestamp
)