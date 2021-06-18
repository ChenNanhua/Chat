package com.example.chat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.chat.chatUtil.LogUtil

open class MyActivity : AppCompatActivity() {
    private val className = this.javaClass.toString()
    val tag = className.substring(className.lastIndexOf(".") + 1, className.length)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null)
            super.onCreate(null)
        else
            super.onCreate(savedInstanceState)
        //设置状态栏黑色
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        LogUtil.d(tag, "启动:$tag")
    }
}