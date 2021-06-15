package com.example.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import com.example.chat.chatUtil.DBUtil.DB
import com.example.chat.chatUtil.MyData
import com.example.chat.chatUtil.HashUtil
import com.example.chat.chatUtil.TinyUtil.toast
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : MyActivity(), View.OnClickListener {
    private lateinit var sharedEdit: SharedPreferences.Editor        //写入SharedPreferences
    private lateinit var shared: SharedPreferences                   //读取SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        sharedEdit = getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        sharedEdit.apply()
        shared = getSharedPreferences("data", Context.MODE_PRIVATE)
        //给按钮设置监听事件
        buttonLogin.setOnClickListener(this)
        buttonRegister.setOnClickListener(this)
        buttonQuit.setOnClickListener(this)

        //动态申请权限
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 1
        )

        //判断是否记住密码
        val username: String = shared.getString("username", "")!!
        editName.setText(username)
        DB.rawQuery("select * from User where username = ?", arrayOf(username)).use {
            if (it.count == 1) {
                it.moveToFirst()
                if (it.getInt(it.getColumnIndex("remember")) > 0) {
                    rememberPassword.isChecked = true
                    val password: String = it.getString(it.getColumnIndex("passwordMd5"))
                    editPassword.setText(password)
                }
            }
        }
        //判断是否自动登录
        val autoLoginName = shared.getString("autoLoginName", "")
        if (autoLoginName != "" && autoLoginName == username && intent.getStringExtra("NoAutoLogin") == null) {
            autoLogin.isChecked = true
            login(username)
            "自动登录成功...".toast()
        }
    }

    //动态申请权限的回调方法
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Permission GET", Toast.LENGTH_SHORT).show()
        } else {    //获取权限失败
            "请授予必要权限...".toast()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonLogin -> {    //登录逻辑
                val username = editName.text.toString()     //与数据库中保存的账号信息比对
                val password = editPassword.text.toString()
                val passwordMd5 = HashUtil.md5(password)
                DB.rawQuery("select passwordMd5 from user where username = ?", arrayOf(username)).use {
                    if (it.moveToFirst()) {
                        val sqlPassword = it.getString(0)
                        if (sqlPassword == passwordMd5 || sqlPassword == password) {
                            //成功登录
                            "登录成功...".toast()
                            //保存记住密码信息
                            if (rememberPassword.isChecked)
                                DB.execSQL("update user set remember = 1 where username = ?", arrayOf(username))
                            else
                                DB.execSQL("update user set remember = 0 where username = ?", arrayOf(username))
                            //保存自动登录信息
                            if (autoLogin.isChecked)
                                sharedEdit.putString("autoLoginName", username)
                            else
                                sharedEdit.putString("autoLoginName", "")
                            //保存上次登录的用户名
                            sharedEdit.putString("username", username).apply()
                            login(username)
                        } else {  //没有匹配账号，登录失败
                            "密码错误！".toast()
                            editName.setText("")
                            editPassword.setText("")
                        }
                    } else {
                        "账号错误！".toast()
                        editName.setText("")
                        editPassword.setText("")
                    }
                }
            }
            R.id.buttonRegister -> {     //跳转到注册逻辑
                startActivityForResult(Intent(this, RegisterActivity::class.java), 1)
            }
            R.id.buttonQuit -> {        //退出
                finish()
            }
        }
    }

    //获取RegisterActivity返回的用户名和密码
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> if (resultCode == Activity.RESULT_OK) {
                editName.setText(data?.getStringExtra("username"))
                editPassword.setText((data?.getStringExtra("password")))
            }
        }
    }

    //跳转到联系人界面,并更新登录用户的信息
    private fun login(username: String) {
        MyData.username = username
        DB.rawQuery("select avatarUri from user where username = ?", arrayOf(username)).use {
            if (it.moveToFirst()) {
                val uri = it.getString(0)
                MyData.myAvatarUri = Uri.parse(uri)
            }
        }
        startActivity(Intent(MyApplication.context, ContactListActivity::class.java))
        finish()
    }
}