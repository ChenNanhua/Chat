package com.example.chat


import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.chat.chatUtil.*
import kotlinx.coroutines.*
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

    companion object Instance {
        //保存所有打开port的用户IP
        @Volatile
        var availableIp: MutableSet<String> = synchronizedSet(mutableSetOf<String>())
    }

    //服务器处理线程
    class ServerThread(private val socket: Socket, private val count: Int = 0) : Thread() {
        private val tag = "LocalNetServerThread"
        override fun run() {
            super.run()
            var startTime = System.currentTimeMillis()  //判断连接是否超时
            val get = BufferedReader(InputStreamReader(socket.getInputStream()))
            val send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            while (true) {
                val str: String? = get.readLine()
                if (str == "END") break
                if (str != null) {
                    startTime = System.currentTimeMillis()  //判断连接是否超时
                    LogUtil.d(tag, "服务器${count}收到消息: $str")
                    send.println(str)
                } else {
                    if ((System.currentTimeMillis() - startTime) / 1000 > 5) {
                        LogUtil.d(tag, "服务器${count}等待超时，断开连接")
                        break   //客户端超时未连接即断开
                    }
                    LogUtil.d(tag, "服务器${count}未接收到消息，正在循环")
                    sleep(300)
                }
            }
            socket.close()
        }
    }

    //创建服务器，接收客户端的连接请求
    fun startServer() {  //接收发来的消息
        thread {
            val serverSocket = ServerSocket(port)
            lateinit var socket: Socket
            LogUtil.d(tag, "启动服务器")
            var count = 0
            try {
                while (true) {
                    try {
                        socket = serverSocket.accept()
                        LogUtil.d(tag, "服务器收到一个连接，启动新线程${count}")
                        ServerThread(socket, count++).start()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket.close()
                serverSocket.close()
                LogUtil.d(tag, "服务器被关闭---------")
            }
        }
    }

    //在局域网中发送消息
    fun sendMessage(message: String, ip: String = "0.0.0.0") {
        thread {
            var socket: Socket? = null
            var send: PrintWriter? = null
            var get: BufferedReader? = null
            try {
                socket = Socket(ip, port)
                get = BufferedReader(InputStreamReader(socket.getInputStream()))
                send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                send.println(message)
                send.println("END")
                val str: String = get.readLine()
                LogUtil.d(tag, "客户端收到消息: $str")
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                send?.close()
                get?.close()
                socket?.close()
            }
        }
    }

    //不停的在局域网中发起连接请求，搜索其他用户
    fun searchLocal(handler: Handler) {
        availableIp.clear()     //清空已保存其他用户的数据
        val address = getIp()   //获取本机IP
        LogUtil.d(tag, "客户端正在搜寻,客户端IP: $address")
        val addressList: MutableList<String> = address.split(".").toMutableList()
        val host: Int = addressList[addressList.size - 1].toInt() //排除host时所用
        val start = System.currentTimeMillis()
        thread {
            var isBreak = true
            //循环发送连接请求
            while (isBreak) {
                LogUtil.d(tag, "客户端进入循环")
                scope.launch(Dispatchers.IO) {
                    //遍历局域网所有主机
                    for (i in 1..255) {
                        launch {
                            if (i != host) {
                                //通过遍历IP地址最后一段，遍历局域网所有主机
                                addressList[addressList.size - 1] = i.toString()
                                val tempAddress = addressList.joinToString(".")
                                //LogUtil.d(tag, tempAddress)
                                var socket: Socket? = null
                                var send: PrintWriter? = null
                                try {
                                    //尝试与主机建立联系
                                    socket = Socket()
                                    send = withContext(Dispatchers.IO) {
                                        socket.connect(InetSocketAddress(tempAddress, port), 100)
                                        PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                                    }
                                    send.println("INFO")    //发送获取主机信息信号
                                    send.println("END")     //发送断开连接信号
                                    addAddress(tempAddress)     //将建立过连接的主机添加到可访问主机中
                                } catch (e: Exception) {
                                    //LogUtil.v(tag, e.message.toString()
                                } finally {
                                    withContext(Dispatchers.IO) {
                                        send?.close()
                                        socket?.close()
                                    }
                                }
                            }
                        }
                    }
                }
                Thread.sleep(1500)
                LogUtil.d(tag, "在本地局域网找到的IP: $availableIp")
                //更新UI
                val msg = Message()
                msg.what = ContactActivity.updateRecyclerView
                handler.sendMessage(msg)
                //发送超过一定时间后退出
                Thread.sleep(1000)
                if (System.currentTimeMillis() - start > 2000) {
                    isBreak = false
                }
            }
        }
    }

    //为多线程提供的锁
    @Synchronized
    private fun addAddress(address: String) {
        availableIp.add(address)
    }

    //获取本机的IP地址
    private fun getIp(): String {
        val wm = getSystemService(MyApplication.context, WifiManager::class.java)
        //检查Wifi状态
        if (!wm!!.isWifiEnabled) {
            Toast.makeText(MyApplication.context, "未连接WIFI，请打开WIFI", Toast.LENGTH_SHORT).show()
            return "0.0.0.0"
        }
        val wi = wm.connectionInfo
        //把整型地址转换成“*.*.*.*”地址
        return intToIp(wi.ipAddress)
    }

    //32位整型IP地址转换成“*.*.*.*”地址
    private fun intToIp(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)
    }
}