package com.example.chat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import com.example.chat.chatUtil.LogUtil

class MyService : Service() {
    private val tag = "MyService"
    private lateinit var localNet: LocalNet
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.d(tag, "onCreate")
        localNet = LocalNet()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        LogUtil.d(tag, "onStartCommand")
        val messenger = intent?.extras?.get("contactListMessenger") as Messenger
        localNet.startServer(messenger)
        localNet.searchLocal(messenger)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(tag, "onDestroy")
        LogUtil.w(tag, "MyService被关闭")
        MyApplication.job.cancel()
        LocalNet.serverSocket.close()
    }
}
