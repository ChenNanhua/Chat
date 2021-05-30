package com.example.chat.test

import com.example.chat.chatUtil.DBUtil
import com.example.chat.data.Msg
import java.text.SimpleDateFormat
import java.util.*

fun main() {
    println(SimpleDateFormat.getDateTimeInstance().format(Date(121, 4, 30, 8, 29, 0)))
    println(Date(System.currentTimeMillis() - 1000 * 60))
}

fun insert() {
    DBUtil.DB.execSQL(
        "insert into msg(username,contactName,type,content,date) values (?,?,?,?,?)",
        arrayOf(    //SimpleDateFormat("YYYY-MM-DD HH:MM:SS").format(Date())
            "test", "test", Msg.TYPE_RECEIVED, "添加的测试", SimpleDateFormat.getDateTimeInstance().format(Date())
        )
    )
    DBUtil.DB.execSQL(
        "insert into msg(username,contactName,type,content,date) values (?,?,?,?,?)",
        arrayOf(    //SimpleDateFormat("YYYY-MM-DD HH:MM:SS").format(Date())
            "test", "test", Msg.TYPE_RECEIVED, "添加的测试1", SimpleDateFormat.getDateTimeInstance().format(Date())
        )
    )
}

fun select() {
    DBUtil.DB.rawQuery("select * from msg", arrayOf()).use {
        if (it.moveToFirst())
            do {
                println(it.getString(0))
                println(it.getString(3))
                println(it.getString(4))
            } while (it.moveToNext())
    }
}

fun select2() {
    DBUtil.DB.rawQuery(
        "select * from msg where date > ?",
        arrayOf(SimpleDateFormat.getDateTimeInstance().format(Date(121, 4, 30, 8, 29, 0)))
    ).use {
        if (it.moveToFirst())
            do {
                println(it.getString(0))
                println(it.getString(3))
                println(it.getString(4))
                println("/n")
            } while (it.moveToNext())
    }
}