package com.example.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chat.chatUtil.*
import kotlinx.android.synthetic.main.activity_contact.*
import java.io.*
import java.sql.Blob


class ContactActivity : AppCompatActivity() {
    private val contactList = ArrayList<Contact>()
    private val tag = "ContactActivity"
    private val localNet = LocalNet()

    companion object {
        const val updateRecyclerView = 1
    }

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                updateRecyclerView -> initContact(LocalNet.availableIp)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        //搜索局域网用户
        startService(Intent(this, MyService::class.java))
        localNet.searchLocal(handler)
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 1
        )
    }

    //动态申请权限的回调方法
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission GET", Toast.LENGTH_SHORT).show()
        } else {    //Permission Denied
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //创建menu菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.contact_menu, menu)
        return true
    }

    //menu菜单添加响应
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.getMessage -> {    //打开相册,选择要作为头像的图片
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, 2)
            }
            R.id.sendMessage -> localNet.sendMessage(LocalNet.availableIp.elementAt(0))
            R.id.searchLocal -> localNet.searchLocal(handler)
            R.id.startMyService -> {    //各种调试
                val uri = StorageUtil.getUri("069fdcff")
                //测试发送原始数据
                val i = DataInputStream(FileInputStream(StorageUtil.getFileDescriptor(uri)))
                val byte = ByteArray(1024 * 4)
                var arr = byteArrayOf()
                var len = 0
                len = i.read(byte)
                while (len > 0) {
                    arr += byte
                    len = i.read(byte)
                }
                val bitmap: Bitmap = BitmapFactory.decodeByteArray(arr, 0, arr.size)
                contactImageView.setImageBitmap(bitmap)
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
                        LogUtil.d(tag, it.toString())
                        val bitmap = StorageUtil.getBitmapFromUri(it)
                        val name = StorageUtil.getName()
                        DBUtil.setAvatar(name)
                        contactImageView.setImageBitmap(bitmap)
                        StorageUtil.saveBitmapToPicture(bitmap, name) //保存图片到本地
                    }
                }
            }
        }
    }

    //更新联系人列表
    private fun initContact(availableIp: MutableSet<String>) {
        Toast.makeText(this, "更新联系人列表", Toast.LENGTH_SHORT).show()
        LogUtil.d(tag, "IP列表长度${availableIp.size}")
        with(contactList) {
            for (Ip in availableIp)
                add(Contact(Ip, R.drawable.none))
        }
        val layoutManager = LinearLayoutManager(this)
        val adapter = ContactAdapter(contactList)
        contactRecycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        contactRecycleView.layoutManager = layoutManager
        contactRecycleView.adapter = adapter
    }
}