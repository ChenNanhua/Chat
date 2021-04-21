package com.example.chat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_contact.*


class ContactActivity : AppCompatActivity() {
    private val contactList = ArrayList<Contact>()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.contact_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {   //menu菜单添加响应
        val localNet = LocalNet()
        when(item.itemId){
            R.id.getMessage->localNet.getMessage()
            R.id.sendMessage->localNet.sendMessage()
            R.id.searchLocal->localNet.searchLocal()
            R.id.startMyService->{
                startService(Intent(this,MyService::class.java))
            }
        }
        return true
    }
    private fun initContact(){
        repeat(1){
            with(contactList){
                add(Contact("girl",R.drawable.girl))
                add(Contact("dog",R.drawable.dog))
                add(Contact("grass",R.drawable.grass))
                add(Contact("wangzai",R.drawable.wangzai))
            }
        }
    }
}