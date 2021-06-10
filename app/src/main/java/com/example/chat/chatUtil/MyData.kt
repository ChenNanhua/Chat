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
    private val tempTimeMsgMap = HashMap<String, ArrayList<TimeMsg>>()  //保存新的、待展示到界面上的聊天记录
    var tempMsgMapName = ""

    //保存查询到的所有打开对应port的用户IP,Uri
    @Volatile
    var accessContact: HashMap<String, Contact> = HashMap()

    //已保存过的联系人
    val savedContact: HashMap<String, String> = HashMap()

    //初始化savedContact
    fun initSavedContact() {
        thread {
            DB.rawQuery("select * from contact", arrayOf()).use {
                if (it.moveToFirst()) {
                    do {
                        with(it) {
                            //姓名下标0，Uri下标1
                            savedContact[getString(0)] = getString(1)
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
            ).use {
                if (it.moveToFirst()) {
                    do {
                        //可通信的用户才取出历史消息
                        if (accessContact.containsKey(it.getString(0))) {
                            msgMap[it.getString(0)] = ArrayList()
                            DB.rawQuery(    //获取联系人的聊天记录
                                "select content,type from msg where username=? and contactName = ? order by date limit 0,50",
                                arrayOf(username, it.getString(0))
                            ).use { it2 ->
                                if (it2.moveToFirst()) {
                                    do {
                                        msgMap[it.getString(0)]!!.add(
                                            Msg(
                                                it2.getString(0),   //content
                                                it2.getString(1).toInt(),      //type
                                                with(it2.getString(1).toInt()) {     //返回消息对应的图片Uri
                                                    when (this) {   //根据不同的类型设置不同的头像uri
                                                        Msg.TYPE_SENT -> myAvatarUri
                                                        Msg.TYPE_IMAGE_SENT -> myAvatarUri
                                                        Msg.TYPE_RECEIVED -> Uri.parse(accessContact[it.getString(0)]!!.avatarUri)
                                                        Msg.TYPE_IMAGE_RECEIVED -> Uri.parse(
                                                            accessContact[it.getString(0)]!!.avatarUri
                                                        )
                                                        else -> myAvatarUri
                                                    }
                                                }
                                            )
                                        )
                                    } while (it2.moveToNext())
                                }
                            }
                        }
                    } while (it.moveToNext())
                }
            }
            initTempTimeMsg()
        }
    }

    private fun initTempTimeMsg() {
        accessContact.forEach { (key) ->
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