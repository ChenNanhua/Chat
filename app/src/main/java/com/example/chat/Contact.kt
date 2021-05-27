package com.example.chat

import java.io.Serializable

class Contact (val name:String, val IP:String, val imageUriString:String):Serializable{
    override fun toString(): String {
        return "name:$name, IP:$IP"
    }
}