package com.example.chat

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chat.chatUtil.ImageUtil
import com.example.chat.chatUtil.LogUtil
import com.example.chat.data.Msg
import kotlinx.android.synthetic.main.msg_image_left_item.view.*
import kotlinx.android.synthetic.main.msg_image_right_item.view.*
import kotlinx.android.synthetic.main.msg_left_item.view.*
import kotlinx.android.synthetic.main.msg_left_item.view.msgLeftAvatar
import kotlinx.android.synthetic.main.msg_right_item.view.*
import kotlinx.android.synthetic.main.msg_right_item.view.msgRightAvatar
import java.lang.Exception
import java.lang.IllegalArgumentException

class ContactAdapter(private val msgList: List<Msg>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class LeftViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgLeftText: TextView = view.msgLeftText
        val msgLeftAvatar: ImageView = view.msgLeftAvatar
    }

    inner class RightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgRightText: TextView = view.msgRightText
        val msgRightAvatar: ImageView = view.msgRightAvatar
    }

    inner class RightImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgRightImage: ImageView = view.msgRightImage
        val msgRightAvatar: ImageView = view.msgRightAvatar
    }

    inner class LeftImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgLeftImage: ImageView = view.msgLeftImage
        val msgLeftAvatar: ImageView = view.msgLeftAvatar
    }

    override fun getItemViewType(position: Int): Int {
        return msgList[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            Msg.TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_left_item, parent, false)
                LeftViewHolder(view)
            }
            Msg.TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_right_item, parent, false)
                RightViewHolder(view)
            }
            Msg.TYPE_IMAGE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_image_left_item, parent, false)
                LeftImageViewHolder(view)
            }
            Msg.TYPE_IMAGE_SENT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.msg_image_right_item, parent, false)
                RightImageViewHolder(view)
            }
            else -> throw Exception("Msg.type不在四种之中")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = msgList[position]
        when (holder) {
            is LeftViewHolder -> {
                holder.msgLeftText.text = msg.content
                if (msg.avatarUri.toString() != "") {
                    holder.msgLeftAvatar.setImageBitmap(ImageUtil.getBitmapFromUri(msg.avatarUri))
                } else
                    holder.msgLeftAvatar.setImageResource(R.drawable.none)
            }
            is RightViewHolder -> {
                holder.msgRightText.text = msg.content
                if (msg.avatarUri.toString() != "") {
                    holder.msgRightAvatar.setImageBitmap(ImageUtil.getBitmapFromUri(msg.avatarUri))
                } else
                    holder.msgRightAvatar.setImageResource(R.drawable.none)
            }
            is LeftImageViewHolder -> {
                holder.msgLeftImage.setImageBitmap(ImageUtil.getBitmapFromUri(Uri.parse(msg.content)))
                if (msg.avatarUri.toString() != "") {
                    holder.msgLeftAvatar.setImageBitmap(ImageUtil.getBitmapFromUri(msg.avatarUri))
                } else
                    holder.msgLeftAvatar.setImageResource(R.drawable.none)
            }
            is RightImageViewHolder -> {
                holder.msgRightImage.setImageBitmap(ImageUtil.getBitmapFromUri(Uri.parse(msg.content)))
                if (msg.avatarUri.toString() != "") {
                    holder.msgRightAvatar.setImageBitmap(ImageUtil.getBitmapFromUri(msg.avatarUri))
                } else
                    holder.msgRightAvatar.setImageResource(R.drawable.none)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int = msgList.size
}