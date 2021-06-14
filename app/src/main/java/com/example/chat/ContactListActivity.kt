package com.example.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.*
import com.example.chat.data.Contact
import kotlinx.android.synthetic.main.activity_contact_list.*
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class ContactListActivity : MyActivity() {
    private val contactList = ArrayList<Contact>()

    companion object {
        const val updateRecyclerView = 1
    }

    //更新联系人列表
    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateRecyclerView -> {
                    initContact()
                    MyData.initMsgMap()     //更新联系人的历史消息
                }
            }
        }
    }
    private val contactListMessenger = Messenger(handler)  //发送到MyService的信使

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_list)
        setSupportActionBar(contactToolbar)
        //toolbar设置头像
        if (MyData.myAvatarUri.toString() != "")
            ImageUtil.getBitmapFromUri(MyData.myAvatarUri)?.let {
                contactListToolbarImageView.setImageBitmap(it)
            }
        //初始化已保存的联系人列表
        MyData.initSavedContact()
        //初始化服务器Url对应的本地Uri
        MyData.initUrlToUri()
        //开启搜索用户的服务
        val serviceIntent = Intent(this, MyService::class.java)
        serviceIntent.putExtra("contactListMessenger", contactListMessenger)
        startService(serviceIntent)
    }

    //创建menu菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.contact_list_menu, menu)
        return true
    }

    //menu菜单添加响应
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.setAvatar -> {    //打开相册,选择要作为头像的图片
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, 2)
            }
            R.id.clear -> {
                DBUtil.DB.execSQL("delete from msg")
            }
            R.id.searchIP -> {//开启搜索局域网用户的服务
                val serviceIntent = Intent(this, MyService::class.java)
                serviceIntent.putExtra("contactListMessenger", contactListMessenger)
                startService(serviceIntent)
            }
            R.id.logout -> {
                with(Intent(this, LoginActivity::class.java)) {
                    this.putExtra("NoAutoLogin", "NoAutoLogin")
                    startActivity(this)
                    finish()
                }
            }
            R.id.test -> {
                DBUtil.DB.execSQL("DELETE FROM urlToUri")
            }
        }
        return true
    }

    //打开其他activity后回调结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            2 -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let {
                        LogUtil.d(tag, "选取的照片Uri:$it")
                        ImageUtil.getBitmapFromUri(it)?.let { bitmap ->
                            //标题栏设置头像，随机生成保存头像的文件名
                            contactListToolbarImageView.setImageBitmap(bitmap)
                            val name = ImageUtil.getName()
                            //保存图片到本app文件夹下
                            ImageUtil.saveBitmapToPicture(bitmap, name)?.let { saveUri ->
                                DBUtil.setAvatarUser(MyData.username, saveUri, name)
                            }
                        }
                    }
                }
            }
        }
    }

    //更新联系人列表
    private fun initContact() {
        LogUtil.d(tag, "更新联系人列表")
        contactList.clear()     //清空以往保存的联系人信息
        with(contactList) {
            //先添加历史聊天对象，再添加新发现的聊天对象
            MyData.savedContact.forEach {
                if (MyData.onlineContact.containsKey(it.key)) {
                    add(MyData.onlineContact[it.key]!!)
                } else
                    add(it.value)
            }
            MyData.onlineContact.forEach {
                if (!MyData.savedContact.containsKey(it.key))
                    add(it.value)
            }
            sortBy { contact -> !contact.isOnline }     //根据是否在线排序
        }
        contactListRecycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        contactListRecycleView.layoutManager = LinearLayoutManager(this)
        contactListRecycleView.adapter = ContactListAdapter(contactList)
    }
}