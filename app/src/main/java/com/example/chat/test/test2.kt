package com.example.chat.test

import com.example.chat.data.Contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request

fun main() {
    val client  = OkHttpClient()
    val request = Request.Builder().url("http://localhost:8080/android/info?name=charon&avatarUri=123").build()
    val response = client.newCall(request).execute()
    println(response.message)
    println(response.code)
    val result = response.body?.string()
    println(result)

    val gson = Gson()
    val type = object : TypeToken<List<Contact>>(){}.type
    val contacts:List<Contact> = gson.fromJson(result,type)
    for (contact in contacts){
        println(contact)
    }
}