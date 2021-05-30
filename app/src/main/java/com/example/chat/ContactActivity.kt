package com.example.chat

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.DBUtil
import com.example.chat.chatUtil.LogUtil
import com.example.chat.chatUtil.MyData
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import kotlinx.android.synthetic.main.activity_contact.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ContactActivity : MyActivity(), View.OnClickListener {
    private var adapter: MsgAdapter? = null
    private lateinit var imageUri: Uri          //聊天对象的头像信息
    private val localNet = LocalNet()           //局域网通信
    private lateinit var contact: Contact       //聊天对象的信息
    private lateinit var msgList: ArrayList<Msg> //聊天对象的历史聊天记录
    private lateinit var tempMsgList: ArrayList<Msg>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        //获取聊天对象contact信息
        contact = intent.getSerializableExtra("contact") as Contact
        imageUri = Uri.parse(contact.imageUriString)
        LogUtil.d(tag, "聊天的对象: $contact")

        //聊天消息的获取
        msgList = MyData.getMsgList(contact.name)
        tempMsgList = MyData.getTempMsgList(contact.name)
        //addMsg()     //给Msg添加一些数据

        //聊天界面初始化
        val layoutManager = LinearLayoutManager(this)
        adapter = MsgAdapter(msgList)
        contactRecycleView.adapter = adapter
        contactRecycleView.layoutManager = layoutManager
        contactRecycleView.scrollToPosition(msgList.size - 1)
        sendMessage.setOnClickListener(this)
    }

    //点击发送按钮后更新聊天界面
    override fun onClick(v: View?) {
        when (v) {
            sendMessage -> {
                val content = inputText.text.toString()
                if (content.isNotEmpty()) {
                    val msg = Msg(content, Msg.TYPE_SENT, imageUri)
                    LogUtil.d(tag, "发送消息：$content")
                    msgList.add(msg)
                    //有新消息时刷新RecyclerView中的显示
                    adapter?.notifyItemInserted(msgList.size - 1)
                    //将RecyclerView定位到最后一行
                    contactRecycleView.scrollToPosition(msgList.size - 1)
                    inputText.setText("")
                    localNet.sendMessage(MyData.username, content, contact.IP)
                }
                //尝试在此处更新界面
                if (tempMsgList.size > 0) {
                    //依次取出消息显示在屏幕上
                    for (msg in tempMsgList) {
                        msgList.add(msg)
                        tempMsgList.remove(msg)
                        adapter?.notifyItemInserted(msgList.size - 1)
                        //将RecyclerView定位到最后一行
                        contactRecycleView.scrollToPosition(msgList.size - 1)
                    }
                }
            }
        }
    }

    //初始化要展示的数据
    private fun addMsg() {
        for (i in (0..0)) {
            val msg1 = Msg("hello$i", Msg.TYPE_SENT, MyData.myImageUri)
            val msg2 = Msg(
                "hello$i 123456789123456789123456789123456789123456789123456789123456789123456789" +
                        "123456789123456789123456789123456789123456789123456789123456789123456789",
                Msg.TYPE_RECEIVED,
                imageUri
            )
            val msg3 = Msg("hello$i", Msg.TYPE_RECEIVED, imageUri)
            val msg4 = Msg(
                "hello$i 123456789123456789123456789123456789123456789123456789123456789123456789" +
                        "123456789123456789123456789123456789123456789123456789123456789123456789",
                Msg.TYPE_SENT,
                MyData.myImageUri
            )
            msgList.add(msg1)
            msgList.add(msg2)
            msgList.add(msg3)
            msgList.add(msg4)
        }
        return
        DBUtil.DB.execSQL(
            "insert into msg(username,contactName,type,content,date) values (?,?,?,?,?)",
            arrayOf(    //SimpleDateFormat("YYYY-MM-DD HH:MM:SS").format(Date())
                MyData.username, contact.name, Msg.TYPE_RECEIVED, "添加的测试",SimpleDateFormat.getDateTimeInstance().format(Date())
            )
        )
    }
}