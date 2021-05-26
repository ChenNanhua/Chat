package com.example.chat.chatUtil

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.chat.MyApplication
import com.example.chat.MyDBHelper

object DBUtil {
    val DB: SQLiteDatabase = MyDBHelper(MyApplication.context).writableDatabase

    fun setAvatar(name: String){
        val username = MyApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE).getString("username", "")
        DB.execSQL("update User set avatar = ? where username = ?", arrayOf(name,username))
    }

    fun close(){
        DB.close()
    }
}