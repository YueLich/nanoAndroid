package com.nano.llm.agent

import com.nano.llm.intent.CoordinationStrategy
import com.nano.llm.intent.IntentUnderstanding
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** Agent 响应封装 */
data class AgentResponse(
    val agent: AppAgent,
    val payload: TaskResponsePayload? = null,
    val error: Throwable? = null,
    val success: Boolean
)

/**
 * Agent 协调器 - 负责多 Agent 的执行调度
 *
 * 执行策略:
 * - PARALLEL: 所有 Agent 同时执行 (默认)
 * - SEQUENTIAL: 依次执行
 * - RACE: 最快响应赢
 * - FALLBACK: 前一个失败才执行下一个
 */
class AgentCoordinator {

    /**
     * 协调多个 Agent 执行
     */
    suspend fun coordinate(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        if (agents.isEmpty()) return emptyList()

        return when (understanding.coordinationStrategy) {
            CoordinationStrategy.PARALLEL -> executeParallel(agents, understanding, context)
            CoordinationStrategy.SEQUENTIAL -> executeSequential(agents, understanding, context)
            CoordinationStrategy.RACE -> executeRace(agents, understanding, context)
            CoordinationStrategy.FALLBACK -> executeFallback(agents, understanding, context)
        }
    }

    /** 并行执行 - 同时请求所有 Agent */
    private suspend fun executeParallel(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        val timeout = understanding.timeout ?: DEFAULT_PARALLEL_TIMEOUT
        return withTimeout(timeout) {
            agents.map { agent ->
                async {
                    executeAgent(agent, understanding, context)
                }
            }.map { it.await() }
        }
    }

    /** 串行执行 - 依次执行 */
    private suspend fun executeSequential(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        return agents.map { agent ->
            executeAgent(agent, understanding, context)
        }
    }

    /** 赛跑模式 - 取最快成功响应 */
    private suspend fun executeRace(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        val timeout = understanding.timeout ?: DEFAULT_RACE_TIMEOUT
        val channel = Channel<AgentResponse>(agents.size)
        var winner: AgentResponse? = null

        withTimeout(timeout) {
            val jobs = agents.map { agent ->
                launch {
                    channel.send(executeAgent(agent, understanding, context))
                }
            }

            repeat(agents.size) {
                val result = channel.receive()
                if (result.success && winner == null) {
                    winner = result
                    jobs.forEach { it.cancel() }
                }
            }
        }
        return listOfNotNull(winner)
    }

    /** 回退模式 - 前一个失败才执行下一个 */
    private suspend fun executeFallback(
        agents: List<AppAgent>,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): List<AgentResponse> {
        for (agent in agents) {
            val result = executeAgent(agent, understanding, context)
            if (result.success) return listOf(result)
        }
        return emptyList()
    }

    /** 执行单个 Agent */
    private suspend fun executeAgent(
        agent: AppAgent,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): AgentResponse {
        return try {
            val request = buildRequest(agent, understanding, context)
            val response = agent.handleRequest(request)
            AgentResponse(agent = agent, payload = response, success = true)
        } catch (e: Exception) {
            AgentResponse(agent = agent, error = e, success = false)
        }
    }

    /** 构建请求消息 */
    private fun buildRequest(
        agent: AppAgent,
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): AgentMessage {
        return AgentMessage(
            messageId = generateMessageId(),
            from = AgentIdentity(AgentType.SYSTEM, "system_agent", "System Agent"),
            to = AgentIdentity(AgentType.APP, agent.agentId, agent.agentName),
            type = AgentMessageType.TASK_REQUEST,
            payload = TaskRequestPayload(
                intent = IntentInfo(
                    type = understanding.intentType.name,
                    action = understanding.action,
                    confidence = understanding.confidence
                ),
                entities = understanding.entities,
                userQuery = context.userQuery,
                expectedResponseType = ResponseType.A2UI_JSON
            )
        )
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }

    companion object {
        const val DEFAULT_PARALLEL_TIMEOUT = 10000L  // 10s
        const val DEFAULT_RACE_TIMEOUT = 5000L       // 5s
    }
}
