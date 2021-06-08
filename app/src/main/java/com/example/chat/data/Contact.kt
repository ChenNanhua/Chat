package com.example.chat.data

import java.io.Serializable

data class Contact(val name: String, val IP: String, val avatarUri: String, val isLocal: Boolean = true) : Serializable