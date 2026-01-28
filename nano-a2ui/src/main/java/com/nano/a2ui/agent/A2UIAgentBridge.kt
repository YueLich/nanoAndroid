package com.nano.a2ui.agent

import com.nano.a2ui.protocol.A2UIProtocol
import com.nano.llm.a2ui.*
import com.nano.llm.agent.AgentRegistry
import java.util.concurrent.ConcurrentHashMap

/**
 * A2UI Agent 桥接器 - 协调 A2UI 与多个 Agent 之间的通信
 *
 * 功能:
 * 1. 管理 Agent 与 A2UISpec 的活跃映射
 * 2. 合并多个 Agent 的 A2UI 响应（Tab 视图 / 统一列表）
 * 3. 支持跨 Agent 的 A2UI 消息构建
 */
class A2UIAgentBridge(
    private val agentRegistry: AgentRegistry
) {

    /** Agent ID → 当前活跃 A2UISpec */
    private val activeSpecs = ConcurrentHashMap<String, A2UISpec>()

    /** 会话 ID → 参与的 Agent 列表 */
    private val sessionAgents = ConcurrentHashMap<String, MutableList<String>>()

    // ==================== Agent Spec 管理 ====================

    fun registerAgentSpec(agentId: String, spec: A2UISpec) {
        activeSpecs[agentId] = spec
    }

    fun clearAgentSpec(agentId: String) {
        activeSpecs.remove(agentId)
    }

    fun getAgentSpec(agentId: String): A2UISpec? = activeSpecs[agentId]

    fun getAllActiveSpecs(): Map<String, A2UISpec> = activeSpecs.toMap()

    // ==================== A2UI 合并 ====================

    /**
     * 将多个 Agent 的 A2UI 合并为 Tab 视图
     *
     * 每个 Agent 的结果作为一个 Tab，用户可切换查看。
     */
    fun mergeToTabs(specs: Map<String, A2UISpec>): A2UISpec {
        val tabs = specs.map { (agentId, spec) ->
            val agent = agentRegistry.getAgent(agentId)
            val label = agent?.agentName ?: agentId
            A2UITab(
                id = agentId,
                label = label,
                content = spec.root
            )
        }

        return A2UISpec(
            version = A2UIProtocol.VERSION,
            root = A2UITabContent(id = "merged_tabs", tabs = tabs)
        )
    }

    /**
     * 将多个 Agent 的 A2UI 合并为统一列表
     *
     * 提取各 Agent 容器的子组件，平铺到同一个容器下。
     */
    fun mergeToUnifiedList(specs: Map<String, A2UISpec>): A2UISpec {
        val children = specs.flatMap { (_, spec) ->
            flattenComponents(spec.root)
        }

        return A2UISpec(
            version = A2UIProtocol.VERSION,
            root = A2UIContainer(id = "unified_list", children = children)
        )
    }

    // ==================== 跨 Agent 消息 ====================

    /**
     * 构建跨 Agent 的消息信封
     */
    fun createCrossAgentMessage(
        sourceAgentId: String,
        targetAgentId: String?,
        specJson: String
    ): A2UIProtocol.A2UIMessage {
        // 追踪会话参与者
        val sessionId = "session_${sourceAgentId}"
        sessionAgents.getOrPut(sessionId) { mutableListOf() }.let { agents ->
            if (sourceAgentId !in agents) agents.add(sourceAgentId)
            targetAgentId?.let { if (it !in agents) agents.add(it) }
        }

        return A2UIProtocol.A2UIMessage(
            messageId = "a2ui_msg_${System.currentTimeMillis()}",
            sourceAgentId = sourceAgentId,
            targetAgentId = targetAgentId,
            specJson = specJson
        )
    }

    /** 获取会话中的参与 Agent 列表 */
    fun getSessionAgents(sessionId: String): List<String> {
        return sessionAgents[sessionId]?.toList() ?: emptyList()
    }

    // ==================== 辅助方法 ====================

    /** 递归展平容器组件的子组件 */
    private fun flattenComponents(component: A2UIComponent): List<A2UIComponent> {
        return when (component) {
            is A2UIContainer -> component.children
            else -> listOf(component)
        }
    }
}
