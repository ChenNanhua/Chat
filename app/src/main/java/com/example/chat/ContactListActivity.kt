package com.example.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.*
import com.example.chat.chatUtil.TinyUtil.toast
import kotlinx.android.synthetic.main.activity_contact_list.*
import kotlinx.android.synthetic.main.activity_contact_list.contactToolbar
import kotlin.collections.ArrayList


class ContactListActivity : MyActivity(), View.OnClickListener {
    companion object {
        const val updateRecyclerView = 1
        const val updateRedCircle = 2
    }

    val adapter: ContactListAdapter = ContactListAdapter(MyData.contactList)
    private lateinit var testUri: Uri

    //更新联系人列表
    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateRecyclerView -> {
                    initContact()
                    swipeRefresh.isRefreshing = false   //刷新完成
                    MyData.initMsgMap()     //更新联系人的历史消息
                }
                updateRedCircle -> {
                    for (index in msg.obj as ArrayList<*>)
                        adapter.notifyItemChanged(index as Int)
                    contactListRecycleView.scrollToPosition(MyData.contactList.size - 1)
                }
            }
        }
    }
    private val contactListMessenger = Messenger(handler)  //发送到MyService的信使

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_list)
        setSupportActionBar(contactToolbar)

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

        //nav的menu添加点击响应,统一放到onOptionsItemSelected
        nav.setNavigationItemSelectedListener {
            onOptionsItemSelected(it)
            false
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

        //更新红点
        MyData.redCircle = true
        NetUtil.updateRedCircle(contactListMessenger)
    }

    override fun onDestroy() {
        super.onDestroy()
        MyData.redCircle = false
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
                with(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)){
                    type = "image/*"
                    setAvatar.launch(this)
                }
            }
        }
    }

    //创建menu菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.contact_list_menu, menu)
        return true
    }

    //menu菜单添加响应
    @SuppressLint("InflateParams")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //toolbar左侧导航按钮
            android.R.id.home -> drawerLayout.openDrawer(GravityCompat.START)

            //nav修改名字
            R.id.useLocal -> {
                if (MyApplication.useLocal) {
                    MyApplication.useLocal = false
                    nav.menu.findItem(R.id.useLocal).title = "已关闭局域网"
                    "已关闭局域网".toast()
                } else {
                    MyApplication.useLocal = true
                    startService()
                    nav.menu.findItem(R.id.useLocal).title = "已开启局域网"
                    "已开启局域网".toast()
                }
            }
            /*with(AlertDialog.Builder(this)) {
                setTitle("输入新名字")
                setMessage("你将会成为另一个人")
                val view = LayoutInflater.from(MyApplication.context).inflate(R.layout.edit_text, null)
                val editText = view.findViewById<EditText>(R.id.editText)
                setView(view)
                setCancelable(true)
                setPositiveButton("是") { _, _ ->
                    val newName = editText.text.toString()
                    "修改为：${editText.text}".toast()
                    DBUtil.DB.execSQL(
                        "update user set username=? where username = ?",
                        arrayOf(newName, MyData.username)
                    )
                    MyData.username = newName
                    //重启Activity
                    finish()
                    startActivity(Intent(MyApplication.context, ContactListActivity::class.java))
                }
                setNegativeButton("否") { _, _ -> }
            }.show()*/

            R.id.setAvatar -> {    //打开相册,选择要作为头像的图片
                with(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)){
                    type = "image/*"
                    setAvatar.launch(this)
                }
            }

            //删除所有聊天数据
            R.id.clear ->
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
                        finish()
                    }
                    setNegativeButton("否") { _, _ -> }
                }.show()

            //开启搜索用户的服务
            R.id.searchContact -> {
                startService()
            }

            R.id.logout -> {
                with(Intent(this, LoginActivity::class.java)) {
                    this.putExtra("NoAutoLogin", "NoAutoLogin")
                    finish()
                    startActivity(this)
                }
            }

            R.id.test -> {
                contactListToolbarImageView.setImageBitmap(ImageUtil.getBitmapFromUri(testUri))
            }
        }
        return true
    }

    //打开其他相册处理选择的Uri
    private val setAvatar = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data!!.data?.let {
                LogUtil.d(tag, "选取的照片Uri:$it")
                val uri = ImageUtil.getExternalUriFromUri(it)
                ImageUtil.getBitmapFromUri(uri)?.let { bitmap ->
                    //标题栏设置头像，保存头像Uri
                    contactListToolbarImageView.setImageBitmap(bitmap)
                    DBUtil.setAvatarUser(
                        MyData.username, uri, ""
                    )
                }
            }
        }
    }

    //更新联系人列表
    private fun initContact() {
        LogUtil.d(tag, "更新联系人列表")
        MyData.contactList.clear()     //清空以往保存的联系人信息
        with(MyData.contactList) {
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
        contactListRecycleView.adapter = adapter
    }
}