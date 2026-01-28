package com.nano.a2ui.protocol

import com.nano.llm.a2ui.A2UIAction
import com.nano.llm.a2ui.ActionType

/**
 * A2UI 协议定义
 *
 * 定义协议版本、消息信封格式和事件类型，
 * 用于跨应用 A2UI 数据交换和用户交互事件传递。
 */
object A2UIProtocol {

    /** 当前协议版本 */
    const val VERSION = "1.0"

    /** 支持的协议版本列表 */
    val SUPPORTED_VERSIONS = setOf("1.0")

    /**
     * 验证协议版本是否支持
     */
    fun isVersionSupported(version: String): Boolean = version in SUPPORTED_VERSIONS

    /**
     * 验证 A2UIAction 是否合法
     *
     * 不同 ActionType 对参数的要求不同：
     * - AGENT_CALL: 需要 intent 参数
     * - NAVIGATE: 需要 url 参数或非空 target
     * - SUBMIT/DISMISS/CUSTOM: 无额外要求
     */
    fun validateAction(action: A2UIAction): Boolean {
        if (action.target.isEmpty()) return false
        if (action.method.isEmpty()) return false
        return when (action.type) {
            ActionType.AGENT_CALL -> true
            ActionType.NAVIGATE -> action.params?.containsKey("url") == true || action.target.isNotEmpty()
            ActionType.SUBMIT -> true
            ActionType.DISMISS -> true
            ActionType.CUSTOM -> true
        }
    }

    /**
     * A2UI 消息信封 - 跨应用传递 A2UI 数据的容器
     *
     * targetAgentId 为 null 时表示广播消息。
     */
    data class A2UIMessage(
        val messageId: String,
        val version: String = VERSION,
        val sourceAgentId: String,
        val targetAgentId: String? = null,
        val specJson: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * A2UI 事件 - 用户在 A2UI 上的交互
     *
     * 由 A2UIEventBridge 创建，传递给目标 Agent 处理。
     */
    data class A2UIEvent(
        val eventId: String,
        val sourceComponent: String,
        val eventType: EventType,
        val action: A2UIAction?,
        val userData: Map<String, String> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )

    /** 事件类型 */
    enum class EventType {
        CLICK,          // 按钮点击
        ITEM_SELECT,    // 列表项选择
        INPUT_SUBMIT,   // 输入提交
        TAB_SWITCH,     // Tab 切换
        CARD_CLICK      // 卡片点击
    }
}
