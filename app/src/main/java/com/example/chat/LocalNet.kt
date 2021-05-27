package com.example.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.chat.chatUtil.*
import kotlinx.coroutines.*
import java.io.*
import java.net.BindException
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
        @Volatile   //保存所有打开对应port的用户IP
        var availableContact: MutableSet<Contact> = synchronizedSet(mutableSetOf<Contact>())
    }

    //服务器处理线程
    inner class ServerThread(private val socket: Socket, private val messenger: Messenger, private val count: Int = 0) :
        Thread() {
        private val tag = "LocalNetServerThread"
        private lateinit var get: BufferedReader
        private lateinit var send: PrintWriter
        override fun run() {
            super.run()
            var startTime = System.currentTimeMillis()  //判断连接时间是否超时
            get = BufferedReader(InputStreamReader(socket.getInputStream()))
            send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            while (true) {  //处理客户端发送的不同信号
                if (socket.isClosed) break
                val string = get.readLine()
                if (string == "END") break
                if (string != null) {
                    startTime = System.currentTimeMillis()  //更新连接时间
                    LogUtil.d(tag, "服务器${count}收到消息: $string")
                    answer(string)  //处理客户端发送的信息
                } else {    //未收到信息，判断是否超时退出
                    if ((System.currentTimeMillis() - startTime) / 1000 > 10) {
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
                "yourInfo" -> {     //请求用户信息
                    send.println(android.os.Build.MODEL)
                    sendImage(socket)
                    val name = get.readLine()
                    val bitmapName = getImage(socket)
                    val address = socket.remoteSocketAddress.toString()
                    addAddress(Contact(name, address, StorageUtil.getUri(bitmapName).toString()))
                    //更新UI
                    val msg = Message()
                    msg.what = ContactListActivity.updateRecyclerView
                    messenger.send(msg)
                }
                "message" -> {        //发送一条消息
                    val name = get.readLine()
                    val content = get.readLine()
                    if (!ContactActivity.contentMap.containsKey(name))
                        ContactActivity.contentMap[name] = ArrayList()
                    ContactActivity.contentMap[name]?.add(content)
                }
                else -> send.println(string)
            }
            socket.close()
        }
    }

    //创建服务器，接收客户端的连接请求
    fun startServer(messenger: Messenger) {  //接收发来的消息
        thread {
            val serverSocket: ServerSocket
            try {
                serverSocket = ServerSocket(port)
            } catch (e: BindException) {
                LogUtil.d(tag, "服务器已绑定:$port")
                return@thread
            }
            lateinit var socket: Socket
            LogUtil.d(tag, "启动服务器:$port")
            var count = 0
            try {
                while (true) {
                    try {
                        socket = serverSocket.accept()
                        LogUtil.d(tag, "服务器收到一个连接，启动新线程${count}")
                        ServerThread(socket, messenger, count++).start()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket.close()
                serverSocket.close()
                LogUtil.w(tag, "服务器被关闭")
            }
        }
    }

    //在局域网中发起连接请求，搜索其他用户
    fun searchLocal(messenger: Messenger) {
        availableContact.clear()    //清空已保存其他用户的数据
        var address = getIp()       //获取本机IP
        address = "172.16.0.0"      //TODO 蒲公英组网时所用，最后应删去
        LogUtil.d(tag, "客户端正在搜寻,客户端IP: $address")
        val addressList: MutableList<String> = address.split(".").toMutableList()
        thread {
            //发送连接请求
            scope.launch(Dispatchers.IO) {
                //遍历局域网所有主机
                for (i in 1..254) {     //通过遍历IP地址最后一段，遍历局域网所有主机
                    addressList[addressList.size - 1] = i.toString()
                    val tempAddress = addressList.joinToString(".")
                    //LogUtil.d(tag, tempAddress)
                    //排除本机IP和模拟器下的回环地址
                    if (tempAddress != address && tempAddress != "10.0.2.17" && tempAddress != "10.0.2.15") {
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
                                    //与其他用户交换信息
                                    println("与其他用户交换信息: $tempAddress")
                                    send.println("yourInfo")    //发送获取主机信息的信号
                                    val name = get.readLine()
                                    val bitmapName = getImage(socket)
                                    send.println("testName")    //发送自身信息
                                    sendImage(socket)
                                    send.println("END")         //发送断开连接信号
                                    addAddress(
                                        Contact(
                                            name,
                                            tempAddress,
                                            StorageUtil.getUri(bitmapName).toString()
                                        )
                                    )     //将建立过连接的主机添加到可访问主机中
                                } catch (e: java.net.SocketTimeoutException) {
                                    //LogUtil.e(tag, e.message.toString())
                                } catch (e: java.net.ConnectException) {
                                    //e.printStackTrace()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    LogUtil.e(tag, "访问IP出现错误：$tempAddress $e")
                                } finally {
                                    get?.close()
                                    send?.close()
                                    socket?.close()
                                }
                            }
                        }
                    }
                }

            }
            Thread.sleep(1500)  //等待发送完成
            LogUtil.d(tag, "在本地局域网找到的IP: $availableContact")
            //更新UI
            val msg = Message()
            msg.what = ContactListActivity.updateRecyclerView
            messenger.send(msg)
        }
    }

    //服务器端发送图片
    fun sendImage(socket: Socket, imageName: String = "") {
        try {
            val uri = StorageUtil.getUri(imageName)
            val dataOutputStream = DataOutputStream(socket.getOutputStream())
            //发送图片数据
            val dataInputStream = DataInputStream(FileInputStream(StorageUtil.getFileDescriptor(uri)))
            val byte = ByteArray(1024 * 10)
            var len: Int = dataInputStream.read(byte)    //计算每次读取的数据
            var totalLen = 0
            while (len > 0) {
                totalLen += len
                dataOutputStream.writeInt(len)  //发送接下来的字节长度
                dataOutputStream.write(byte, 0, len)    //发送字节数据
                dataOutputStream.flush()        //刷新缓冲
                len = dataInputStream.read(byte)//len==-1代表读取到末尾
            }
            dataOutputStream.writeInt(999999)   //999999 约定的退出符号
            LogUtil.e(tag, "服务器发送一张图片:$uri,图片大小:$totalLen")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //客户端接收图片并保存
    private fun getImage(socket: Socket): String {
        try {
            val byte = ByteArray(1024 * 10)
            var arr = byteArrayOf()
            val dataInputStream = DataInputStream(socket.getInputStream())
            var len = dataInputStream.readInt()
            var totalLen = 0
            while (len > 0) {
                if (len == 999999) break
                totalLen += len
                dataInputStream.readFully(byte, 0, len)
                arr += byte
                len = dataInputStream.readInt()
            }
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(arr, 0, arr.size)
            val bitmapName = StorageUtil.getName()
            StorageUtil.saveBitmapToPicture(bitmap, bitmapName)
            LogUtil.e(tag, "客户端接收到一张图片:$bitmapName,图片大小:  $totalLen,arr大小:  ${arr.size}")
            return bitmapName
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    //在局域网中发送消息
    fun sendMessage(name: String, message: String, ip: String = "0.0.0.0") {
        thread {
            var socket: Socket? = null
            var send: PrintWriter? = null
            var get: BufferedReader? = null
            try {
                socket = Socket(ip, port)
                get = BufferedReader(InputStreamReader(socket.getInputStream()))
                send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                send.println("message")
                send.println(name)
                send.println(message)
                send.println("END")
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
        for (item in availableContact) {
            if (contact.name == item.name)    //已有相同用户直接退出
                return
        }
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