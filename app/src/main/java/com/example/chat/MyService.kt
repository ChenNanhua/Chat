package com.example.chat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import com.example.chat.chatUtil.LocalNetUtil
import com.example.chat.chatUtil.LogUtil

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
        val messenger = intent?.extras?.get("contactListMessenger") as Messenger
        LocalNetUtil.startServer(messenger)
        LocalNetUtil.searchLocal(messenger)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(tag, "onDestroy")
        LogUtil.w(tag, "MyService被关闭")
        MyApplication.job.cancel()
        LocalNetUtil.serverSocket.close()
    }
}
