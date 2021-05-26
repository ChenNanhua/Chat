package com.example.chat

import java.io.Serializable

class Contact (val name:String,val IP:String,val imageName:String,val imageId:Int = 0):Serializable{
    override fun toString(): String {
        return "name:$name, IP:$IP"
    }
}