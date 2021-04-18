package com.example.chat

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_contact.*
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket


class ContactActivity : AppCompatActivity() {
    private val contactList = ArrayList<Contact>()
    private val port = 8080
    private val tag = "ContactActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        initContact()
        val layoutManager = LinearLayoutManager(this)
        val adapter = ContactAdapter(contactList)
        contactRecycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        contactRecycleView.layoutManager = layoutManager
        contactRecycleView.adapter = adapter
    }

    private fun initContact(){
        repeat(1){
            contactList.add(Contact("girl",R.drawable.girl))
            contactList.add(Contact("dog",R.drawable.dog))
            contactList.add(Contact("grass",R.drawable.grass))
            contactList.add(Contact("wangzai",R.drawable.wangzai))
        }
    }
    private fun searchLocal(){
        val server:Socket = ServerSocket(port).accept()

    }
    private fun keepAliveLocal(){
        Log.d(tag,"创建客户端")
        val address: InetAddress = InetAddress.getByName(null)
        try {
            val socket:Socket = Socket(address, 8080)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}