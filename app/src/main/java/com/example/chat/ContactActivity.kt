package com.example.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.ImageUtil
import com.example.chat.chatUtil.NetUtil
import com.example.chat.chatUtil.LogUtil
import com.example.chat.chatUtil.MyData
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import com.example.chat.data.TimeMsg
import kotlinx.android.synthetic.main.activity_contact.*
import kotlin.collections.ArrayList

class ContactActivity : MyActivity(), View.OnClickListener {
    private var adapter: ContactAdapter? = null
    private lateinit var contactAvatarUri: Uri          //聊天对象的头像信息
    private lateinit var contact: Contact       //聊天对象的信息
    private lateinit var msgList: ArrayList<Msg> //聊天对象的历史聊天记录
    private lateinit var tempMsgList: ArrayList<TimeMsg>

    //更新消息列表
    companion object {
        const val updateRecyclerView = 1
    }

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateRecyclerView -> {
                    //更新界面
                    if (tempMsgList.size > 0) {
                        val removeTempMsgList = ArrayList<TimeMsg>()
                        //依次取出消息显示在屏幕上
                        for (i in 0 until tempMsgList.size) {
                            with(tempMsgList[i]) {
                                when (this.type) {
                                    Msg.TYPE_SENT -> msgList.add(Msg(this.content, this.type, MyData.myAvatarUri))
                                    Msg.TYPE_RECEIVED -> msgList.add(Msg(this.content, this.type, contactAvatarUri))
                                    Msg.TYPE_IMAGE_RECEIVED -> msgList.add(
                                        Msg(this.content, this.type, contactAvatarUri)
                                    )
                                    Msg.TYPE_IMAGE_SENT -> msgList.add(Msg(this.content, this.type, MyData.myAvatarUri))
                                }
                                removeTempMsgList.add(this)
                            }
                            //刷新新的一行
                            adapter?.notifyItemInserted(msgList.size - 1)
                            //将RecyclerView定位到最后一行
                            contactRecycleView.scrollToPosition(msgList.size - 1)
                        }
                        tempMsgList.removeAll(removeTempMsgList)
                    }
                }
            }
        }
    }
    private val contactMessenger = Messenger(handler)  //信使

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        //获取聊天对象contact信息
        contact = intent.getSerializableExtra("contact") as Contact
        contactAvatarUri = Uri.parse(contact.avatarUri)
        LogUtil.d(tag, "聊天的对象: $contact")

        //聊天消息的获取
        msgList = MyData.getMsgList(contact.name)
        tempMsgList = MyData.getTempMsgList(contact.name)

        //聊天消息界面初始化
        val layoutManager = LinearLayoutManager(this)
        adapter = ContactAdapter(msgList)
        contactRecycleView.adapter = adapter
        contactRecycleView.layoutManager = layoutManager
        contactRecycleView.scrollToPosition(msgList.size - 1)
        sendMessage.setOnClickListener(this)
        choseImage.setOnClickListener(this)

        //toolbar设置头像和名字
        ImageUtil.getBitmapFromUri(contactAvatarUri)?.let {
            contactToolbarImageView.setImageBitmap(it)
        }
        contactTitle.text = contact.name

        //后台更新聊天记录
        MyData.tempMsgMapName = contact.name
        NetUtil.updateTempMsg(contactMessenger)
    }

    override fun onDestroy() {
        MyData.tempMsgMapName = ""
        super.onDestroy()
    }

    //点击发送按钮后更新聊天界面
    override fun onClick(v: View?) {
        when (v) {
            sendMessage -> {
                val content = inputText.text.toString()
                inputText.setText("")
                if (content.isNotEmpty()) {
                    msgList.add(Msg(content, Msg.TYPE_SENT, MyData.myAvatarUri))
                    LogUtil.d(tag, "发送消息：$content")
                    //有新消息时刷新RecyclerView中的显示
                    adapter?.notifyItemInserted(msgList.size - 1)
                    //将RecyclerView定位到最后一行
                    contactRecycleView.scrollToPosition(msgList.size - 1)
                    NetUtil.sendMessageLocal(content, contact.name, contact.IP)
                }
            }
            choseImage -> {     //打开相册,选择要发送的的图片
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, 2)
            }
        }
    }

    //打开其他activity后回调结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            2 -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let {
                        NetUtil.sendSingleImageLocal(it, contact.name, contact.IP, contactMessenger)
                        LogUtil.d(tag, "选取的照片Uri:$it")
                    }
                }
            }
        }
    }
}