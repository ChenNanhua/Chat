package com.example.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.chat.chatUtil.*
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import kotlin.concurrent.thread

class LocalNet {
    private val tag = "LocalNet"
    private val port = 8080

    companion object Instance {
        lateinit var serverSocket: ServerSocket
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
                "yourInfo" -> {     //返回自身信息
                    send.println(MyData.username)
                    if (get.readLine() == "YES")
                        sendImage(socket)
                    //获取对方信息
                    val name = get.readLine()
                    val ip = with(socket.remoteSocketAddress) {
                        this.toString().split("/")[0].split(":")[0]
                    }
                    val imageUri: Uri
                    //有头像的联系人不用请求头像
                    imageUri = if (MyData.savedContact.containsKey(name)) {
                        send.println("NO")
                        Uri.parse(MyData.savedContact[name])
                    } else {
                        send.println("YES")
                        getImage(socket, name)
                    }
                    //将建立过连接的主机添加到可访问主机中
                    addAddress(Contact(name, ip, imageUri.toString()))
                    //更新UI
                    val msg = Message()
                    msg.what = ContactListActivity.updateRecyclerView
                    messenger.send(msg)
                }
                "message" -> {        //发送一条消息
                    val name = get.readLine()
                    val content = get.readLine()
                    val imageUri = Uri.parse("")
                    if (!MyData.tempMsgMap.containsKey(name))
                        MyData.tempMsgMap[name] = ArrayList()
                    MyData.tempMsgMap[name]?.add(Msg(content, Msg.TYPE_RECEIVED,imageUri))
                }
                else -> send.println(string)
            }
            socket.close()
        }
    }

    //创建服务器，接收客户端的连接请求
    fun startServer(messenger: Messenger) {  //接收发来的消息
        thread {
            var out = false
            try {
                serverSocket = ServerSocket(port)
            } catch (e: BindException) {
                LogUtil.d(tag, "服务器已被绑定:$port")
                out = true
            }
            if (!out) {
                lateinit var socket: Socket
                LogUtil.d(tag, "启动服务器:$port")
                var count = 0
                serverSocket@ while (true) {
                    try {
                        socket = serverSocket.accept()
                        LogUtil.d(tag, "服务器收到一个连接，启动新线程${count}")
                        ServerThread(socket, messenger, count++).start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        socket.close()
                        serverSocket.close()
                        LogUtil.d(tag, "服务器被关闭...")
                        break@serverSocket
                    }
                }
            }
        }
    }

    //在局域网中发起连接请求，搜索其他用户
    fun searchLocal(messenger: Messenger) {
        MyData.accessContact.clear()    //清空已保存其他用户的数据
        var address = getIp()       //获取本机IP
        address = "172.16.0.0"      //TODO 蒲公英组网时所用，最后应删去
        LogUtil.d(tag, "客户端正在搜寻,客户端IP: $address")
        val addressList: MutableList<String> = address.split(".").toMutableList()
        thread {
            //发送连接请求
            MyApplication.scope.launch(Dispatchers.IO) {
                //遍历局域网所有主机
                for (i in 1..254) {     //通过遍历IP地址最后一段，遍历局域网所有主机
                    addressList[addressList.size - 1] = i.toString()
                    val tempIP = addressList.joinToString(".")
                    //排除本机IP和模拟器下的回环地址
                    if (tempIP != address && tempIP != "10.0.2.17" && tempIP != "10.0.2.15") {
                        launch {
                            withContext(Dispatchers.IO) {
                                var socket: Socket? = null
                                var send: PrintWriter? = null
                                var get: BufferedReader? = null
                                try {
                                    socket = Socket()
                                    socket.connect(InetSocketAddress(tempIP, port), 200)   //尝试与主机建立联系
                                    get = BufferedReader(InputStreamReader(socket.getInputStream()))
                                    send = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                                    //与其他用户交换信息
                                    println("与其他用户交换信息: $tempIP")
                                    send.println("yourInfo")    //发送获取主机信息的信号
                                    val name = get.readLine()
                                    val imageUri: Uri
                                    //有头像的联系人不用请求头像
                                    imageUri = if (MyData.savedContact.containsKey(name)) {
                                        send.println("NO")
                                        Uri.parse(MyData.savedContact[name])
                                    } else {
                                        send.println("YES")
                                        getImage(socket, name)
                                    }
                                    //将建立过连接的主机添加到可访问主机中
                                    addAddress(Contact(name, tempIP, imageUri.toString()))
                                    //发送自身信息
                                    send.println(MyData.username)
                                    if (get.readLine() == "YES")
                                        sendImage(socket)
                                    //发送断开连接信号
                                    send.println("END")
                                } catch (e: SocketTimeoutException) {
                                    //LogUtil.e(tag, e.message.toString())
                                } catch (e: ConnectException) {
                                    //e.printStackTrace()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    LogUtil.e(tag, "访问IP出现错误：$tempIP $e")
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
            LogUtil.d(tag, "在本地局域网找到的IP: ${MyData.accessContact}")
            //更新UI
            val msg = Message()
            msg.what = ContactListActivity.updateRecyclerView
            messenger.send(msg)
        }
    }

    //服务器端发送图片
    fun sendImage(socket: Socket, imageName: String = "") {
        val dataOutputStream = DataOutputStream(socket.getOutputStream())
        val dataInputStream: DataInputStream
        val uri = ImageUtil.getUri(imageName)
        try {
            //获取待发送的图片数据
            dataInputStream = DataInputStream(FileInputStream(ImageUtil.getFileDescriptor(uri)))
        } catch (e: Exception) {        //数据获取出错，发送错误消息，不发送照片
            //e.printStackTrace()
            LogUtil.d(tag, "服务器获取照片信息出错,imageName:$imageName")
            dataOutputStream.writeInt(0)
            return
        }
        dataOutputStream.writeInt(1)    //数据获取正常，发送照片数据
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
    }

    //客户端接收图片并保存
    private fun getImage(socket: Socket, username: String): Uri {
        val dataInputStream = DataInputStream(socket.getInputStream())
        val isSend = dataInputStream.readInt()
        if (isSend == 0)
            return Uri.parse("")
        val byte = ByteArray(1024 * 10)
        var arr = byteArrayOf()
        var totalLen = 0
        try {
            var len = dataInputStream.readInt()
            while (len > 0) {
                if (len == 999999) break
                totalLen += len
                dataInputStream.readFully(byte, 0, len)
                arr += byte
                len = dataInputStream.readInt()
            }
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(arr, 0, arr.size)
            val bitmapName = ImageUtil.getName()
            LogUtil.e(tag, "客户端接收到一张图片:$bitmapName,图片大小:  $totalLen,arr大小:  ${arr.size}")
            ImageUtil.saveBitmapToPicture(bitmap, bitmapName)?.run {
                DBUtil.setAvatarContact(username, this, bitmapName)
                return this
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Uri.parse("")
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
        if(MyData.accessContact.containsKey(contact.name))//已有相同用户直接退出
                return
        MyData.accessContact[contact.name] = contact
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