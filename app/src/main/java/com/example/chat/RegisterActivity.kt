package com.example.chat

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        buttonRegister.setOnClickListener(this)
        buttonQuit.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonRegister -> {    //注册逻辑
                val username = editName.text.toString()
                val password = editPassword.text.toString()
                val passwordRepeat = editPasswordRepeat.text.toString()
                if (password != passwordRepeat || password == "") {
                    Toast.makeText(this, "两次密码需相同且不为空！", Toast.LENGTH_SHORT).show()
                    editPassword.setText("")
                    editPasswordRepeat.setText("")
                } else {
                    val passwordMd5 = Hash.md5(password)
                    val db = MyDBHelper(this).writableDatabase
                    db.execSQL(
                        "insert into User(username,passwordMd5,remember) values (?,?,?)",
                        arrayOf(username, passwordMd5, 0)
                    )
                    //返回数据给MainActivity
                    val intent = Intent()
                    intent.putExtra("username", username)
                    intent.putExtra("password", password)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
            R.id.buttonQuit -> {     //退出
                finish()
            }
        }
    }
}