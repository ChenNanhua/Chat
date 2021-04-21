package com.example.chat

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.*

fun main() {


    val address: String = InetAddress.getLocalHost().toString().split("/")[1];  //电脑名/IP
    val addressList: MutableList<String> = address.split(".").toMutableList()
    val availablePort: MutableList<String> = mutableListOf()
    val job= Job()
    val scope = CoroutineScope(job)

    val start = System.currentTimeMillis()
    scope.launch(Dispatchers.IO) {
        for (i in 1..255) {
            launch {
                connect(i, addressList, availablePort)
            }
        }
    }
    Thread.sleep(1100)
    job.cancel()
    println(availablePort)
    println("运行时间：${System.currentTimeMillis() - start}")
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