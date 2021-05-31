package com.example.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.chatUtil.ImageUtil
import com.example.chat.data.Msg
import kotlinx.android.synthetic.main.msg_left_item.view.*
import kotlinx.android.synthetic.main.msg_right_item.view.*
import java.lang.IllegalArgumentException

class ContactAdapter(private val msgList: List<Msg>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class LeftViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgLeft: TextView = view.msgLeft
        val msgLeftImage: ImageView = view.msgLeftImage
    }

    inner class RightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgRight: TextView = view.msgRight
        val msgRightImage: ImageView = view.msgRightImage
    }

    override fun getItemViewType(position: Int): Int {
        return msgList[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == Msg.TYPE_RECEIVED) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_left_item, parent, false)
            LeftViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_right_item, parent, false)
            RightViewHolder(view)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = msgList[position]
        when (holder) {
            is LeftViewHolder -> {
                holder.msgLeft.text = msg.content
                if (msg.imageUri.toString() != "") {
                    holder.msgLeftImage.setImageBitmap(ImageUtil.getBitmapFromUri(msg.imageUri))
                } else
                    holder.msgLeftImage.setImageResource(R.drawable.none)
            }
            is RightViewHolder -> {
                holder.msgRight.text = msg.content
                if (msg.imageUri.toString() != "") {
                    holder.msgRightImage.setImageBitmap(ImageUtil.getBitmapFromUri(msg.imageUri))
                } else
                    holder.msgRightImage.setImageResource(R.drawable.none)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int = msgList.size
}