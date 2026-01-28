package com.nano.llm.api

import com.nano.llm.agent.*
import com.nano.llm.intent.IntentParser
import com.nano.llm.intent.IntentUnderstanding
import com.nano.llm.model.LLMMessage

/**
 * NaturalLanguageAPI - 自然语言处理入口
 *
 * 封装从用户输入到最终响应的完整流程：
 *   用户输入 → 意图解析 → Agent 选择 → 协调执行 → 响应聚合 → 最终响应
 *
 * 这是 nano-llm 对外暴露的核心 API，其他模块通过此类与 LLM 功能交互。
 */
class NaturalLanguageAPI(
    private val agentRegistry: AgentRegistry,
    private val systemAgent: SystemAgent,
    private val intentParser: IntentParser
) {

    /**
     * 处理用户自然语言输入 - 核心入口方法
     *
     * @param userInput 用户输入文本
     * @param context 对话上下文（包含会话 ID、历史信息等）
     * @param conversationHistory 多轮对话历史消息（供意图解析器使用）
     * @return SystemAgentResponse 最终响应，包含消息、A2UI 规格和后续建议
     */
    suspend fun processUserInput(
        userInput: String,
        context: ConversationContext,
        conversationHistory: List<LLMMessage> = emptyList()
    ): SystemAgentResponse {
        // 1. 获取已注册 Agent 的能力描述（供意图解析器参考）
        val capabilities = agentRegistry.getAllCapabilities()
        val capabilityDescriptions = capabilities.map { cap ->
            "- ${cap.agentId}(${cap.supportedIntents.joinToString(",")}): ${cap.exampleQueries.joinToString(", ")}"
        }

        // 2. 调用意图解析器，将自然语言转换为结构化意图
        val understanding = intentParser.parse(
            userInput = userInput,
            agentCapabilities = capabilityDescriptions,
            conversationHistory = conversationHistory
        )

        // 3. 如果意图不够明确，直接返回澄清请求
        if (understanding.needsClarification) {
            return SystemAgentResponse(
                message = understanding.clarificationQuestion ?: "请您详细说明一下？",
                conversationState = TaskStatus.NEED_MORE_INFO
            )
        }

        // 4. 交给 SystemAgent 执行 Agent 调度和响应聚合
        val updatedContext = context.withQuery(userInput)
        return systemAgent.processRequest(understanding, updatedContext)
    }

    /**
     * 直接处理已解析的意图（跳过意图解析步骤）
     *
     * 当调用者已经自行解析意图，或需要精确控制路由时使用此方法。
     *
     * @param understanding 已解析的意图结构
     * @param context 对话上下文
     * @return SystemAgentResponse
     */
    suspend fun processIntent(
        understanding: IntentUnderstanding,
        context: ConversationContext
    ): SystemAgentResponse {
        return systemAgent.processRequest(understanding, context)
    }
}
