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
        MyData.myAvatarUri = avatarUri
    }

    //更新数据库中的联系人信息
    fun setAvatarContact(contactName: String, avatarUri: Uri, avatarName: String) {
        try {
            DB.execSQL(
                "insert into contact(contactName,avatarUri,avatarName) values(?,?,?)",
                arrayOf(contactName, avatarUri.toString(), avatarName)
            )
        } catch (e: Exception) {
            LogUtil.d(tag, "数据库contact插入了重复数据")
            DB.execSQL(
                "update contact set avatarUri = ?,avatarName = ? where contactName = ?",
                arrayOf(avatarUri.toString(), avatarName,contactName)
            )
            LogUtil.d(tag, "改为更新数据")
        }
        MyData.savedContact[contactName] = avatarUri.toString()
    }

    fun close() {
        DB.close()
    }
}