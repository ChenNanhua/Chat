package com.example.chat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDBHelper(context: Context, name: String="chat.db", version: Int=1) :
    SQLiteOpenHelper(context, name, null, version) {
     private val createUser = "create table User("+
             "username text unique,"+
             "passwordMd5 text,"+
             "remember integer)"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createUser)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("drop table if exists User")
        onCreate(db)
    }
}