package com.example.chat

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/*控制数据库的创建与更新*/
class MyDBHelper(name: String = "chat.db", version: Int = 1) :
    SQLiteOpenHelper(MyApplication.context, name, null, version) {
    //用户信息数据库
    private val createUser = "create table user(" +
            "username varchar(20) unique," +
            "passwordMd5 char(32)," +
            "remember integer," +
            "avatarUri varchar(70)," +
            "avatarName varchar(60))"
    //联系人信息
    private val createContact = "create table contact(" +
            "contactName varchar(20) unique," +
            "avatarUri varchar(70)," +
            "avatarName varchar(60)," +
            "isLocal varchar(5))"
    //消息记录
    private val createMsg = "create table msg(" +
            "username varchar(20)," +
            "contactName varchar(20)," +
            "type int," +
            "content text," +
            "date Date)"
    //服务器Url对应本地Uri
    private val createUrlToUri= "create table urlToUri("+
            "url varchar(70),"+
            "avatarUri varchar(70))"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.run {
            execSQL(createUser)
            execSQL(createContact)
            execSQL(createMsg)
            execSQL(createUrlToUri)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.run {
            execSQL("drop table if exists user")
            execSQL("drop table if exists contact")
            execSQL("drop table if exists msg")
            execSQL("drop table if exists urlToUri")
        }
        onCreate(db)
    }
}