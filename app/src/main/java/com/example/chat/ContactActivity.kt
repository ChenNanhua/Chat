package com.example.chat

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.LogUtil
import com.example.chat.chatUtil.StorageUtil
import kotlinx.android.synthetic.main.activity_contact.*

class ContactActivity : AppCompatActivity(), View.OnClickListener {
    private val tag = "ContactActivity"
    private val msgList = ArrayList<Msg>()
    private var adapter: MsgAdapter? = null
    private lateinit var contact: Contact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        //获取聊天对象contact
        contact = intent.getSerializableExtra("contact") as Contact
        LogUtil.d(tag, "聊天的对象: $contact")
        initMsg(contact)
        val layoutManager = LinearLayoutManager(this)
        adapter = MsgAdapter(msgList)
        contactRecycleView.adapter = adapter
        contactRecycleView.layoutManager = layoutManager
        contactRecycleView.scrollToPosition(msgList.size-1)
        sendMessage.setOnClickListener(this)
    }

    //点击发送按钮后更新聊天界面
    override fun onClick(v: View?) {
        when (v) {
            sendMessage -> {
                val content = inputText.text.toString()
                if (content.isNotEmpty()) {
                    val msg = Msg(content, Msg.TYPE_SENT, StorageUtil.getUri(contact.imageName))
                    msgList.add(msg)
                    //有新消息时刷新RecyclerView中的显示
                    adapter?.notifyItemInserted(msgList.size - 1)
                    //将RecyclerView定位到最后一行
                    contactRecycleView.scrollToPosition(msgList.size - 1)
                    inputText.setText("")
                }
            }
        }
    }

    //初始化要展示的数据
    private fun initMsg(contact: Contact) {
        var uri = Uri.parse("")
        if (contact.imageName != "")
            uri = StorageUtil.getUri(contact.imageName)
        for (i in (0..1)) {
            val msg1 = Msg("hello$i", Msg.TYPE_SENT, uri)
            val msg2 = Msg(
                "hello$i 123456789123456789123456789123456789123456789123456789123456789123456789" +
                        "123456789123456789123456789123456789123456789123456789123456789123456789",
                Msg.TYPE_RECEIVED,
                uri
            )
            val msg3 = Msg("hello$i", Msg.TYPE_RECEIVED, uri)
            val msg4 = Msg(
                "hello$i 123456789123456789123456789123456789123456789123456789123456789123456789" +
                        "123456789123456789123456789123456789123456789123456789123456789123456789",
                Msg.TYPE_SENT,
                uri
            )
            msgList.add(msg1)
            msgList.add(msg2)
            msgList.add(msg3)
            msgList.add(msg4)
        }
    }
}