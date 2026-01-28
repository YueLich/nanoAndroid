package com.nano.llm.agent

import com.nano.llm.a2ui.A2UIAction

/**
 * App Agent 接口 - 应用需要实现此接口以支持 AI 交互
 */
interface AppAgent {

    /** Agent 标识 */
    val agentId: String

    /** Agent 名称 */
    val agentName: String

    /** 支持的能力 */
    val capabilities: Set<AgentCapability>

    /** 处理来自 System Agent 的请求 */
    suspend fun handleRequest(request: AgentMessage): TaskResponsePayload

    /** 获取 Agent 能力描述 */
    fun describeCapabilities(): AgentCapabilityDescription

    /** 处理用户在 A2UI 上的交互 */
    suspend fun handleUIAction(action: A2UIAction): TaskResponsePayload
}

/** Agent 能力枚举 */
enum class AgentCapability {
    SEARCH,
    ORDER,
    PAYMENT,
    MESSAGE,
    NAVIGATION,
    MEDIA,
    SETTINGS
}

/** 能力描述 */
data class AgentCapabilityDescription(
    val agentId: String,
    val supportedIntents: List<String>,
    val supportedEntities: List<String>,
    val exampleQueries: List<String>,
    val responseTypes: Set<ResponseType>
)
