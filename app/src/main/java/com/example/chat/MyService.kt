package com.example.chat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class MyService : Service() {
    private val tag = "MyService"
    private lateinit var localNet:LocalNet
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag,"onCreate")
        localNet = LocalNet()
        localNet.getMessage()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag,"onStartCommand")
        localNet.searchLocal()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag,"onDestroy")
        localNet.job.cancel()
    }
}
