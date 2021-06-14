package com.example.chat.test

import com.example.chat.data.Contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

fun main() {
    httpTest()
}

fun httpTest() {
    val client = with(OkHttpClient.Builder()) {
        connectTimeout(5, TimeUnit.SECONDS)
        readTimeout(5, TimeUnit.SECONDS)
        writeTimeout(5, TimeUnit.SECONDS)
    }.build()
    val request =
        Request.Builder().url("http://localhost:8080/androidImage/ee931cb4-340e-44bb-89cb-b1879c3cfef9.jpg").build()
    val start = System.currentTimeMillis()
    runBlocking {
            launch {
                withContext(IO) {
                for (i in 1..5) {
                    //client.newCall(request).execute().body?.string()
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        println("Success$i")
                        println(System.currentTimeMillis())
                        //TODO("Not yet implemented")
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println("Failure")
                        //TODO("Not yet implemented")
                    }
                })
                }
            }
        }
    }
    println(System.currentTimeMillis()-start)
    //println("start： ${start}")
}

fun threadTest() {
    println("开始")
    Thread {
        println("服务器")
        val serverSocket = ServerSocket(8090)
        val socket = serverSocket.accept()
        println("服务器收到连接")
        val get = BufferedReader(InputStreamReader(socket.getInputStream()))
        // val send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
        println(get.readLine())
        val dataInputStream = DataInputStream(socket.getInputStream())
        println(dataInputStream.readInt())
        println(dataInputStream.readUTF())
    }.start()

    Thread {
        println("客户端")
        Thread.sleep(100)
        val socket = Socket("127.0.0.1", 8090)
        //val get = BufferedReader(InputStreamReader(socket.getInputStream()))
        val send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
        send.println("test")
        val dataOutputStream = DataOutputStream(socket.getOutputStream())
        dataOutputStream.writeInt(1)
        dataOutputStream.flush()
        dataOutputStream.writeUTF("测试测试测试")
    }.start()
    println("正在等待")
    Thread.sleep(1000)
}