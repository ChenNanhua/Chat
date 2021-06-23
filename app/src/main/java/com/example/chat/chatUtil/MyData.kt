package com.example.chat.chatUtil

import android.graphics.Bitmap
import android.net.Uri
import com.example.chat.chatUtil.TinyUtil.loge
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import com.example.chat.data.TimeMsg
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

object MyData {
    private val DB = DBUtil.DB

    //个人信息
    var username = ""   //我的名字
    var myAvatarUri: Uri = Uri.parse("")
    var myBitmap: Bitmap = ImageUtil.getBitmapFromResource()    //我的头像bitmap
    var contactBitmap: Bitmap = ImageUtil.getBitmapFromResource()    //聊天对象bitmap
    var msgTime = HashMap<String, Date>()                //保存聊天对象上一条聊天的时间戳

    //所有聊天消息集合
    private val msgMap = HashMap<String, ArrayList<Msg>>()  //保存所有聊天对象的历史聊天记录

    //带时间的聊天信息，用于插入到数据库
    val tempTimeMsgMap = HashMap<String, ArrayList<TimeMsg>>()  //保存新的、待展示到界面上的聊天记录

    //判断聊天对象
    var tempMsgMapName = ""

    //判断是否需要更新redCircle
    var redCircle = false

    //保存查询到的所有打开对应port的用户IP,Uri
    @Volatile
    var onlineContact: HashMap<String, Contact> = HashMap()

    //已保存过的联系人
    val savedContact: HashMap<String, Contact> = HashMap()

    //保存在线的联系人
    val contactList = ArrayList<Contact>()

    //服务器Url对应的本地Uri
    val urlToUri: HashMap<String, String> = HashMap()

    //初始化savedContact
    fun initSavedContact() {
        thread {
            DB.rawQuery("select * from contact", arrayOf()).use {
                if (it.moveToFirst()) {
                    do {
                        with(it) {
                            //姓名下标0，Uri下标1,
                            savedContact[getString(0)] =
                                Contact(getString(0), "0.0.0.0", getString(1), false, getInt(3) == 1)
                        }
                    } while (it.moveToNext())
                }
            }
            "savedContact：$savedContact".loge()
        }
    }

    fun initUrlToUri() {
        thread {
            DB.rawQuery("select * from urlToUri", arrayOf()).use {
                if (it.moveToFirst()) {
                    do {
                        with(it) {
                            //Url下标0，Uri下标1,
                            urlToUri[getString(0)] = getString(1)
                        }
                    } while (it.moveToNext())
                }
            }
        }
    }

    //初始化msgMap
    fun initMsgMap() {
        thread {    //在子线程中执行避免界面卡顿
            msgMap.clear()
            DB.rawQuery(    //获取该用户所有的联系人
                "select contactName from msg where username =? group by contactName",
                arrayOf(username)
            ).use { contactNames ->
                //遍历所有曾经有联系的用户
                if (contactNames.moveToFirst()) {
                    do {
                        val contactName = contactNames.getString(0)
                        //初始化通信用户的历史消息
                        msgMap[contactName] = ArrayList()
                        DB.rawQuery(    //获取联系人的聊天记录
                            "select content,type from msg where username=? and contactName = ? order by date limit 0,50",
                            arrayOf(username, contactName)
                        ).use {
                            if (it.moveToFirst()) {
                                do {
                                    msgMap[contactName]!!.add(
                                        Msg(
                                            it.getString(0),   //content
                                            it.getString(1).toInt()      //type
                                        )
                                    )
                                } while (it.moveToNext())
                            }
                        }
                    } while (contactNames.moveToNext())
                }
            }
            initTempTimeMsg()
        }
    }

    private fun initTempTimeMsg() {
        onlineContact.forEach { (key) ->
            getTempMsgList(key)
        }
    }

    //获取聊天对象的历史聊天记录
    fun getMsgList(contactName: String): ArrayList<Msg> {
        if (!msgMap.containsKey(contactName))
            msgMap[contactName] = ArrayList()
        return msgMap[contactName]!!
    }

    //获取待展示在界面上的聊天信息
    fun getTempMsgList(contactName: String): ArrayList<TimeMsg> {
        if (!tempTimeMsgMap.containsKey(contactName))
            tempTimeMsgMap[contactName] = ArrayList()
        return tempTimeMsgMap[contactName]!!
    }
}