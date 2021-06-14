package com.example.chat

import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

fun main() {
    val port = 8080
    println("开始监听")
    var inputByte: ByteArray? = null
    var length = 0
    var dis: DataInputStream? = null
    var fos: FileOutputStream? = null
    try {
        val server = ServerSocket(port)
        val socket = server.accept()
        try {
            dis = DataInputStream(socket.getInputStream())
            fos = FileOutputStream(File("D:\\temp\\test-in\\grass.png"))
            inputByte = ByteArray(1024 * 4)
            println("开始接收数据...")
            while (dis.read(inputByte, 0, inputByte.size).also({ length = it }) > 0) {
                fos.write(inputByte, 0, length)
                fos.flush()
            }
            println("完成接收")
        } finally {
            fos?.close()
            dis?.close()
            socket?.close()
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

}

fun connect(i: Int, addressList: MutableList<String>, availablePort: MutableList<String>) {
    addressList[addressList.size - 1] = i.toString()
    val tempAddress = addressList.joinToString(".")
    println(tempAddress)
    try {
        val socket = Socket()
        socket.connect(InetSocketAddress(tempAddress, 8080), 100)
        availablePort.add(tempAddress)
    } catch (e: Exception) {
        println(e.message)
    }
}