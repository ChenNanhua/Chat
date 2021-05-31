package com.example.chat

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.chat.chatUtil.*
import kotlinx.android.synthetic.main.activity_register.*
import java.lang.Exception

class RegisterActivity : MyActivity(), View.OnClickListener {
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
                    val passwordMd5 = HashUtil.md5(password)
                    val db = DBUtil.DB
                    try {
                        db.execSQL(
                            "insert into user(username,passwordMd5,remember,avatarUri) values (?,?,?,?)",
                            arrayOf(username, passwordMd5, 0, "")
                        )
                        //返回数据给MainActivity
                        val intent = Intent()
                        intent.putExtra("username", username)
                        intent.putExtra("password", password)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } catch (e: SQLiteConstraintException) {
                        LogUtil.w(tag,"注册时用户名重复${e}")
                        Toast.makeText(this, "用户名已被注册", Toast.LENGTH_SHORT).show()
                        editName.setText("")
                        editPassword.setText("")
                        editPasswordRepeat.setText("")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            R.id.buttonQuit -> {     //退出
                finish()
            }
        }
    }
}