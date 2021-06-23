package com.example.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.*
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import com.example.chat.data.TimeMsg
import kotlinx.android.synthetic.main.activity_contact.*
import java.lang.Exception
import kotlin.collections.ArrayList

class ContactActivity : MyActivity(), View.OnClickListener {
    private var adapter: ContactAdapter? = null
    private lateinit var contact: Contact               //聊天对象的信息
    private lateinit var msgList: ArrayList<Msg>        //聊天对象的历史聊天记录
    private lateinit var tempMsgList: ArrayList<TimeMsg>    //新的聊天信息，待添加到msgList

    companion object {
        const val updateRecyclerView = 1    //更新消息信号
    }

    //主线程更新聊天信息
    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateRecyclerView -> {
                    //更新界面
                    if (tempMsgList.size > 0) {
                        val positionStart = msgList.size - 1
                        var count = 0
                        val removeTempMsgList = ArrayList<TimeMsg>()
                        //依次取出消息显示在屏幕上
                        for (i in 0 until tempMsgList.size) {
                            with(tempMsgList[i]) {
                                msgList.add(Msg(content, type))
                                count++
                                removeTempMsgList.add(this)
                            }
                        }
                        //刷新新的一行
                        adapter?.notifyItemRangeInserted(positionStart, count)
                        //将RecyclerView定位到最后一行
                        contactRecycleView.scrollToPosition(msgList.size - 1)
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
        LogUtil.d(tag, "聊天的对象: $contact")
        //设置我的bitmap和聊天对象bitmap
        try {
            ImageUtil.getBitmapFromUri(MyData.myAvatarUri)?.let {
                MyData.myBitmap = it
            }
            ImageUtil.getBitmapFromUri(Uri.parse(contact.avatarUri))?.let {
                MyData.contactBitmap = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //聊天消息的获取
        msgList = MyData.getMsgList(contact.name)
        tempMsgList = MyData.getTempMsgList(contact.name)

        //toolbar设置头像和名字
        contactToolbarImageView.setImageBitmap(MyData.contactBitmap)
        contactTitle.text = contact.name

        //聊天消息界面初始化
        contactRecycleView.adapter = ContactAdapter(msgList)
        val linearLayoutManager = LinearLayoutManager(this)
        //设置RecycleView随软键盘向上弹起
        linearLayoutManager.stackFromEnd = true
        contactRecycleView.layoutManager = linearLayoutManager
        contactRecycleView.scrollToPosition(msgList.size - 1)

        //后台更新聊天记录
        MyData.tempMsgMapName = contact.name
        NetUtil.updateTempMsg(contactMessenger)
    }

    override fun onDestroy() {
        MyData.tempMsgMapName = ""
        //重置我的bitmap和聊天对象bitmap
        MyData.myBitmap = ImageUtil.getBitmapFromResource()
        MyData.contactBitmap = ImageUtil.getBitmapFromResource()
        super.onDestroy()
    }

    //点击发送按钮后更新聊天界面
    override fun onClick(v: View?) {
        when (v) {
            sendMessage -> {    //发送文字消息
                val content = inputText.text.toString()
                if (content.isEmpty())
                    return
                inputText.setText("")
                if (contact.isLocal)
                    NetUtil.sendMessageLocal(content, contact.name, contact.IP)
                else
                    NetUtil.sendMessageInternet(contact.name, content)
            }
            choseImage -> {     //打开相册,选择要发送的的图片
                with(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)){
                    type = "image/*"
                    sendImage.launch(this)
                }
            }
        }
    }

    //打开相册处理选择的Uri
    private val sendImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data!!.data?.let {
                val uri = ImageUtil.getExternalUriFromUri(it)
                if (contact.isLocal)
                    NetUtil.sendSingleImageLocal(uri, contact.name, contact.IP)
                else
                    NetUtil.sendSingleImageInternet(uri, contact.name)
                LogUtil.d(tag, "选取的照片Uri:$it")
            }
        }
    }
}