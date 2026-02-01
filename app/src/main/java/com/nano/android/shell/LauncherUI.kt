package com.nano.android.shell

import com.nano.framework.ui.ChatMessage
import com.nano.framework.ui.ILauncherView
import com.nano.llm.a2ui.A2UISpec
import com.nano.llm.agent.ConversationContext
import com.nano.llm.api.NaturalLanguageAPI
import com.nano.llm.model.LLMMessage
import com.nano.llm.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LauncherUI 核心控制器
 *
 * 职责：
 * - 管理对话历史（messageHistory）
 * - 维护对话上下文（conversationContext）
 * - 协调 SystemAgent 调用（通过 NaturalLanguageAPI）
 * - 处理用户输入和 A2UI 动作
 * - 控制 UI 层的展示逻辑（通过 ILauncherView 接口）
 *
 * @param naturalLanguageAPI LLM 自然语言 API
 * @param view LauncherView 接口实现（UI 层）
 */
class LauncherUI(
    private val naturalLanguageAPI: NaturalLanguageAPI,
    private val view: ILauncherView
) {
    // 对话历史（仅文本消息，不包含 ProcessMessage）
    private val messageHistory = mutableListOf<ChatMessage>()

    // 对话上下文（用于传递给 LLM）
    private var conversationContext = ConversationContext(
        conversationId = "launcher_${System.currentTimeMillis()}",
        userQuery = "",
        previousQueries = emptyList(),
        activeAgents = emptyList()
    )

    // 当前渲染的 A2UI 规范（用于状态追踪）
    private var currentA2UISpec: A2UISpec? = null

    init {
        // 设置 A2UI 动作监听器
        view.setOnA2UIActionListener { action ->
            // 注意：这里是同步调用，需要外部用协程包裹
            // onA2UIAction(action)
        }
    }

    /**
     * 处理用户发送消息
     *
     * 流程：
     * 1. 添加用户消息到聊天容器
     * 2. 显示处理中提示
     * 3. 调用 NaturalLanguageAPI
     * 4. 移除处理中提示
     * 5. 添加系统响应到聊天容器
     * 6. 渲染 A2UI（如果有）
     *
     * @param input 用户输入的文本
     */
    suspend fun onUserSendMessage(input: String) = withContext(Dispatchers.Main) {
        // 1. 添加用户消息到聊天容器
        val userMsg = ChatMessage.UserMessage(
            id = "user_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            text = input
        )
        messageHistory.add(userMsg)
        view.addMessage(userMsg)
        view.clearInput()

        // 2. 添加处理过程消息到聊天容器
        val processId = "process_${System.currentTimeMillis()}"
        val processMsg = ChatMessage.ProcessMessage(
            id = processId,
            timestamp = System.currentTimeMillis(),
            process = "正在处理您的请求..."
        )
        view.addMessage(processMsg)
        view.scrollChatToBottom()

        // 3. 调用 NaturalLanguageAPI（切换到 IO 线程）
        val response = try {
            withContext(Dispatchers.IO) {
                conversationContext = conversationContext.withQuery(input)
                naturalLanguageAPI.processUserInput(
                    userInput = input,
                    context = conversationContext,
                    conversationHistory = buildConversationHistory()
                )
            }
        } catch (e: Exception) {
            // 错误处理：返回错误消息
            withContext(Dispatchers.IO) {
                naturalLanguageAPI.processUserInput(
                    userInput = "ERROR: ${e.message}",
                    context = conversationContext,
                    conversationHistory = emptyList()
                )
            }
        }

        // 4. 移除处理消息（切回主线程）
        withContext(Dispatchers.Main) {
            view.removeMessage(processId)

            // 5. 添加系统响应到聊天容器
            val systemMsg = ChatMessage.SystemMessage(
                id = "system_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                message = response.message,
                state = response.conversationState.toString(),
                participatingAgents = response.participatingAgents,
                suggestions = response.followUpSuggestions
            )
            messageHistory.add(systemMsg)
            view.addMessage(systemMsg)
            view.scrollChatToBottom()

            // 6. 渲染 A2UI 到独立容器（如果有）
            response.a2ui?.let { a2uiSpec ->
                currentA2UISpec = a2uiSpec
                val agentName = response.participatingAgents.firstOrNull() ?: "System"
                view.renderA2UI(a2uiSpec, agentName)
                view.showA2UIContainer(true)
            } ?: run {
                // 如果没有 A2UI，隐藏容器
                currentA2UISpec = null
                view.showA2UIContainer(false)
            }
        }
    }

    /**
     * 处理 A2UI 动作（按钮点击等）
     *
     * 策略：将 A2UI 动作作为新的用户输入处理
     *
     * @param action 动作字符串（如 "清除"、"计算"）
     */
    suspend fun onA2UIAction(action: String) {
        onUserSendMessage(action)
    }

    /**
     * 构建对话历史（用于传递给 LLM）
     *
     * 格式转换：ChatMessage → LLMMessage
     *
     * @return LLM 消息列表
     */
    private fun buildConversationHistory(): List<LLMMessage> {
        return messageHistory.mapNotNull { msg ->
            when (msg) {
                is ChatMessage.UserMessage -> LLMMessage(
                    role = MessageRole.USER,
                    content = msg.text
                )
                is ChatMessage.SystemMessage -> LLMMessage(
                    role = MessageRole.ASSISTANT,
                    content = msg.message
                )
                is ChatMessage.ProcessMessage -> null // 不包含临时消息
            }
        }
    }

    /**
     * 清空对话历史（重置会话）
     */
    fun clearHistory() {
        messageHistory.clear()
        conversationContext = ConversationContext(
            conversationId = "launcher_${System.currentTimeMillis()}",
            userQuery = "",
            previousQueries = emptyList(),
            activeAgents = emptyList()
        )
        currentA2UISpec = null
    }
}
