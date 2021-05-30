package com.example.chat.data

import android.net.Uri

data class Msg(val content: String, val type: Int, val imageUri: Uri) {
    companion object {
        const val TYPE_RECEIVED = 0
        const val TYPE_SENT = 1
    }
}