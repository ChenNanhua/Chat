package com.example.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        val db = MyDBHelper(this).writableDatabase
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //给按钮设置监听事件
        buttonLogin.setOnClickListener(this)
        buttonRegister.setOnClickListener(this)
        buttonQuit.setOnClickListener(this)
        val autoLoginName = getSharedPreferences("data", Context.MODE_PRIVATE).getString("autoLoginName", "")
        if (autoLoginName != "") {      //存在要自动登录的账号，如果有记住密码则可以自动登录
            editName.setText(autoLoginName)
            val cursor =
                db.rawQuery("select * from User where username = ?", arrayOf(autoLoginName))
            if (cursor.count != 0) {
                cursor.moveToFirst()
                val isRemember = cursor.getInt(cursor.getColumnIndex("remember"))
                if (isRemember > 0) {
                    autoLogin.isChecked = true
                    rememberPassword.isChecked = true
                    //todo 登入主界面
                    Toast.makeText(this, "登录成功...", Toast.LENGTH_SHORT).show()
                }
            }
            cursor.close()
        }

    }

    override fun onClick(v: View?) {
        val db = MyDBHelper(this).writableDatabase
        when (v?.id) {
            R.id.buttonLogin -> {    //登录逻辑
                val username = editName.text.toString()     //与数据库中保存的账号信息比对
                val passwordMd5 = Hash.md5(editPassword.text.toString())
                val cursor = db.rawQuery(
                    "select * from User where username = ? and passwordMd5 = ?", arrayOf(username, passwordMd5)
                )
                if (cursor.count == 0) {  //没有匹配账号，登录失败
                    Toast.makeText(this, "账户或密码错误！", Toast.LENGTH_SHORT).show()
                    editName.setText("")
                    editPassword.setText("")
                } else {  //成功登录
                    //todo 成功登录后保存信息到记住密码，自动登录信息，并进入程序主界面
                    Toast.makeText(this, "登录成功...", Toast.LENGTH_SHORT).show()
                    if (rememberPassword.isChecked)      //保存记住密码信息
                        db.execSQL("update User set remember = 1 where username = ?", arrayOf(username))
                    else
                        db.execSQL("update User set remember = 0 where username = ?", arrayOf(username))
                    if (autoLogin.isChecked)       //保存自动登录信息
                        getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("username", username)
                            .apply()
                    else
                        getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("username", "").apply()
                }
                cursor.close()
            }
            R.id.buttonRegister -> {     //注册逻辑
                startActivityForResult(Intent(this, RegisterActivity::class.java), 1)
            }
            R.id.buttonQuit -> {     //退出
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> if (resultCode == Activity.RESULT_OK) {
                editName.setText(data?.getStringExtra("username"))
                editPassword.setText((data?.getStringExtra("password")))
            }
        }
    }

}