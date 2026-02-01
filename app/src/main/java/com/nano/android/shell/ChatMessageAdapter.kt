package com.nano.android.shell

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nano.android.R
import com.nano.android.shell.viewholders.ProcessMessageViewHolder
import com.nano.android.shell.viewholders.SystemMessageViewHolder
import com.nano.android.shell.viewholders.UserMessageViewHolder
import com.nano.framework.ui.ChatMessage

/**
 * 聊天消息 RecyclerView 适配器
 *
 * 支持 3 种消息类型：
 * - UserMessage: 用户输入消息
 * - SystemMessage: 系统响应消息
 * - ProcessMessage: 处理过程提示（Loading）
 *
 * 职责：
 * - 管理消息列表（增删改）
 * - 根据消息类型创建不同的 ViewHolder
 * - 提供消息操作的公共方法
 */
class ChatMessageAdapter(
    private val onSuggestionClick: (String) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 消息列表
    private val messages = mutableListOf<ChatMessage>()

    // ViewType 常量
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_SYSTEM = 2
        private const val VIEW_TYPE_PROCESS = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ChatMessage.UserMessage -> VIEW_TYPE_USER
            is ChatMessage.SystemMessage -> VIEW_TYPE_SYSTEM
            is ChatMessage.ProcessMessage -> VIEW_TYPE_PROCESS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_user_message, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_SYSTEM -> {
                val view = inflater.inflate(R.layout.item_system_message, parent, false)
                SystemMessageViewHolder(view, onSuggestionClick)
            }
            VIEW_TYPE_PROCESS -> {
                val view = inflater.inflate(R.layout.item_process_message, parent, false)
                ProcessMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message as ChatMessage.UserMessage)
            is SystemMessageViewHolder -> holder.bind(message as ChatMessage.SystemMessage)
            is ProcessMessageViewHolder -> holder.bind(message as ChatMessage.ProcessMessage)
        }
    }

    override fun getItemCount(): Int = messages.size

    // ==================== 消息操作方法 ====================

    /**
     * 添加消息
     */
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    /**
     * 更新消息
     */
    fun updateMessage(messageId: String, message: ChatMessage) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages[index] = message
            notifyItemChanged(index)
        }
    }

    /**
     * 移除消息
     */
    fun removeMessage(messageId: String) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    /**
     * 清空所有消息
     */
    fun clearMessages() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    /**
     * 启用稳定 ID（性能优化）
     */
    override fun getItemId(position: Int): Long {
        return messages[position].id.hashCode().toLong()
    }

    init {
        setHasStableIds(true)
    }
}
