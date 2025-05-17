package com.samurai.chatme.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samurai.chatme.MessageModel
import com.samurai.chatme.R

class ChatAdapter(
    private val messages: MutableList<MessageModel>,
    private val currentUserId: String,
    private val onMessageLongClick: (View, Int, MessageModel) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val youLayout = view.findViewById<LinearLayout>(R.id.youMessageLayout)
        val youName = view.findViewById<TextView>(R.id.youName)
        val youMessage = view.findViewById<TextView>(R.id.youMessage)

        val otherLayout = view.findViewById<LinearLayout>(R.id.otherMessageLayout)
        val otherName = view.findViewById<TextView>(R.id.otherName)
        val otherMessage = view.findViewById<TextView>(R.id.otherMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        if (message.senderId == currentUserId) {
            holder.youLayout.visibility = View.VISIBLE
            holder.otherLayout.visibility = View.GONE
            holder.youName.text = "Siz"
            holder.youMessage.text = message.message
            holder.youLayout.setOnClickListener {
                onMessageLongClick(it, position, message)
                true
            }
        } else {
            holder.youLayout.visibility = View.GONE
            holder.otherLayout.visibility = View.VISIBLE
            holder.otherName.text = message.senderName
            holder.otherMessage.text = message.message
            holder.otherLayout.setOnClickListener {
                onMessageLongClick(it, position, message)
                true
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun removeMessage(position: Int) {
        if (position >= 0 && position < messages.size) {
            messages.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}
