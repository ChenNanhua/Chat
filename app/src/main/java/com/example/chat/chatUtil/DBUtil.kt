package com.example.chat.chatUtil

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.chat.MyDBHelper
import com.example.chat.data.Contact
import kotlin.concurrent.thread

object DBUtil {
    private const val tag = "DBUtil"
    val DB: SQLiteDatabase = MyDBHelper().writableDatabase

    fun setAvatarUser(username: String, avatarUri: Uri, avatarName: String) {
        thread {
            //插入到本地数据库
            DB.execSQL(
                "update user set avatarUri = ?,avatarName=? where username = ?",
                arrayOf(avatarUri.toString(), avatarName, username)
            )
            MyData.myAvatarUri = avatarUri
            //头像信息发送到服务器
            NetUtil.sendAvatarInternet(avatarUri)
        }
    }

    //更新数据库中的联系人信息
    fun setAvatarContact(contactName: String, avatarUri: String, avatarName: String = "",isLocal: Boolean = true) {
        try {
            DB.execSQL(
                "insert into contact(contactName,avatarUri,avatarName) values(?,?,?)",
                arrayOf(contactName, avatarUri, avatarName)
            )
        } catch (e: Exception) {
            LogUtil.d(tag, "数据库contact插入了重复数据，改为更新数据")
            DB.execSQL(
                "update contact set avatarUri = ?,avatarName = ?,isLocal=? where contactName = ?",
                arrayOf(avatarUri, avatarName,isLocal,contactName)
            )
        }
        MyData.savedContact[contactName] = Contact(contactName, "0.0.0.0", avatarUri)
    }

    fun close() {
        DB.close()
    }
}