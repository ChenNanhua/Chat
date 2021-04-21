package com.example.chat


import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections.*
import kotlin.concurrent.thread


class LocalNet {
    private val tag = "LocalNet"
    private val port = 8080
    val job = Job()
    private val scope = CoroutineScope(job)
    companion object Instance{
        //保存所有打开port的用户IP
        @Volatile var availablePort: MutableSet<String> = synchronizedSet(mutableSetOf<String>())
    }
    //创建服务器，接收传来的数据
    fun getMessage() {  //接收发来的消息
        thread {
            val serverSocket = ServerSocket(port)
            lateinit var socket:Socket
            Log.d(tag, "启动服务器")
            try {
                while (true) {
                    try {
                        socket = serverSocket.accept()
                        val get = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                        println("-----------------准备读取输入")
                        while (true) {
                            val str: String? = get.readLine()
                            if (str == "END") break
                            if (str != null) {
                                Log.d(tag, "服务器收到消息: $str")
                                send.println(str)
                            } else {
                                Log.d(tag, "未接收到消息，正在循环")
                                Thread.sleep(100)
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        socket.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                serverSocket.close()
            }
        }
    }
    //在局域网中发送消息
    fun sendMessage() {
        thread {
            println("sendMessage中遍历端口:${availablePort}")
            val ip = availablePort.elementAt(0)
            try {
                val socket = Socket(ip, 8080)
                val get = BufferedReader(InputStreamReader(socket.getInputStream()))
                val send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                send.println("send消息:test")
                send.println("END")
                val str: String = get.readLine()
                println("客户端收到消息: $str")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    //不停的在局域网中发起连接请求，搜索其他用户
    fun searchLocal() {
        thread{
            val address = getIp()
            Log.d(tag, "客户端保活,IP: $address")
            val addressList: MutableList<String> = address.split(".").toMutableList()
            val host: Int = addressList[addressList.size - 1].toInt()
            val start = System.currentTimeMillis()
            var isBreak = true
            //循环发送连接请求
            while (isBreak) {
                scope.launch(Dispatchers.IO) {
                    //遍历局域网所有主机
                    for (i in 1..255) {
                        launch {
                            //TODO 排除本机
                            if (i > 0) {
                                addressList[addressList.size - 1] = i.toString()
                                val tempAddress = addressList.joinToString(".")
                                println(tempAddress)
                                try {
                                    val socket = Socket()
                                    socket.connect(InetSocketAddress(tempAddress, 8080), 100)
                                    val send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                                    send.println("END")
                                    addAddress(address)
                                } catch (e: Exception) {
                                    //Log.v(tag, e.message.toString()
                                }
                            }
                        }
                    }
                }
                Thread.sleep(1000)
                Log.d(tag, "在本地局域网找到的IP: ${availablePort.toString()}")
                Thread.sleep(19000)
                //发送超过一定时间后退出
                if (System.currentTimeMillis() - start > 20000) {
                    isBreak = false
                    job.cancel()
                }
            }
        }
    }
    //为多线程提供的锁
    @Synchronized
    private fun addAddress(address: String) {
        availablePort.add(address)
    }
    //打印availablePort中的数据
    private fun show(string: String = "") {
        println(string)
        for (i in availablePort)
            println("在i的元素：${i}")
    }

    private fun getIp(): String {
        val wm = getSystemService(MyApplication.context, WifiManager::class.java)
        //检查Wifi状态
        if (!wm!!.isWifiEnabled) {
            Toast.makeText(MyApplication.context, "未连接WIFI，请打开WIFI", Toast.LENGTH_SHORT).show()
            return "0.0.0.0"
        }
        val wi = wm.connectionInfo
        //获取32位整型IP地址
        val ipAdd = wi.ipAddress
        //把整型地址转换成“*.*.*.*”地址
        return intToIp(ipAdd)
    }

    private fun intToIp(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)
    }
}