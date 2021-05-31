package com.example.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.*
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import com.example.chat.data.TimeMsg
import kotlinx.android.synthetic.main.activity_contact_list.*
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
        ImageUtil.getBitmapFromUri(MyData.myImageUri)?.let {
            contactListToolbarImageView.setImageBitmap(it)
        }
        //初始化已保存的联系人列表
        MyData.initSavedContact()
        //动态申请权限
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 1
        )
        //开启搜索局域网用户的服务
        val serviceIntent = Intent(this, MyService::class.java)
        serviceIntent.putExtra("contactListMessenger", contactListMessenger)
        startService(serviceIntent)
        //添加部分测试操作
        //test()
    }

    private fun test() {
        thread {
            for (i in 1..5) {
                DBUtil.DB.execSQL(
                    "insert into msg(username,contactName,type,content,date) values (?,?,?,?,?)",
                    arrayOf(    //SimpleDateFormat("YYYY-MM-DD HH:MM:SS").format(Date())
                        MyData.username, MyData.username, Msg.TYPE_RECEIVED, i.toString(), TimeMsg.getDate()
                    )
                )
                Thread.sleep(2000)
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    //动态申请权限的回调方法
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Permission GET", Toast.LENGTH_SHORT).show()
        } else {    //获取权限失败
            Toast.makeText(this, "请授予必要权限...", Toast.LENGTH_SHORT).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
            R.id.sendMessage -> {
                DBUtil.DB.execSQL("delete from msg")
            }
            R.id.searchIP -> {//开启搜索局域网用户的服务
                val serviceIntent = Intent(this, MyService::class.java)
                serviceIntent.putExtra("contactListMessenger", contactListMessenger)
                startService(serviceIntent)
            }
            R.id.startMyService -> {    //各种调试
                ImageUtil.getUri("641").let {
                    ImageUtil.getBitmapFromUri(it)?.let { bitmap ->
                        contactListToolbarImageView.setImageBitmap(bitmap)
                    }
                }
            }
            R.id.contactTest -> {
                //打开聊天界面,传递联系人数据
                val contact = Contact("test", "127.0.0.1", "")//拿到联系人数据
                val intent = Intent(this, ContactActivity::class.java)
                intent.putExtra("contact", contact)
                startActivity(intent)
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
            MyData.accessContact.forEach() {
                add(it.value)
            }
        }
        val layoutManager = LinearLayoutManager(this)
        val adapter = ContactListAdapter(contactList)
        contactListRecycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        contactListRecycleView.layoutManager = layoutManager
        contactListRecycleView.adapter = adapter
    }
}