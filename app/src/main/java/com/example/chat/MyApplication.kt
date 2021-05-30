package com.example.chat

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.facebook.stetho.Stetho
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job


class MyApplication : Application() {
    companion object {
        @SuppressLint("StaticFiledLeak")
        lateinit var context: Context
        val job = Job()
        val scope = CoroutineScope(job)
    }

    //获取全局context
    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)    //查看sqlite插件
        context = applicationContext
    }
}