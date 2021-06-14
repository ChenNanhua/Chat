package com.example.chat.chatUtil

import android.net.Uri
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import com.example.chat.data.TimeMsg
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

object MyData {
    private val DB = DBUtil.DB

    //个人信息
    var username = ""
    var myAvatarUri: Uri = Uri.parse("")

    //聊天消息
    private val msgMap = HashMap<String, ArrayList<Msg>>()  //保存所有聊天对象的历史聊天记录
    //带时间的聊天信息，用于插入到数据库
    private val tempTimeMsgMap = HashMap<String, ArrayList<TimeMsg>>()  //保存新的、待展示到界面上的聊天记录
    //判断聊天对象
    var tempMsgMapName = ""

    //保存查询到的所有打开对应port的用户IP,Uri
    @Volatile
    var onlineContact: HashMap<String, Contact> = HashMap()

    //已保存过的联系人
    val savedContact: HashMap<String, Contact> = HashMap()
    //服务器Url对应的本地Uri
    val urlToUri:HashMap<String,String> = HashMap()

    //初始化savedContact
    fun initSavedContact() {
        thread {
            DB.rawQuery("select * from contact", arrayOf()).use {
                if (it.moveToFirst()) {
                    do {
                        with(it) {
                            //姓名下标0，Uri下标1,
                            savedContact[getString(0)] = Contact(getString(0), "0.0.0.0", getString(1))
                        }
                    } while (it.moveToNext())
                }
            }
        }
    }

    fun initUrlToUri(){
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
            //Thread.sleep(200)   //延时一会，防止savedContact未初始化完成
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
                                            it.getString(1).toInt(),      //type
                                            with(it.getString(1).toInt()) {     //返回消息对应的图片Uri
                                                when (this) {   //根据不同的类型设置不同的头像uri
                                                    Msg.TYPE_SENT -> myAvatarUri
                                                    Msg.TYPE_IMAGE_SENT -> myAvatarUri
                                                    Msg.TYPE_RECEIVED -> Uri.parse(savedContact[contactName]!!.avatarUri)
                                                    Msg.TYPE_IMAGE_RECEIVED -> Uri.parse(
                                                        savedContact[contactName]!!.avatarUri
                                                    )
                                                    else -> myAvatarUri
                                                }
                                            }
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

    //保存待展示在界面上的聊天信息
    fun getTempMsgList(contactName: String): ArrayList<TimeMsg> {
        if (!tempTimeMsgMap.containsKey(contactName))
            tempTimeMsgMap[contactName] = ArrayList()
        return tempTimeMsgMap[contactName]!!
    }
}