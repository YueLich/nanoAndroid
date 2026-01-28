package com.nano.a2ui.bridge

import com.nano.a2ui.protocol.A2UIProtocol
import com.nano.llm.a2ui.A2UIAction
import com.nano.llm.agent.AppAgent
import com.nano.llm.agent.TaskResponsePayload
import java.util.concurrent.ConcurrentHashMap

/**
 * A2UI 事件桥接器 - 处理用户在 A2UI 上的交互事件
 *
 * 核心流程：
 * 1. 用户触发交互（点击按钮、选择列表项等）
 * 2. 根据组件绑定的 A2UIAction 确定目标 Agent
 * 3. 调用目标 Agent 的 handleUIAction
 * 4. 触发回调通知 UI 层更新
 */
class A2UIEventBridge {

    /** 事件处理回调接口 */
    interface EventCallback {
        fun onResponse(response: TaskResponsePayload)
        fun onError(error: Exception)
    }

    /** Agent ID → Agent 实例映射 */
    private val agentMap = ConcurrentHashMap<String, AppAgent>()

    /** 全局事件回调列表 */
    private val globalCallbacks = mutableListOf<EventCallback>()

    /** 组件级别回调（按组件 ID 绑定） */
    private val componentCallbacks = ConcurrentHashMap<String, EventCallback>()

    // ==================== Agent 管理 ====================

    fun registerAgent(agent: AppAgent) {
        agentMap[agent.agentId] = agent
    }

    fun unregisterAgent(agentId: String) {
        agentMap.remove(agentId)
    }

    fun getRegisteredAgentCount(): Int = agentMap.size

    // ==================== 回调管理 ====================

    fun addGlobalCallback(callback: EventCallback) {
        globalCallbacks.add(callback)
    }

    fun setComponentCallback(componentId: String, callback: EventCallback) {
        componentCallbacks[componentId] = callback
    }

    // ==================== 事件分发 ====================

    /**
     * 分发 A2UI 事件到对应 Agent
     *
     * @return 是否成功分发到 Agent
     */
    suspend fun dispatchEvent(event: A2UIProtocol.A2UIEvent): Boolean {
        val action = event.action ?: return false

        val agent = agentMap[action.target] ?: return false

        return try {
            val response = agent.handleUIAction(action)

            // 触发组件级别回调
            componentCallbacks[event.sourceComponent]?.onResponse(response)

            // 触发全局回调
            globalCallbacks.forEach { it.onResponse(response) }

            true
        } catch (e: Exception) {
            componentCallbacks[event.sourceComponent]?.onError(e)
            globalCallbacks.forEach { it.onError(e) }
            false
        }
    }

    /**
     * 直接执行 Action（不经过事件包装）
     */
    suspend fun executeAction(action: A2UIAction): TaskResponsePayload? {
        val agent = agentMap[action.target] ?: return null
        return try {
            agent.handleUIAction(action)
        } catch (e: Exception) {
            globalCallbacks.forEach { it.onError(e) }
            null
        }
    }

    // ==================== 事件构建 ====================

    /** 构建按钮点击事件 */
    fun createClickEvent(componentId: String, action: A2UIAction): A2UIProtocol.A2UIEvent {
        return A2UIProtocol.A2UIEvent(
            eventId = "evt_click_${System.currentTimeMillis()}",
            sourceComponent = componentId,
            eventType = A2UIProtocol.EventType.CLICK,
            action = action
        )
    }

    /** 构建列表项选择事件 */
    fun createItemSelectEvent(
        componentId: String,
        itemId: String,
        action: A2UIAction,
        itemData: Map<String, String> = emptyMap()
    ): A2UIProtocol.A2UIEvent {
        return A2UIProtocol.A2UIEvent(
            eventId = "evt_select_${System.currentTimeMillis()}",
            sourceComponent = componentId,
            eventType = A2UIProtocol.EventType.ITEM_SELECT,
            action = action,
            userData = itemData + ("itemId" to itemId)
        )
    }
}
