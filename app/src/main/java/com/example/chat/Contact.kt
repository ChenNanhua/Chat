package com.example.chat

class Contact (val name:String,val IP:String,val imageName:String,val imageId:Int = 0){
    override fun toString(): String {
        return IP
    }
}