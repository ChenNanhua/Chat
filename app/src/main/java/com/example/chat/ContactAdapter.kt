package com.example.chat


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.chatUtil.StorageUtil
import kotlinx.android.synthetic.main.contact_item.view.*

class ContactAdapter(private val contactList: List<Contact>) :
    RecyclerView.Adapter<ContactAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contactImage: ImageView = view.contactImage
        val contactName: TextView = view.contactName
        val contactIP: TextView = view.contactIP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contactList[position]
        holder.contactName.text = contact.name
        holder.contactIP.text = contact.IP
        if (contact.imageName == "")
            holder.contactImage.setImageResource(contact.imageId)
        else {
            val uri = StorageUtil.getUri(contact.imageName)
            holder.contactImage.setImageBitmap(StorageUtil.getBitmapFromUri(uri))
        }
    }

    override fun getItemCount(): Int = contactList.size

}