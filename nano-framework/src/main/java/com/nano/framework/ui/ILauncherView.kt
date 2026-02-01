package com.nano.framework.ui

/**
 * LauncherView 接口定义
 *
 * 定义了 Launcher UI 必须实现的方法，用于：
 * - 管理聊天消息（增删改查）
 * - 渲染 A2UI 到独立容器
 * - 处理用户交互回调
 *
 * 实现类：
 * - 当前：NanoShellActivity（使用 Android RecyclerView + FrameLayout）
 * - 未来：NanoLauncherView（使用 NanoRecyclerView + NanoContainer）
 */
interface ILauncherView {

    // ==================== 聊天消息管理 ====================

    /**
     * 添加新消息到聊天列表
     * @param message 要添加的消息
     */
    fun addMessage(message: ChatMessage)

    /**
     * 更新已有消息（用于修改 ProcessMessage 状态等）
     * @param messageId 消息 ID
     * @param message 新的消息对象
     */
    fun updateMessage(messageId: String, message: ChatMessage)

    /**
     * 移除消息（用于移除临时的 ProcessMessage）
     * @param messageId 消息 ID
     */
    fun removeMessage(messageId: String)

    /**
     * 滚动聊天列表到底部（查看最新消息）
     */
    fun scrollChatToBottom()

    /**
     * 清空输入框
     */
    fun clearInput()

    // ==================== A2UI 渲染（独立容器）====================

    /**
     * 渲染 A2UI 到独立容器
     * @param spec A2UI 规范对象（使用 Any 避免循环依赖）
     * @param agentName 触发此 A2UI 的 Agent 名称
     */
    fun renderA2UI(spec: Any, agentName: String)

    /**
     * 清空 A2UI 容器
     */
    fun clearA2UI()

    /**
     * 显示/隐藏 A2UI 容器
     * @param show true 显示，false 隐藏
     */
    fun showA2UIContainer(show: Boolean)

    // ==================== 用户交互回调 ====================

    /**
     * 设置用户发送消息的监听器
     * @param listener 回调函数，参数为用户输入的文本
     */
    fun setOnSendListener(listener: (String) -> Unit)

    /**
     * 设置 A2UI 动作的监听器（按钮点击等）
     * @param listener 回调函数，参数为动作字符串
     */
    fun setOnA2UIActionListener(listener: (String) -> Unit)
}
