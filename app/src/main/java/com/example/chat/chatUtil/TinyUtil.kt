package com.example.chat.chatUtil

import android.content.Context
import android.widget.Toast
import com.example.chat.MyApplication
import com.example.chat.data.TimeMsg

object TinyUtil {
    //Toast.makeText
    fun Any.toast(context: Context = MyApplication.context, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, this.toString(), duration).show()
    }

    //Log.e 排错所用
    fun Any.loge() {
        LogUtil.e("排错中...", this.toString())
    }

    //添加到TempMsg时也插入到数据库
    fun ArrayList<TimeMsg>.addInsert(timeMsg: TimeMsg) {
        this.add(timeMsg)
        with(timeMsg) {
            DBUtil.DB.execSQL(      //更新到数据库中去
                "insert into msg values(?,?,?,?,?)",
                arrayOf(MyData.username, contactName, type, content, date)
            )
        }
    }
    fun ArrayList<TimeMsg>.addInsert(index:Int,timeMsg: TimeMsg) {
        this.add(index,timeMsg)
        with(timeMsg) {
            DBUtil.DB.execSQL(      //更新到数据库中去
                "insert into msg values(?,?,?,?,?)",
                arrayOf(MyData.username, contactName, type, content, date)
            )
        }
    }
    //添加到TempMsg时也插入到数据库
    fun ArrayList<TimeMsg>.addInsertAll(timeMsgList: List<TimeMsg>) {
        this.addAll(timeMsgList)
        for (timeMsg: TimeMsg in timeMsgList)
            with(timeMsg) {
                DBUtil.DB.execSQL(      //更新到数据库中去
                    "insert into msg values(?,?,?,?,?)",
                    arrayOf(MyData.username, contactName, type, content, date)
                )
            }
    }
}