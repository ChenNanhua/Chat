package com.example.chat.chatUtil

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    @SuppressLint("SimpleDateFormat")
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:sss")

    //格式化日期
    fun getDate(date: Date = Date()): String {
        return format.format(date)
    }

    //格式化的日期转回Date
    fun parseDate(dateString: String): Date {
        return format.parse(dateString)!!
    }

    //格式化日期减一秒,让它在数据库中排前面
    fun minusSec(dateString: String): String {
        val date = format.parse(dateString)!!
        return format.format(Date(date.time - 1000 * 1))
    }

    //去除毫秒数
    fun noMillSec(dateString: String): String {
        return with(dateString.split(":").toMutableList()) {
            this.removeAt(this.size - 1)        //去除最后的毫秒数
            this.joinToString(":")
        }
    }
}