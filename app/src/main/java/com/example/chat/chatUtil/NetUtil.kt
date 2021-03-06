package com.example.chat.chatUtil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Message
import android.os.Messenger
import androidx.core.content.ContextCompat.getSystemService
import com.example.chat.ContactListActivity
import com.example.chat.MyApplication
import com.example.chat.chatUtil.*
import com.example.chat.chatUtil.TinyUtil.addInsert
import com.example.chat.chatUtil.TinyUtil.addInsertAll
import com.example.chat.chatUtil.TinyUtil.loge
import com.example.chat.chatUtil.TinyUtil.toast
import com.example.chat.data.Contact
import com.example.chat.data.Msg
import com.example.chat.data.TimeMsg
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.net.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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
    private const val urlHead = "http://121.40.239.98:8080/"     //TODO 修改为真实IP地址 本地地址125.216.247.37 服务器地址121.40.239.98
    var isGetMessageInternet = false    //判断是否已经开启了接收服务器消息
    private const val localTail = "-local"

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
                    send.writeUTF(MyData.username + localTail)
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
                        && MyData.savedContact[name]!!.avatarUri != ""
                    ) {
                        send.writeUTF("NO")
                        Uri.parse(MyData.savedContact[name]!!.avatarUri)
                    } else {
                        send.writeUTF("YES")
                        if (get.readUTF() == "NO")
                            Uri.parse("")
                        else getImageLocal(socket, name)
                    }
                    //将建立过连接的主机添加到可访问主机中
                    addContact(Contact(name, ip, avatarUri.toString()))
                    if (avatarUri.toString() == "")   //没有头像也要添加到历史联系人数据库
                        DBUtil.setAvatarContact(name, avatarUri.toString())
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
                        MyData.getTempMsgList(name).addInsert(this)
                    }
                }
                "image" -> {
                    println("进入image")
                    val name = get.readUTF()
                    with(getImageLocal(socket)) {
                        with(TimeMsg(name, Msg.TYPE_IMAGE_RECEIVED, this.toString())) {
                            //添加到待展示的数据中
                            MyData.getTempMsgList(name).addInsert(this)
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
        if (!MyApplication.useLocal)        //判断是否打开了局域网搜索
            return
        MyApplication.scope.launch(Dispatchers.IO) {
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
        if (!MyApplication.useLocal)
            return
        val myIP = getIp()       //获取本机IP
        //myIP = "172.16.0.1"      //TODO 蒲公英组网时所用，最后应删去
        LogUtil.d(tag, "客户端正在搜寻,客户端IP: $myIP")
        MyApplication.scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.IO) {   //阻塞
                MyData.onlineContact.clear()    //清空已保存其他用户的数据
                val addressList: MutableList<String> = myIP.split(".").toMutableList()
                //遍历局域网所有主机,发送连接请求
                for (i in 1..254) {     //通过遍历IP地址最后一段，遍历局域网所有主机
                    addressList[addressList.size - 1] = i.toString()
                    val tempIP = addressList.joinToString(".")
                    //LogUtil.d(tag, tempIP)
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
                                    "局域网搜索到的用户：$name".loge()
                                    val avatarUri: Uri
                                    //有头像的联系人不用请求头像
                                    avatarUri = if (MyData.savedContact.containsKey(name)
                                        && MyData.savedContact[name]!!.avatarUri != ""
                                    ) {
                                        send.writeUTF("NO")
                                        Uri.parse(MyData.savedContact[name]!!.avatarUri)
                                    } else {
                                        send.writeUTF("YES")
                                        if (get.readUTF() == "NO")
                                            Uri.parse("")
                                        else getImageLocal(socket, name)
                                    }
                                    //将建立过连接的主机添加到可访问主机中
                                    addContact(Contact(name, tempIP, avatarUri.toString(), true))
                                    if (avatarUri.toString() == "")   //没有头像也要添加到历史联系人数据库
                                        DBUtil.setAvatarContact(name, avatarUri.toString())
                                    //发送自身信息
                                    send.writeUTF(MyData.username + localTail)
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
                                    //LogUtil.d(tag, "$tempIP done!")
                                }
                            }
                        }
                    }
                }
            }
            LogUtil.d(tag, "在本地局域网找到的IP: ${MyData.onlineContact}")
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

    //局域网客户端接收图片并保存 contactName用来判断是否接收的头像，是的话更新联系人数据库
    private fun getImageLocal(socket: Socket, contactName: String = ""): Uri {
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
                if (contactName != "")
                    DBUtil.setAvatarContact(contactName, this.toString(), bitmapName)
                return this
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Uri.parse("")
    }

    //在局域网中发送消息
    fun sendMessageLocal(content: String, contactName: String, ip: String) {
        MyApplication.scope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(ip, port)
                val send = DataOutputStream(socket.getOutputStream())
                send.writeUTF("message")
                send.writeUTF(MyData.username + localTail)
                send.writeUTF(content)
                send.writeUTF("END")
                MyData.getTempMsgList(contactName)
                    .addInsert(TimeMsg(contactName, Msg.TYPE_SENT, content, DateUtil.getDate()))
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }

    //局域网发送一张照片
    fun sendSingleImageLocal(imageUri: Uri, contactName: String, ip: String) {
        MyApplication.scope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(ip, port)
                val send = DataOutputStream(socket.getOutputStream())
                send.writeUTF("image")
                send.writeUTF(MyData.username + localTail)
                sendImageLocal(socket, imageUri)
                send.writeUTF("END")
                //更新到数据库中去
                MyData.getTempMsgList(contactName)
                    .addInsert(TimeMsg(contactName, Msg.TYPE_IMAGE_SENT, imageUri.toString()))
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }

    //处理待更新的数据，插入数据库，发送更新信息到ContactActivity
    fun updateTempMsg(contactMessenger: Messenger) {
        MyApplication.scope.launch(Dispatchers.IO) {
            val tempMsgMapName = MyData.tempMsgMapName
            while (true) {      //判断对应聊天对象是否有消息发送过来
                if (MyData.tempMsgMapName != "" && MyData.getTempMsgList(MyData.tempMsgMapName).size > 0) {
                    //遍历添加时间信息
                    MyData.tempTimeMsgMap.forEach { (key, value) ->
                        var i = 0
                        while (i < value.size) {
                            if (!MyData.msgTime.containsKey(key)) {      //第一次初始化时间戳
                                MyData.msgTime[key] = DateUtil.parseDate(value[i].date)
                                val timeMsg: TimeMsg             //待插入的时间戳
                                with(value[i]) {
                                    timeMsg = TimeMsg(
                                        contactName,
                                        Msg.TIME,
                                        DateUtil.noMillSec(date),
                                        DateUtil.minusSec(date)
                                    )
                                    value.addInsert(i, timeMsg)
                                    i++
                                }
                            } else {    //判断是否与上次时间戳超过五分钟
                                if ((DateUtil.parseDate(value[i].date).time - MyData.msgTime[key]!!.time) / 1000 > 1 * 60) {
                                    MyData.msgTime[key] = DateUtil.parseDate(value[i].date)
                                    val timeMsg: TimeMsg             //待插入的时间戳
                                    with(value[i]) {
                                        timeMsg = TimeMsg(
                                            contactName,
                                            Msg.TIME,
                                            DateUtil.noMillSec(date),
                                            DateUtil.minusSec(date)
                                        )
                                        value.addInsert(i, timeMsg)
                                        i++
                                    }
                                }
                            }
                            i++
                        }
                    }
                    //更新contact UI
                    val msg = Message()
                    msg.what = ContactListActivity.updateRecyclerView
                    contactMessenger.send(msg)
                } else
                    delay(50)  //没有新消息时休眠50毫秒
                if (tempMsgMapName != MyData.tempMsgMapName)    //判断是否仍在对应聊天对象的聊天界面
                    break
            }
        }
    }

    //更新联系人界面的新消息红点
    fun updateRedCircle(contactListMessenger: Messenger) {
        MyApplication.scope.launch(Dispatchers.IO) {
            while (MyData.redCircle) {
                var update = false
                val updateList = ArrayList<Int>(MyData.contactList.size)
                //有新消息就设置红点
                for (i in 0 until MyData.contactList.size)
                    with(MyData.contactList[i]) {
                        if (MyData.tempTimeMsgMap.containsKey(this.name)) {
                            //如果有联系人信息，且没设为红点，设为红点
                            if (MyData.tempTimeMsgMap[this.name]!!.size > 0) {
                                if (!MyData.contactList[i].isRedCircle) {
                                    this.isRedCircle = true
                                    updateList.add(i)
                                    update = true
                                }
                            } else    //如果没联系人信息，但联系人为红点，设置回红点
                                if (this.isRedCircle) {
                                    this.isRedCircle = false
                                    updateList.add(i)
                                    update = true
                                }
                        }
                    }
                if (update) {
                    //更新contact UI
                    val msg = Message()
                    msg.obj = updateList
                    msg.what = ContactListActivity.updateRedCircle
                    contactListMessenger.send(msg)
                } else
                    delay(400)
            }
        }
    }

    //为多线程提供的锁
    @Synchronized
    private fun addContact(contact: Contact) {
        if (MyData.onlineContact.containsKey(contact.name))//已有相同用户直接跳过
            return
        MyData.onlineContact[contact.name] = contact
    }

    //获取本机的IP地址
    private fun getIp(): String {
        val wm = getSystemService(MyApplication.context, WifiManager::class.java)
        //检查Wifi状态
        if (!wm!!.isWifiEnabled) {
            "要进行局域网聊天，请打开WIFI".toast()
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

    //okHttp get请求模板
    private fun okHttpGet(urlTail: String): Response {
        val request = Request.Builder().url(urlHead + urlTail).build()
        return MyApplication.client.newCall(request).execute()
    }

    //从服务器获取聊天用户
    fun searchInternet(contactListMessenger: Messenger) {
        MyApplication.scope.launch(Dispatchers.IO) {
            try {
                val response = okHttpGet("android/info?name=${MyData.username}")
                val result = response.body?.string()
                val type = object : TypeToken<List<Contact>>() {}.type
                val contacts: List<Contact> = gson.fromJson(result, type)   //返回的json数据转换为联系人列表
                LogUtil.d(tag, "纯净的从服务器找到的contact${contacts}")
                MyData.onlineContact.clear()
                withContext(Dispatchers.IO) {
                    for (contact in contacts) {                 //将服务器有记录的主机添加到可访问主机中
                        launch {
                            withContext(Dispatchers.IO) Return@{
                                if (contact.name == MyData.username)    //排除自己账号
                                    return@Return
                                val avatarName: String              //储存保存到本地的图片名
                                if (contact.avatarUri != "") {      //头像链接不为空，获取头像数据
                                    LogUtil.e(tag, "urlToUri内容：${MyData.urlToUri}")
                                    //如果保存过联系人对应的头像，且头像不为空，直接使用本地uri
                                    if (MyData.urlToUri.containsKey(contact.avatarUri) && MyData.urlToUri[contact.avatarUri] != "")
                                        contact.avatarUri = MyData.urlToUri[contact.avatarUri]!!
                                    else {    //如果没有保存过联系人对应的头像，当即保存并使用保存后的uri,uri插入数据库
                                        LogUtil.e(tag, "访问的url: ${urlHead + contact.avatarUri}")
                                        //虚拟机下载图片要二十秒，但是真机下载非常快
                                        val starTime = System.currentTimeMillis()
                                        val response1 = okHttpGet(contact.avatarUri)
                                        LogUtil.e(tag, "下载头像用时:${(System.currentTimeMillis() - starTime) / 1000}秒")
                                        val bitmap = BitmapFactory.decodeStream(response1.body?.byteStream())
                                        avatarName = ImageUtil.getName()
                                        //把接收到的头像信息保存到本地相册，并更新contact数据库
                                        if (bitmap != null) {
                                            ImageUtil.saveBitmapToPicture(bitmap, avatarName)?.run {
                                                //更新uriToUri数据库
                                                DBUtil.DB.execSQL(
                                                    "INSERT INTO urlToUri values(?,?)",
                                                    arrayOf(contact.avatarUri, this.toString())
                                                )
                                                contact.avatarUri = this.toString()
                                            }
                                        } else
                                            contact.avatarUri = ""      //下载头像失败,avatarUri重置回去
                                    }
                                }
                                //统一添加联系人到数据库
                                DBUtil.setAvatarContact(contact.name, contact.avatarUri, isLocal = false)
                                addContact(contact)
                            }
                        }
                    }
                }
                LogUtil.d(tag, "在服务器找到的IP: ${MyData.onlineContact}")
                //更新UI
                val msg = Message()
                msg.what = ContactListActivity.updateRecyclerView
                contactListMessenger.send(msg)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    //从服务器登出
    fun logoutInternet() {
        thread {
            okHttpGet("android/logout?name=${MyData.username}")
            LogUtil.d(tag, "从服务器退出：${MyData.username}")
        }
        Thread.sleep(200)   //等待请求完成
    }

    //上传头像到服务器
    fun sendAvatarInternet(uri: Uri) {
        MyApplication.scope.launch(Dispatchers.IO) {
            try {

                ImageUtil.getInputStream(uri)?.readBytes()?.let { fileData ->
                    val urlInfo = urlHead + "android/uploadAvatar?name=${MyData.username}"
                    val fileBody = fileData.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", ImageUtil.getName() + ".jpg", fileBody)
                        .build()
                    val request = Request.Builder().url(urlInfo).post(requestBody).build()
                    val response = MyApplication.client.newCall(request).execute()
                    val result = response.body?.string()
                    LogUtil.d(tag, "上传头像到服务器：${result.toString()}")
                }
            } catch (e: Exception) {

            }
        }
    }

    //把发送的消息发送到服务器上
    fun sendMessageInternet(contactName: String, content: String) {
        MyApplication.scope.launch(Dispatchers.IO) {
            try {
                val urlInfo = urlHead + "android/sendMessage?" +
                        "name=${MyData.username}&contactName=${contactName}"
                val formBody = FormBody.Builder().add("content", content).build()
                val request = Request.Builder().url(urlInfo).post(formBody).build()
                val response = MyApplication.client.newCall(request).execute()
                val result = response.body?.string()
                MyData.getTempMsgList(contactName)
                    .addInsert(TimeMsg(contactName, Msg.TYPE_SENT, content, DateUtil.getDate()))
                LogUtil.d(tag, "发送消息给服务器：${result.toString()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //发送图片到服务器
    fun sendSingleImageInternet(uri: Uri, contactName: String) {
        MyApplication.scope.launch(Dispatchers.IO) {
            try {
                ImageUtil.getInputStream(uri)?.readBytes()?.let { fileData ->
                    val urlInfo = urlHead + "android/sendImage?name=${MyData.username}&contactName=$contactName"
                    val fileBody = fileData.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", ImageUtil.getName() + ".jpg", fileBody)
                        .build()
                    val request = Request.Builder().url(urlInfo).post(requestBody).build()
                    val response = MyApplication.client.newCall(request).execute()
                    val result = response.body?.string()
                    LogUtil.d(tag, "发送图片到服务器：${uri}，服务器url：$result")
                    //保存图片Uri到数据库,添加到待更新聊天信息列表
                    MyData.getTempMsgList(contactName)
                        .addInsert(TimeMsg(contactName, Msg.TYPE_IMAGE_SENT, uri.toString()))
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    //从服务器上下载其他人发给自己的消息，同时自己保活
    fun getMessageInternet() {
        MyApplication.scope.launch(Dispatchers.IO) {
            if (!isGetMessageInternet) {
                isGetMessageInternet = true
                var startTime = System.currentTimeMillis()
                while (true) {
                    try {
                        if ((System.currentTimeMillis() - startTime) / 1000 > 60) {        //每60秒发送一次在线信息
                            startTime = System.currentTimeMillis()
                            okHttpGet("android/info?name=${MyData.username}").close()
                        }
                        //获取
                        val response = okHttpGet("android/getMessage?name=${MyData.username}")
                        val result = response.body?.string() ?: ""
                        LogUtil.d(tag, "从服务器接收到的消息${result}")
                        if (result.length < 5) {  //长度小于五代表无任何新消息
                            delay(1000)
                            continue
                        }
                        //转换消息
                        val type = object : TypeToken<HashMap<String, List<TimeMsg>>>() {}.type
                        val internetTimeMsgMap: HashMap<String, List<TimeMsg>> =
                            gson.fromJson(result, type)   //返回的json数据转换为联系人列表
                        //插入到数据库，如果是图片消息还要下载
                        internetTimeMsgMap.forEach { (key, value) ->
                            for (timeMsg: TimeMsg in value) {
                                with(timeMsg) {
                                    when (this.type) {
                                        Msg.TYPE_RECEIVED -> {
                                        }
                                        Msg.TYPE_IMAGE_RECEIVED -> {      //发送的是图片，需要下载图片保存到本地
                                            LogUtil.e(tag, "下载的图片url: ${urlHead + timeMsg.content}")
                                            //虚拟机下载图片要二十秒，但是真机下载非常快
                                            val response1 = okHttpGet(timeMsg.content)
                                            val bitmap = BitmapFactory.decodeStream(response1.body?.byteStream())
                                            val imageName = ImageUtil.getName()
                                            //把接收到的图片保存到本地相册，修改content内容
                                            ImageUtil.saveBitmapToPicture(bitmap, imageName)?.let {
                                                this.content = it.toString()
                                            }
                                        }
                                        else -> {
                                            LogUtil.e(tag, "接收到的信息有误！！！")
                                        }
                                    }
                                }
                            }
                            MyData.getTempMsgList(key).addInsertAll(value)
                        }
                    } catch (e: Exception) {
                        //e.printStackTrace()
                        delay(2000)
                    } finally {
                        delay(700)
                    }
                }
            }
        }
    }
}