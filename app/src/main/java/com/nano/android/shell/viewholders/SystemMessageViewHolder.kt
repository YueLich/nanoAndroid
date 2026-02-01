package com.nano.android.shell.viewholders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nano.android.R
import com.nano.framework.ui.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 系统响应消息 ViewHolder
 *
 * 展示系统回复消息（左对齐气泡）
 * 包含：
 * - Agent 名称标签
 * - 系统消息文本
 * - 建议操作 Chips
 * - 时间戳
 */
class SystemMessageViewHolder(
    itemView: View,
    private val onSuggestionClick: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val tvAgentName: TextView = itemView.findViewById(R.id.tvAgentName)
    private val tvMessage: TextView = itemView.findViewById(R.id.tvSystemMessage)
    private val chipGroupSuggestions: ChipGroup = itemView.findViewById(R.id.chipGroupSuggestions)
    private val tvTimestamp: TextView = itemView.findViewById(R.id.tvSystemTimestamp)

    fun bind(message: ChatMessage.SystemMessage) {
        // 显示参与的 Agent
        if (message.participatingAgents.isNotEmpty()) {
            tvAgentName.visibility = View.VISIBLE
            tvAgentName.text = message.participatingAgents.joinToString(", ")
        } else {
            tvAgentName.visibility = View.GONE
        }

        // 显示系统消息
        tvMessage.text = message.message

        // 显示建议操作 Chips
        chipGroupSuggestions.removeAllViews()
        if (message.suggestions.isNotEmpty()) {
            chipGroupSuggestions.visibility = View.VISIBLE
            message.suggestions.forEach { suggestion ->
                val chip = Chip(itemView.context).apply {
                    text = suggestion
                    isClickable = true
                    setOnClickListener { onSuggestionClick(suggestion) }
                }
                chipGroupSuggestions.addView(chip)
            }
        } else {
            chipGroupSuggestions.visibility = View.GONE
        }

        // 显示时间戳
        tvTimestamp.text = formatTimestamp(message.timestamp)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
