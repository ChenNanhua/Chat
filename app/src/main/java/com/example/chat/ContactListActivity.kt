package com.example.chat

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.*
import com.example.chat.chatUtil.TinyUtil.toast
import com.example.chat.data.Contact
import kotlinx.android.synthetic.main.activity_contact_list.*
import kotlinx.android.synthetic.main.activity_contact_list.contactToolbar
import kotlin.collections.ArrayList


class ContactListActivity : MyActivity(), View.OnClickListener {
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
                    swipeRefresh.isRefreshing = false   //刷新完成
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
        //nav设置点击监听,点击后退出nav
        nav.setNavigationItemSelectedListener {
            drawerLayout.closeDrawers()
            true
        }
        //获取nav的header,并设置属性
        val navHeader = nav.getHeaderView(0)
        val navAvatar: ImageView = navHeader.findViewById(R.id.navAvatar)
        val navName: TextView = navHeader.findViewById(R.id.navName)
        //toolbar设置头像，姓名，nav设置头像，姓名
        contactListToolbarName.text = MyData.username
        navName.text = MyData.username
        if (MyData.myAvatarUri.toString() != "")
            ImageUtil.getBitmapFromUri(MyData.myAvatarUri)?.let {
                contactListToolbarImageView.setImageBitmap(it)
                navAvatar.setImageBitmap(it)
            }
        else {
            contactListToolbarImageView.setImageResource(R.drawable.none)
            navAvatar.setImageResource(R.drawable.none)
        }
        //toolbar设置最左侧导航
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_dehaze_24)
        }
        //SwipeRefreshLayout的刷新逻辑
        swipeRefresh.setColorSchemeResources(R.color.colorAccent)
        swipeRefresh.setOnRefreshListener {
            startService()
        }
        //初始化已保存的联系人列表
        MyData.initSavedContact()
        //初始化服务器Url对应的本地Uri
        MyData.initUrlToUri()
        startService()
    }

    //开启搜索用户的服务
    private fun startService() {
        val serviceIntent = Intent(this, MyService::class.java)
        serviceIntent.putExtra("contactListMessenger", contactListMessenger)
        startService(serviceIntent)
    }

    override fun onClick(v: View?) {
        when (v) {
            contactListToolbarImageView -> {
                //打开相册,选择要作为头像的图片
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, 2)
            }
        }
    }

    //创建menu菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.contact_list_menu, menu)
        return true
    }

    //menu菜单添加响应
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //toolbar左侧导航按钮
            android.R.id.home -> drawerLayout.openDrawer(GravityCompat.START)

            R.id.setAvatar -> {    //打开相册,选择要作为头像的图片
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, 2)
            }

            //删除所有聊天数据
            R.id.clear -> {
                with(AlertDialog.Builder(this)) {
                    setTitle("是否删除所有聊天数据")
                    setMessage("操作不可逆，三思而后行")
                    setCancelable(true)
                    setPositiveButton("是") { _, _ ->
                        with(DBUtil.DB) {
                            execSQL("delete from msg")
                            execSQL("delete from contact")
                            execSQL("delete from urlToUri")
                            execSQL("delete from user")
                            "清除数据完成".toast()
                        }
                    }
                    setNegativeButton("否") { _, _ -> }
                }.show()
            }

            //开启搜索用户的服务
            R.id.searchContact -> {
                startService()
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
                "DELETE FROM urlToUri".toast()
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
        //contactListRecycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        contactListRecycleView.layoutManager = LinearLayoutManager(this)
        contactListRecycleView.adapter = ContactListAdapter(contactList)
    }
}