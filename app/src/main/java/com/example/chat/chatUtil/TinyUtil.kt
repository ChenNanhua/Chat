package com.example.chat.chatUtil

import android.content.Context
import android.widget.Toast
import com.example.chat.MyApplication

object TinyUtil {
    //Toast.makeText
    fun Any.toast(context: Context = MyApplication.context, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, this.toString(), duration).show()
    }
    //Log.e 排错所用
    fun Any.loge() {
        LogUtil.e("排错中...", this.toString())
    }
}