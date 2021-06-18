package com.example.chat


import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.chatUtil.ImageUtil
import com.example.chat.data.Contact
import kotlinx.android.synthetic.main.contact_list_item.view.*

class ContactListAdapter(private val contactList: List<Contact>) :
    RecyclerView.Adapter<ContactListAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contactImage: ImageView = view.contactAvatar
        val contactName: TextView = view.contactName
        val contactIP: TextView = view.contactIP
        val contactOnline: TextView = view.contactOnline
        val contactAddress: TextView = view.contactAddress
        val redCircle:View = view.red_circle
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_list_item, parent, false)
        val viewHolder = ViewHolder(view)
        //给子项注册点击事件
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            //打开聊天界面,传递联系人数据
            val contact = contactList[position] //拿到联系人数据
            val intent =
                Intent(MyApplication.context, ContactActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("contact", contact)
            startActivity(MyApplication.context, intent, null)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contactList[position]
        holder.contactName.text = contact.name
        holder.contactIP.text = contact.IP
        if (contact.avatarUri == "")    //设置头像
            holder.contactImage.setImageResource(R.drawable.none)
        else
            holder.contactImage.setImageBitmap(ImageUtil.getBitmapFromUri(Uri.parse(contact.avatarUri)))
        if (contact.isOnline){          //设置是否在线
            holder.contactOnline.text = MyApplication.context.resources.getText(R.string.online)
            holder.contactOnline.setTextColor(MyApplication.context.resources.getColor(R.color.green))
        }else{
            holder.contactOnline.text = MyApplication.context.resources.getText(R.string.offline)
            holder.contactOnline.setTextColor(MyApplication.context.resources.getColor(R.color.red))
        }
        if (contact.isLocal)            //设置地址
            holder.contactAddress.text = MyApplication.context.resources.getText(R.string.local)
        else
            holder.contactAddress.text = MyApplication.context.resources.getText(R.string.internet)
        if (contact.isRedCircle)        //设置小红点
            holder.redCircle.background = MyApplication.context.getDrawable(R.drawable.red_circle)
        else
            holder.redCircle.background = MyApplication.context.getDrawable(R.drawable.primary_circle)

    }

    override fun getItemCount(): Int = contactList.size
}