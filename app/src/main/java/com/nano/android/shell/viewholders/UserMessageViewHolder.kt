package com.nano.android.shell.viewholders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nano.android.R
import com.nano.framework.ui.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用户消息 ViewHolder
 *
 * 展示用户输入的消息（右对齐气泡）
 */
class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
    private val tvTimestamp: TextView = itemView.findViewById(R.id.tvUserTimestamp)

    fun bind(message: ChatMessage.UserMessage) {
        tvMessage.text = message.text
        tvTimestamp.text = formatTimestamp(message.timestamp)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
