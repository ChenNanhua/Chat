package com.example.chat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.chat.chatUtil.LogUtil

class MyService : Service() {
    private val tag = "MyService"
    private lateinit var localNet:LocalNet
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.d(tag,"onCreate")
        localNet = LocalNet()
        localNet.getMessage()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.d(tag,"onStartCommand")
        //localNet.searchLocal(handler) 未找到传递handler方法
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(tag,"onDestroy")
        localNet.job.cancel()
    }
}
