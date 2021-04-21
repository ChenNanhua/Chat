package com.example.chat

import kotlinx.coroutines.*
import java.util.Collections.synchronizedSet
import kotlin.concurrent.thread

fun main(){
    val test = test()
    test.fun1("10")
    Thread.sleep(10)
    println("total:")
    test.show()
    println("end")

}

class test(){
    private val job = Job()
    private val scope = CoroutineScope(job)
    @Volatile
    private var availablePort = synchronizedSet(mutableSetOf<String>("1"))
    @Volatile
    var list = setOf<String>("1")

    fun show(){
        for(i in availablePort)
            println(i)
    }
    @Synchronized
    fun add(add: String){
        availablePort.add(add)
        show()
    }

    fun fun1(add:String){
        scope.launch(Dispatchers.IO){
            for (i in 2..3)
            launch {
                add(i.toString())
            }
        }
    }
}
fun testMet() = runBlocking { // this: CoroutineScope
    val start = System.currentTimeMillis()
    coroutineScope {
        repeat(100_000) {
            //协程方式
            launch {
                print("-$it")
            }
        }
    }
    println()
    val end = System.currentTimeMillis()
    println("协程耗时：${end - start}")
}


fun testMet01() = runBlocking { // this: CoroutineScope
    val start = System.currentTimeMillis()
    coroutineScope {
        repeat(100_000) {
            //线程方式
            Thread {
                print("*->$it")
            }.start()
        }
    }
    println()
    val end = System.currentTimeMillis()
    println("thread耗时：${end - start}")
}