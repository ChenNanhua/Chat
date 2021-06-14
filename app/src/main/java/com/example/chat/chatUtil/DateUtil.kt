package com.example.chat.chatUtil

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    @SuppressLint("SimpleDateFormat")
    fun getDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss:sss").format(Date())
    }
}