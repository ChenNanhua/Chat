package com.example.chat

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.View
import com.example.chat.chatUtil.*
import com.example.chat.chatUtil.TinyUtil.toast
import kotlinx.android.synthetic.main.activity_register.*
import java.lang.Exception

class RegisterActivity : MyActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonRegister -> {    //注册逻辑
                val username = editName.text.toString()
                val password = editPassword.text.toString()
                val passwordRepeat = editPasswordRepeat.text.toString()
                if (password != passwordRepeat || password == "") {
                    "两次密码需相同且不为空！".toast()
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
                        "用户名已被注册".toast()
                        editName.setText("")
                        editPassword.setText("")
                        editPasswordRepeat.setText("")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}