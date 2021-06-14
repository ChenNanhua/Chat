package com.example.chat.data

import java.io.Serializable

data class Contact(
    val name: String,
    val IP: String,
    var avatarUri: String,
    val isOnline: Boolean = false,
    val isLocal: Boolean = true
) : Serializable