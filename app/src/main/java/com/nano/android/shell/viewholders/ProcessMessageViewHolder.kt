package com.nano.android.shell.viewholders

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nano.android.R
import com.nano.framework.ui.ChatMessage

/**
 * 处理过程消息 ViewHolder
 *
 * 展示处理中的状态提示（Loading）
 * 包含：
 * - ProgressBar（加载动画）
 * - 提示文本（如"正在处理您的请求..."）
 */
class ProcessMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    private val tvProcess: TextView = itemView.findViewById(R.id.tvProcess)

    fun bind(message: ChatMessage.ProcessMessage) {
        tvProcess.text = message.process
        progressBar.visibility = View.VISIBLE
    }
}
