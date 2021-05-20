package com.example.chat

class Msg(val content:String,val type:Int,val imageName:String) {
    companion object{
        const val TYPE_RECEIVED = 0
        const val TYPE_SENT = 1
    }
}