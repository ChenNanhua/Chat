package com.example.chat.data

import java.text.SimpleDateFormat
import java.util.*

data class TimeMsg(
    val contactName: String,
    val type: Int,
    val content: String,
    val date: String = getDate()
) {
    companion object{
        fun getDate():String{
            return SimpleDateFormat.getDateTimeInstance().format(Date())
        }
    }
}