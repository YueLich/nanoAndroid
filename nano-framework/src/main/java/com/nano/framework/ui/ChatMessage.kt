package com.nano.framework.ui

/**
 * 聊天消息数据模型
 *
 * 用于在对话流界面中展示不同类型的消息
 */
sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    /**
     * 用户输入消息
     * @param text 用户输入的文本
     */
    data class UserMessage(
        override val id: String,
        override val timestamp: Long,
        val text: String
    ) : ChatMessage()

    /**
     * 系统响应消息
     * @param message 系统回复的文本
     * @param state 对话状态
     * @param participatingAgents 参与的 Agent 列表
     * @param suggestions 建议的后续问题/操作
     */
    data class SystemMessage(
        override val id: String,
        override val timestamp: Long,
        val message: String,
        val state: String? = null,
        val participatingAgents: List<String> = emptyList(),
        val suggestions: List<String> = emptyList()
    ) : ChatMessage()

    /**
     * 处理过程消息（Loading 状态）
     * @param process 当前处理过程的描述
     */
    data class ProcessMessage(
        override val id: String,
        override val timestamp: Long,
        val process: String
    ) : ChatMessage()
}
