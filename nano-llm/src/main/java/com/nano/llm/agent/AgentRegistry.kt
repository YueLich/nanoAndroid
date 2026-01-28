package com.nano.llm.agent

import java.util.concurrent.ConcurrentHashMap

/**
 * Agent 注册表 - 管理所有可用的 App Agent
 */
class AgentRegistry {

    private val agents = ConcurrentHashMap<String, AppAgent>()
    private val capabilityIndex = ConcurrentHashMap<AgentCapability, MutableList<String>>()

    /** 注册 Agent */
    fun registerAgent(agent: AppAgent) {
        // 如果已存在同 ID 的 Agent，先清理索引
        agents[agent.agentId]?.let { existing ->
            existing.capabilities.forEach { capability ->
                capabilityIndex[capability]?.remove(agent.agentId)
            }
        }

        agents[agent.agentId] = agent
        agent.capabilities.forEach { capability ->
            capabilityIndex.getOrPut(capability) { mutableListOf() }.add(agent.agentId)
        }
    }

    /** 注销 Agent */
    fun unregisterAgent(agentId: String) {
        val agent = agents.remove(agentId) ?: return
        agent.capabilities.forEach { capability ->
            capabilityIndex[capability]?.remove(agentId)
        }
    }

    /** 获取 Agent */
    fun getAgent(agentId: String): AppAgent? = agents[agentId]

    /** 根据单个能力查找 Agent */
    fun findAgentsByCapability(capability: AgentCapability): List<AppAgent> {
        return capabilityIndex[capability]?.mapNotNull { agents[it] } ?: emptyList()
    }

    /** 根据多个能力查找（匹配任一即可） */
    fun findAgentsByCapabilities(capabilities: Set<AgentCapability>): List<AppAgent> {
        return agents.values.filter { agent ->
            capabilities.any { it in agent.capabilities }
        }.toList()
    }

    /** 获取所有 Agent 能力描述 */
    fun getAllCapabilities(): List<AgentCapabilityDescription> {
        return agents.values.map { it.describeCapabilities() }
    }

    /** 系统设置 Agent */
    fun getSystemSettingsAgent(): AppAgent? = agents["system_settings"]

    /** 默认 Agent */
    fun getDefaultAgent(): AppAgent? = agents["default"]

    /** 已注册 Agent 数量 */
    fun getAgentCount(): Int = agents.size
}
