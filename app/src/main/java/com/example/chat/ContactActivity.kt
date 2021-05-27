package com.example.chat

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.LogUtil
import kotlinx.android.synthetic.main.activity_contact.*

class ContactActivity : AppCompatActivity(), View.OnClickListener {
    private val tag = "ContactActivity"
    private var adapter: MsgAdapter? = null
    private val msgList = ArrayList<Msg>()
    private var myUri = Uri.parse("")
    private var myName = android.os.Build.MODEL
    private lateinit var imageUri: Uri
    private val localNet = LocalNet()           //局域网通信
    private lateinit var contact: Contact       //聊天对象的信息

    companion object Instance {
        val contentMap = HashMap<String, ArrayList<String>>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        //获取聊天对象contact信息
        contact = intent.getSerializableExtra("contact") as Contact
        imageUri = Uri.parse(contact.imageUriString)
        LogUtil.d(tag, "聊天的对象: $contact")
        initMsg()
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
                    localNet.sendMessage(myName,content, contact.IP)
                }
                //尝试在此处更新界面
                if (!contentMap.containsKey(contact.name))
                    contentMap[contact.name] = ArrayList()
                if (contentMap.size > 0) {
                    //依次取出消息显示在屏幕上
                    for (contentItem in contentMap[contact.name]!!) {
                        val msg = Msg(contentItem, Msg.TYPE_RECEIVED, imageUri)
                        msgList.add(msg)
                        adapter?.notifyItemInserted(msgList.size - 1)
                        //将RecyclerView定位到最后一行
                        contactRecycleView.scrollToPosition(msgList.size - 1)
                    }
                    contentMap.clear()
                }
            }
        }
    }

    //初始化要展示的数据
    private fun initMsg() {
        for (i in (0..1)) {
            val msg1 = Msg("hello$i", Msg.TYPE_SENT, myUri)
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
                myUri
            )
            msgList.add(msg1)
            msgList.add(msg2)
            msgList.add(msg3)
            msgList.add(msg4)
        }
    }
}