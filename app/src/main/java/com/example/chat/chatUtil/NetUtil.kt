package com.example.chat.chatUtil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.chat.ContactListActivity
import com.example.chat.MyApplication
import com.example.chat.chatUtil.*
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import com.example.chat.data.TimeMsg
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import kotlin.concurrent.thread

/*
* socket传输中，先调用PrintWriter(OutputStream)传输文字再调用dataOutStream传输数据，会报奇怪的错误
* 只能调用dataOutStream的writeUTF()方法传输文字后再传输数据，这样没有错误
* */
object NetUtil {
    private const val tag = "NetUtil"
    private const val port = 8090
    private val gson = Gson()
    lateinit var serverSocket: ServerSocket

    //服务器处理线程
    class ServerThread(
        private val socket: Socket,
        private val contactListMessenger: Messenger,
        private val count: Int = 0
    ) : Thread() {
        private val tag = "NetServerThread"
        private lateinit var get: DataInputStream
        private lateinit var send: DataOutputStream
        override fun run() {
            super.run()
            var startTime = System.currentTimeMillis()  //判断连接时间是否超时
            get = DataInputStream(socket.getInputStream())
            send = DataOutputStream(socket.getOutputStream())
            while (true) {  //处理客户端发送的不同信号
                if (socket.isClosed) break
                val string = get.readUTF()
                if (string == "END") {
                    LogUtil.d(tag, "服务器接收到END，退出")
                    break
                }
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
            LogUtil.d(tag, "服务器${count}断开连接")
            socket.close()
        }

        private fun answer(string: String) {
            when (string) {
                "yourInfo" -> {
                    //返回自身信息
                    send.writeUTF(MyData.username)
                    if (get.readUTF() == "YES")       //客户端是否需要头像
                        if (MyData.myAvatarUri.toString() == "")     //自身未设置头像
                            send.writeUTF("NO")
                        else {
                            send.writeUTF("YES")
                            sendImageLocal(socket, MyData.myAvatarUri)
                        }
                    //获取对方信息
                    val name = get.readUTF()
                    //服务器获取的远程IP地址形式 /xx.xx.xx.xx:port
                    val ip = with(socket.remoteSocketAddress.toString()) {
                        if (this.contains("/") && this.contains(":"))
                            this.split("/")[1].split(":")[0]
                        else
                            this
                    }
                    val avatarUri: Uri
                    //有头像的联系人不用请求头像
                    avatarUri = if (MyData.savedContact.containsKey(name)
                        && MyData.savedContact[name] != ""
                    ) {
                        send.writeUTF("NO")
                        Uri.parse(MyData.savedContact[name])
                    } else {
                        send.writeUTF("YES")
                        if (get.readUTF() == "NO")
                            Uri.parse("")
                        else getImageLocal(socket, name)
                    }
                    //将建立过连接的主机添加到可访问主机中
                    addAddress(Contact(name, ip, avatarUri.toString()))
                    //更新UI
                    val msg = Message()
                    msg.what = ContactListActivity.updateRecyclerView
                    contactListMessenger.send(msg)
                }
                "message" -> {        //接收发送过来的消息
                    val name = get.readUTF()
                    val content = get.readUTF()
                    with(TimeMsg(name, Msg.TYPE_RECEIVED, content)) {
                        //添加到待展示的数据中
                        MyData.getTempMsgList(name).add(this)
                        //更新到数据库中去
                        DBUtil.DB.execSQL(
                            "insert into msg values(?,?,?,?,?)",
                            arrayOf(MyData.username, contactName, type, content, date)
                        )
                    }
                }
                "image" -> {
                    println("进入image")
                    val name = get.readUTF()
                    with(getImageLocal(socket)) {
                        with(TimeMsg(name, Msg.TYPE_IMAGE_RECEIVED, this.toString())) {
                            //添加到待展示的数据中
                            MyData.getTempMsgList(name).add(this)
                            //更新到数据库中去
                            DBUtil.DB.execSQL(
                                "insert into msg values(?,?,?,?,?)",
                                arrayOf(MyData.username, contactName, type, content, date)
                            )
                        }
                    }
                }
                "test" -> {
                    //测试
                    println("进入test")
                    val dataInputStream = DataInputStream(socket.getInputStream())
                    try {
                        println("服务器int1:${dataInputStream.readInt()},")
                        println("服务器int2:${dataInputStream.readInt()},")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    println("退出test")
                }
                else -> send.writeUTF(string)
            }
            socket.close()
        }
    }

    //局域网创建服务器，接收客户端的连接请求
    fun startServerLocal(contactListMessenger: Messenger) {  //接收发来的消息
        thread {
            try {
                serverSocket = ServerSocket(port)
                lateinit var socket: Socket
                LogUtil.d(tag, "启动服务器:$port")
                var count = 0
                serverSocket@ while (true) {
                    try {
                        socket = serverSocket.accept()
                        LogUtil.d(tag, "服务器收到一个连接，启动新线程${count}")
                        ServerThread(socket, contactListMessenger, count++).start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        socket.close()
                        serverSocket.close()
                        LogUtil.d(tag, "服务器有异常，服务器被关闭...")
                        break@serverSocket
                    }
                }
            } catch (e: BindException) {
                LogUtil.d(tag, "服务器已被绑定:$port")
            }
        }
    }

    //在局域网中发起连接请求，搜索其他用户
    fun searchLocal(contactListMessenger: Messenger) {
        var myIP = getIp()       //获取本机IP
        myIP = "172.16.0.1"      //TODO 蒲公英组网时所用，最后应删去
        LogUtil.d(tag, "客户端正在搜寻,客户端IP: $myIP")
        MyApplication.scope.launch(Dispatchers.IO) {
            MyData.accessContact.clear()    //清空已保存其他用户的数据
            val addressList: MutableList<String> = myIP.split(".").toMutableList()
            //遍历局域网所有主机,发送连接请求
            for (i in 1..254) {     //通过遍历IP地址最后一段，遍历局域网所有主机
                addressList[addressList.size - 1] = i.toString()
                val tempIP = addressList.joinToString(".")
                //LogUtil.d(tag,tempIP)
                //排除本机IP和模拟器下的回环地址
                if (tempIP != myIP && tempIP != "10.0.2.17" && tempIP != "10.0.2.15") {
                    launch {
                        withContext(Dispatchers.IO) {
                            var socket: Socket? = null
                            try {
                                socket = Socket()
                                socket.connect(InetSocketAddress(tempIP, port), 200)   //尝试与主机建立联系
                                //成功连接上其他用户，与其他用户交换信息
                                LogUtil.d(tag, "与其他用户交换信息: $tempIP")
                                val get = DataInputStream(socket.getInputStream())
                                val send = DataOutputStream(socket.getOutputStream())
                                send.writeUTF("yourInfo")    //发送获取主机信息的信号
                                val name = get.readUTF()
                                val imageUri: Uri
                                //有头像的联系人不用请求头像
                                imageUri = if (MyData.savedContact.containsKey(name)
                                    && MyData.savedContact[name] != ""
                                ) {
                                    send.writeUTF("NO")
                                    Uri.parse(MyData.savedContact[name])
                                } else {
                                    send.writeUTF("YES")
                                    if (get.readUTF() == "NO")
                                        Uri.parse("")
                                    else getImageLocal(socket, name)
                                }
                                //将建立过连接的主机添加到可访问主机中
                                addAddress(Contact(name, tempIP, imageUri.toString()))
                                //发送自身信息
                                send.writeUTF(MyData.username)
                                if (get.readUTF() == "YES")
                                    if (MyData.myAvatarUri.toString() == "")
                                        send.writeUTF("NO")
                                    else {
                                        send.writeUTF("YES")
                                        sendImageLocal(socket, MyData.myAvatarUri)
                                    }
                                //发送断开连接信号
                                send.writeUTF("END")
                            } catch (e: SocketTimeoutException) {
                                //LogUtil.e(tag, e.message.toString())
                            } catch (e: ConnectException) {
                                //e.printStackTrace()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                LogUtil.e(tag, "访问IP出现错误：$tempIP $e")
                            } finally {
                                socket?.close()
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
            contactListMessenger.send(msg)
        }
    }

    //局域网服务器端发送图片
    fun sendImageLocal(socket: Socket, uri: Uri) {
        val dataOutputStream = DataOutputStream(socket.getOutputStream())
        val dataInputStream: DataInputStream
        try {   //获取待发送的图片数据
            dataInputStream = DataInputStream(ImageUtil.getInputStream(uri))
        } catch (e: Exception) {        //数据获取出错，发送错误消息，不发送照片
            e.printStackTrace()
            LogUtil.d(tag, "服务器获取待发送照片出错,imageName:$uri")
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
            //LogUtil.e(tag, "服务器发送数据长度:$len")
            dataOutputStream.write(byte, 0, len)    //发送字节数据
            dataOutputStream.flush()        //刷新缓冲
            len = dataInputStream.read(byte)//len==-1代表读取到末尾
        }
        dataOutputStream.writeInt(999999)   //999999 约定的退出符号
        //LogUtil.e(tag, "服务器发送数据长度:999999")
        LogUtil.e(tag, "服务器发送一张图片:$uri,图片大小:$totalLen")
    }

    //局域网客户端接收图片并保存
    private fun getImageLocal(socket: Socket, username: String = ""): Uri {
        val dataInputStream = DataInputStream(socket.getInputStream())
        val isSend = dataInputStream.readInt()
        if (isSend == 0)
            return Uri.parse("")
        val byte = ByteArray(1024 * 10)
        var arr = byteArrayOf()
        var totalLen = 0
        try {
            var len = dataInputStream.readInt()
            //LogUtil.e(tag, "客户端接收数据长度:$len")
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
                if (username != "")
                    DBUtil.setAvatarContact(username, this, bitmapName)
                return this
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Uri.parse("")
    }

    //在局域网中发送消息
    fun sendMessageLocal(message: String, contactName: String, ip: String) {
        MyApplication.scope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(ip, port)
                val send = DataOutputStream(socket.getOutputStream())
                send.writeUTF("message")
                send.writeUTF(MyData.username)
                send.writeUTF(message)
                send.writeUTF("END")
                //更新到数据库中去
                DBUtil.DB.execSQL(
                    "insert into msg values(?,?,?,?,?)",
                    arrayOf(MyData.username, contactName, Msg.TYPE_SENT, message, TimeMsg.getDate())
                )
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }

    //局域网发送一张照片
    fun sendSingleImageLocal(imageUri: Uri, contactName: String, ip: String,contactMessenger:Messenger) {
        MyApplication.scope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(ip, port)
                val send = DataOutputStream(socket.getOutputStream())
                send.writeUTF("image")
                send.writeUTF(MyData.username)
                sendImageLocal(socket, imageUri)
                send.writeUTF("END")
                //更新到数据库中去
                ImageUtil.getBitmapFromUri(imageUri)?.let { bitmap ->
                    ImageUtil.saveBitmapToPicture(bitmap, ImageUtil.getName())?.let {
                        DBUtil.DB.execSQL(
                            "insert into msg values(?,?,?,?,?)",
                            arrayOf(
                                MyData.username,
                                contactName,
                                Msg.TYPE_IMAGE_SENT,
                                it.toString(),
                                TimeMsg.getDate()
                            )
                        )
                        //添加到待更新聊天信息列表
                        MyData.getTempMsgList(contactName)
                            .add(TimeMsg(contactName, Msg.TYPE_IMAGE_SENT, it.toString()))
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }

    //接收到消息更新到ContactActivity
    fun updateTempMsg(contactMessenger: Messenger) {
        MyApplication.scope.launch(Dispatchers.IO) {
            val tempMsgMapName = MyData.tempMsgMapName
            while (true) {      //判断对应聊天对象是否有消息发送过来
                if (MyData.tempMsgMapName != "" && MyData.getTempMsgList(MyData.tempMsgMapName).size > 0) {
                    //更新contact UI
                    val msg = Message()
                    msg.what = ContactListActivity.updateRecyclerView
                    contactMessenger.send(msg)
                }
                delay(300)  //每三百毫秒更新一次聊天信息
                if (tempMsgMapName != MyData.tempMsgMapName)    //判断是否仍在对应聊天对象的聊天界面
                    break
            }
        }
    }

    //为多线程提供的锁
    @Synchronized
    private fun addAddress(contact: Contact) {
        if (MyData.accessContact.containsKey(contact.name))//已有相同用户直接跳过
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