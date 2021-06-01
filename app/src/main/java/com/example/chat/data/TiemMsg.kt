package com.example.chat.data

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

data class TimeMsg(
    val contactName: String,
    val type: Int,
    val content: String,
    val date: String = getDate()
) {
    companion object {
        @SuppressLint("SimpleDateFormat")
        fun getDate(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss:sss").format(Date())
        }
    }
}