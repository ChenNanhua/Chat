package com.example.chat.test

import android.R.attr.port
import kotlinx.coroutines.*
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections.synchronizedSet


fun main(){
    val port = 8080
    var length = 0
    var socket: Socket? = null
    var send: DataOutputStream? = null
    var get: FileInputStream? = null
    try {
        try {
            socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", port), 30 * 1000)
            send = DataOutputStream(socket.getOutputStream())
            get = FileInputStream(File("D:\\temp\\test-out\\grass.png"))
            val sendBytes = ByteArray(1024 * 4)
            while (get.read(sendBytes, 0, sendBytes.size).also { length = it } > 0) {
                send.write(sendBytes, 0, length)
                send.flush()
            }
        } finally {
            send?.close()
            get?.close()
            socket?.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }


}

class test(){
    private val job = Job()
    private val scope = CoroutineScope(job)
    @Volatile
    private var availablePort = synchronizedSet(mutableSetOf<String>("1"))
    @Volatile
    var list = setOf<String>("1")

    fun show(){
        for(i in availablePort)
            println(i)
    }
    @Synchronized
    fun add(add: String){
        availablePort.add(add)
        show()
    }

    fun fun1(add: String){
        scope.launch(Dispatchers.IO){
            for (i in 2..3)
            launch {
                add(i.toString())
            }
        }
    }
}
fun testMet() = runBlocking { // this: CoroutineScope
    val start = System.currentTimeMillis()
    coroutineScope {
        repeat(100_000) {
            //协程方式
            launch {
                print("-$it")
            }
        }
    }
    println()
    val end = System.currentTimeMillis()
    println("协程耗时：${end - start}")
}


fun testMet01() = runBlocking { // this: CoroutineScope
    val start = System.currentTimeMillis()
    coroutineScope {
        repeat(100_000) {
            //线程方式
            Thread {
                print("*->$it")
            }.start()
        }
    }
    println()
    val end = System.currentTimeMillis()
    println("thread耗时：${end - start}")
}