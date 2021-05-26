package com.example.chat

import org.junit.Test

import android.os.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val socket = Socket()
        socket.connect(InetSocketAddress("10.0.2.2", 8080), 200)
    }
}