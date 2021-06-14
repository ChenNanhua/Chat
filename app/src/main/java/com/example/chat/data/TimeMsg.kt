package com.example.chat.data

import com.example.chat.chatUtil.DateUtil

data class TimeMsg(
    val contactName: String,
    val type: Int,
    var content: String,
    val date: String = DateUtil.getDate()
)
