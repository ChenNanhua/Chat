package com.example.chat.test

import com.example.chat.chatUtil.NetUtil
import com.example.chat.data.Contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.net.ServerSocket
import java.net.Socket

fun main() {
    println("开始")
    Thread{
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

    Thread{
        println("客户端")
        Thread.sleep(100)
        val socket = Socket("127.0.0.1",8090)
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

fun httpTest(){
    val client  = OkHttpClient()
    val request = Request.Builder().url("http://localhost:8080/android/info?name=charon&avatarUri=123").build()
    val response = client.newCall(request).execute()
    println(response.message)
    println(response.code)
    val result = response.body?.string()
    println(result)

    val gson = Gson()
    val type = object : TypeToken<List<Contact>>(){}.type
    val contacts:List<Contact> = gson.fromJson(result,type)
    for (contact in contacts){
        println(contact)
    }
}