package com.example.chat.chatUtil

import android.content.Context
import com.example.chat.MyApplication
import com.example.chat.MyDBHelper

object DBUtil {
    public val db = MyDBHelper(MyApplication.context).writableDatabase

    fun setAvatar(name: String){
        val username = MyApplication.context.getSharedPreferences("data", Context.MODE_PRIVATE).getString("username", "")
        db.execSQL("update User set avatar = ? where username = ?", arrayOf(name,username))
    }

    fun close(){
        db.close()
    }
}