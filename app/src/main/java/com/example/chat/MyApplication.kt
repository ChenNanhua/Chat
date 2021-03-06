package com.example.chat

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.facebook.stetho.Stetho
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


class MyApplication : Application() {
    companion object {
        @SuppressLint("StaticFiledLeak")
        lateinit var context: Context
        var useLocal = false
        val client = with(OkHttpClient.Builder()) {
            connectTimeout(25, TimeUnit.SECONDS)
            readTimeout(25, TimeUnit.SECONDS)
            writeTimeout(25, TimeUnit.SECONDS)
            connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        }.build()
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