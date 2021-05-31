package com.example.chat.chatUtil

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.chat.MyDBHelper

object DBUtil {
    private const val tag = "DBUtil"
    val DB: SQLiteDatabase = MyDBHelper().writableDatabase

    fun setAvatarUser(username: String, avatarUri: Uri, avatarName: String) {
        DB.execSQL(
            "update user set avatarUri = ?,avatarName=? where username = ?",
            arrayOf(avatarUri.toString(), avatarName, username)
        )
        MyData.myImageUri = avatarUri
    }

    fun setAvatarContact(contactName: String, avatarUri: Uri, avatarName: String) {
        try {
            DB.execSQL(
                "insert into contact(contactName,avatarUri,avatarName) values(?,?,?)",
                arrayOf(contactName, avatarUri.toString(), avatarName)
            )
        } catch (e: Exception) {
            LogUtil.d(tag, "数据库contact插入了重复数据")
        }
    }

    fun close() {
        DB.close()
    }
}