package com.example.chat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import com.example.chat.chatUtil.DBUtil
import com.example.chat.chatUtil.NetUtil
import com.example.chat.chatUtil.LogUtil
import com.example.chat.chatUtil.TinyUtil.toast

class MyService : Service() {
    private val tag = "MyService"
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.d(tag, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        LogUtil.d(tag, "onStartCommand")
        "正在搜索联系人".toast()
        val messenger = intent?.extras?.get("contactListMessenger") as Messenger
        NetUtil.startServerLocal(messenger)
        NetUtil.searchLocal(messenger)
        NetUtil.searchInternet(messenger)
        NetUtil.getMessageInternet()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(tag, "onDestroy")
        LogUtil.w(tag, "MyService被关闭")
        NetUtil.logoutInternet()
        DBUtil.close()
        MyApplication.job.cancel()
        NetUtil.serverSocket.close()
    }
}
