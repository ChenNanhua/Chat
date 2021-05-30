package com.example.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chat.chatUtil.LogUtil

open class MyActivity : AppCompatActivity() {
    private val className = this.javaClass.toString()
    val tag = className.substring(className.lastIndexOf(".") + 1, className.length)
    val myApplication = MyApplication()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.d(tag, "启动:$tag")
    }
}