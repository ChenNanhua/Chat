package com.example.chat


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.chat.chatUtil.*
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
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
        var availableContact: MutableSet<Contact> = synchronizedSet(mutableSetOf<Contact>())
    }

    //服务器处理线程
    inner class ServerThread(private val socket: Socket, private val count: Int = 0) : Thread() {
        private val tag = "LocalNetServerThread"
        private lateinit var get: BufferedReader
        private lateinit var send: PrintWriter
        override fun run() {
            super.run()
            var startTime = System.currentTimeMillis()  //判断连接时间是否超时
            get = BufferedReader(InputStreamReader(socket.getInputStream()))
            send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            while (true) {  //处理客户端发送的不同信号
                if(socket.isClosed) break
                val string = get.readLine()
                if (string == "END") break
                if (string != null) {
                    startTime = System.currentTimeMillis()  //更新连接时间
                    LogUtil.d(tag, "服务器${count}收到消息: $string")
                    answer(string)  //处理客户端发送的信息
                } else {    //未收到信息，判断是否超时退出
                    if ((System.currentTimeMillis() - startTime) / 1000 > 5) {
                        LogUtil.d(tag, "服务器${count}等待超时，断开连接")
                        break   //客户端超时未连接即断开
                    }
                    LogUtil.d(tag, "服务器${count}未接收到消息，休眠0.3秒")
                    sleep(300)
                }
            }
            socket.close()
        }

        private fun answer(string: String) {
            when (string) {
                "INFO" -> {
                    send.println("testName")
                    sendImage(socket)
                }
                else->send.println(string)
            }
        }
    }

    //创建服务器，接收客户端的连接请求
    fun startServer() {  //接收发来的消息
        thread {
            val serverSocket = ServerSocket(port)
            lateinit var socket: Socket
            LogUtil.d(tag, "启动服务器:$port")
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

    //在局域网中发起连接请求，搜索其他用户
    fun searchLocal(handler: Handler) {
        availableContact.clear()     //清空已保存其他用户的数据
        val address = getIp()   //获取本机IP
        LogUtil.d(tag, "客户端正在搜寻,客户端IP: $address")
        val addressList: MutableList<String> = address.split(".").toMutableList()
        val host = addressList[addressList.size - 1].toInt() //排除host时所用
        val start = System.currentTimeMillis()
        thread {
            var isBreak = true
            //循环发送连接请求
            while (isBreak) {
                scope.launch(Dispatchers.IO) {
                    //遍历局域网所有主机
                    for (i in 1..254) {     //通过遍历IP地址最后一段，遍历局域网所有主机
                        addressList[addressList.size - 1] = i.toString()
                        val tempAddress = addressList.joinToString(".")
                        //LogUtil.d(tag, tempAddress)
                        //排除本机IP和模拟器下的回环地址
                        if (i != host && tempAddress != "10.0.2.17" && tempAddress != "10.0.2.15") {
                            launch {
                                withContext(Dispatchers.IO) {
                                    var socket: Socket? = null
                                    var send: PrintWriter? = null
                                    var get: BufferedReader? = null
                                    try {
                                        socket = Socket()
                                        socket.connect(InetSocketAddress(tempAddress, port), 200)   //尝试与主机建立联系
                                        get = BufferedReader(InputStreamReader(socket.getInputStream()))
                                        send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                                        send.println("INFO")    //发送获取主机信息信号
                                        val name = get.readLine()
                                        val bitmapName = getImage(socket)
                                        send.println("END")     //发送断开连接信号
                                        addAddress(Contact(name, tempAddress, bitmapName))     //将建立过连接的主机添加到可访问主机中
                                    } catch (e: java.net.SocketTimeoutException) {
                                        //LogUtil.v(tag, e.message.toString())
                                    } catch (e:Exception){
                                        e.printStackTrace()
                                        LogUtil.e(tag,"--------tempAddress$tempAddress")
                                    }finally {
                                        send?.close()
                                        socket?.close()
                                        get?.close()
                                    }
                                }
                            }
                        }
                    }
                }
                Thread.sleep(1500)
                LogUtil.d(tag, "在本地局域网找到的IP: $availableContact")
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

    //服务器端发送图片
    fun sendImage(socket: Socket,imageName:String = "641"){
        try {
            val uri = StorageUtil.getUri(imageName)
            val dataOutputStream = DataOutputStream(socket.getOutputStream())
            //发送图片数据
            val dataInputStream = DataInputStream(FileInputStream(StorageUtil.getFileDescriptor(uri)))
            val byte = ByteArray(1024 * 10)
            var len:Int = dataInputStream.read(byte)    //计算每次读取的数据
            var totalLen = 0
            while (len > 0) {
                totalLen += len
                dataOutputStream.writeInt(len)  //发送接下来的字节长度
                dataOutputStream.write(byte, 0, len)    //发送字节数据
                dataOutputStream.flush()    //刷新缓冲
                if (len<1024*10) break      //读取到末尾
                len = dataInputStream.read(byte)
            }
            LogUtil.e(tag, "服务器发送一张图片:$uri,图片大小:$totalLen")
            dataOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //客户端接收图片并保存
    private fun getImage(socket: Socket):String{
        val byte = ByteArray(1024 * 10)
        var arr = byteArrayOf()
        val dataInputStream = DataInputStream(socket.getInputStream())
        var len = dataInputStream.readInt()
        var totalLen = 0
        while (len > 0){
            totalLen += len
            dataInputStream.readFully(byte,0,len)
            arr += byte
            if (len!=1024*10) break
            len = dataInputStream.readInt()
        }
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(arr, 0, arr.size)
        val bitmapName = StorageUtil.getName()
        StorageUtil.saveBitmapToPicture(bitmap, bitmapName)
        LogUtil.e(tag, "客户端接收到一张图片:$bitmapName,图片大小:  $totalLen,arr大小:  ${arr.size}")
        return bitmapName
    }

    //在局域网中发送消息
    fun sendLocalMessage(message: String, ip: String = "0.0.0.0") {
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

    //为多线程提供的锁
    @Synchronized
    private fun addAddress(contact: Contact) {
        availableContact.add(contact)
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