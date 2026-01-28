package com.nano.llm.agent

import com.nano.llm.a2ui.A2UISpec
import com.nano.llm.intent.IntentUnderstanding

/** System Agent 响应 */
data class SystemAgentResponse(
    val message: String,
    val a2ui: A2UISpec? = null,
    val conversationState: TaskStatus,
    val followUpSuggestions: List<String> = emptyList(),
    val participatingAgents: List<String> = emptyList()
)

/**
 * System Agent - 系统智能体
 *
 * 核心能力:
 * 1. 多 Agent 选择与筛选
 * 2. 协调执行
 * 3. 响应聚合
 * 4. 构建最终响应
 */
class SystemAgent(
    private val agentRegistry: AgentRegistry,
    private val agentCoordinator: AgentCoordinator,
    private val responseAggregator: ResponseAggregator
) {

    /**
     * 处理意图理解结果 - 核心入口
     * @param understanding 由外部（LLM）解析出的意图理解结果
     * @param context 对话上下文
     */
    suspend fun processRequest(
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): SystemAgentResponse {
        // 1. 选择匹配的 Agent
        val selectedAgents = selectAgents(understanding)

        if (selectedAgents.isEmpty()) {
            return SystemAgentResponse(
                message = "抱歉，没有找到能处理此请求的应用",
                conversationState = TaskStatus.FAILED
            )
        }

        // 2. 协调执行
        val responses = agentCoordinator.coordinate(
            agents = selectedAgents,
            understanding = understanding,
            context = context
        )

        // 3. 响应聚合
        val aggregated = responseAggregator.aggregate(
            responses = responses,
            understanding = understanding
        )

        // 4. 构建最终响应
        return buildFinalResponse(aggregated)
    }

    /**
     * 选择匹配的 Agent
     *
     * 策略:
     * - 用户明确指定 app → 仅选对应 Agent
     * - 未指定 app 但有能力需求 → 根据能力广播
     * - 系统操作 → 仅系统设置 Agent
     * - 兜底 → 默认 Agent
     */
    fun selectAgents(understanding: IntentUnderstanding): List<AppAgent> {
        return when {
            // 用户明确指定了应用
            understanding.targetApps.isNotEmpty() -> {
                understanding.targetApps.mapNotNull { agentRegistry.getAgent(it) }
            }

            // 根据能力广播到多个 Agent
            understanding.broadcastCapability != null -> {
                agentRegistry.findAgentsByCapability(understanding.broadcastCapability)
            }

            // 系统级意图
            understanding.isSystemIntent() -> {
                listOfNotNull(agentRegistry.getSystemSettingsAgent())
            }

            // 兜底: 默认 Agent
            else -> listOfNotNull(agentRegistry.getDefaultAgent())
        }
    }

    /** 构建最终响应 */
    private fun buildFinalResponse(aggregated: AggregatedResponse): SystemAgentResponse {
        // 优先使用 A2UI 响应
        val a2ui = when {
            aggregated.a2uiResponses.size == 1 -> {
                aggregated.a2uiResponses.first().first
            }
            aggregated.a2uiResponses.size > 1 -> {
                // 多个 A2UI 需要合并 (后续由 A2UIGenerator 处理)
                aggregated.a2uiResponses.first().first
            }
            else -> null
        }

        return SystemAgentResponse(
            message = aggregated.summary,
            a2ui = a2ui,
            conversationState = aggregated.overallState,
            followUpSuggestions = aggregated.allFollowUpActions.map { it.label },
            participatingAgents = aggregated.participatingAgents
        )
    }
}
