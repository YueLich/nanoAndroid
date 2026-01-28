package com.nano.llm.intent

import com.nano.llm.agent.AgentCapability
import com.nano.llm.model.*
import com.nano.llm.provider.LLMProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 意图解析器 - 使用 LLM 将自然语言转换为结构化 IntentUnderstanding
 *
 * 工作流程：
 * 1. 构建包含已注册 Agent 能力信息的系统提示
 * 2. 调用 LLM 解析用户输入
 * 3. 将 LLM 的 JSON 响应解析为 IntentUnderstanding
 * 4. 解析失败时返回低置信度的兜底意图
 */
class IntentParser(private val provider: LLMProvider) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        /** 意图解析系统提示模板 */
        const val INTENT_PARSING_PROMPT = """你是一个智能助手的意图解析模块。将用户的自然语言输入解析为结构化的意图信息。

请按以下 JSON 格式输出（只输出 JSON，不要其他文字）：
{
    "intentType": "APP_SEARCH|APP_ORDER|APP_NAVIGATE|SYSTEM_SETTINGS|GENERAL_CHAT",
    "targetApps": ["app_id1", "app_id2"],
    "broadcastCapability": "SEARCH|ORDER|PAYMENT|MESSAGE|NAVIGATION|MEDIA|SETTINGS|null",
    "action": "action_name",
    "entities": {"key": "value"},
    "confidence": 0.0-1.0,
    "coordinationStrategy": "PARALLEL|SEQUENTIAL|RACE|FALLBACK",
    "preferredLayout": "TABS|UNIFIED_LIST|CARDS",
    "needsClarification": false,
    "clarificationQuestion": null
}

已注册的应用及能力：
{{AGENT_CAPABILITIES}}

规则:
- targetApps: 用户明确提到某个应用时填充，否则为空数组
- broadcastCapability: 用户未指定应用但需要搜索时，广播到所有支持该能力的应用
- entities: 提取用户提到的关键实体（如菜品名、地点、价格范围等）
- needsClarification: 当意图不够明确无法确定目标时为 true
- coordinationStrategy: 搜索类请求用 PARALLEL，依赖上一步结果用 SEQUENTIAL，简单查询用 RACE"""
    }

    /**
     * 解析用户输入为意图
     *
     * @param userInput 用户原始输入文本
     * @param agentCapabilities 已注册 Agent 的能力描述列表
     * @param conversationHistory 多轮对话历史（用于上下文理解）
     * @return 解析出的结构化意图
     */
    suspend fun parse(
        userInput: String,
        agentCapabilities: List<String> = emptyList(),
        conversationHistory: List<LLMMessage> = emptyList()
    ): IntentUnderstanding {
        val capabilities = if (agentCapabilities.isEmpty()) {
            "无已注册应用"
        } else {
            agentCapabilities.joinToString("\n")
        }

        val systemPrompt = INTENT_PARSING_PROMPT.replace("{{AGENT_CAPABILITIES}}", capabilities)

        val messages = mutableListOf<LLMMessage>()
        messages.add(LLMMessage(role = MessageRole.SYSTEM, content = systemPrompt))
        messages.addAll(conversationHistory)
        messages.add(LLMMessage(role = MessageRole.USER, content = userInput))

        val response = provider.generate(
            LLMRequest(
                messages = messages,
                temperature = 0.1f, // 低温度确保解析结果一致性
                maxTokens = 512
            )
        )

        return parseResponse(response.content)
    }

    /**
     * 将 LLM 返回的 JSON 文本解析为 IntentUnderstanding
     *
     * 如果解析失败，返回低置信度的通用意图并请求澄清。
     */
    fun parseResponse(jsonContent: String): IntentUnderstanding {
        return try {
            val cleanJson = jsonContent.trim()
                .removeSurrounding("```json", "```")
                .removeSurrounding("```", "```")
                .trim()

            val parsed = json.decodeFromString<ParsedIntent>(cleanJson)
            parsed.toIntentUnderstanding()
        } catch (e: Exception) {
            // 解析失败，返回兜底意图
            IntentUnderstanding(
                intentType = IntentType.GENERAL_CHAT,
                action = "chat",
                confidence = 0.1f,
                coordinationStrategy = CoordinationStrategy.FALLBACK,
                preferredLayout = MergeLayout.UNIFIED_LIST,
                needsClarification = true,
                clarificationQuestion = "抱歉，我没有理解你的意思。你能详细说说吗？"
            )
        }
    }

    /**
     * LLM 响应的中间解析结构
     */
    @Serializable
    private data class ParsedIntent(
        val intentType: String,
        val targetApps: List<String> = emptyList(),
        val broadcastCapability: String? = null,
        val action: String,
        val entities: Map<String, String> = emptyMap(),
        val confidence: Float,
        val coordinationStrategy: String = "PARALLEL",
        val preferredLayout: String = "UNIFIED_LIST",
        val needsClarification: Boolean = false,
        val clarificationQuestion: String? = null
    ) {
        fun toIntentUnderstanding(): IntentUnderstanding {
            return IntentUnderstanding(
                intentType = IntentType.valueOf(intentType),
                targetApps = targetApps,
                broadcastCapability = broadcastCapability?.let {
                    if (it == "null") null else AgentCapability.valueOf(it)
                },
                action = action,
                entities = entities,
                confidence = confidence.coerceIn(0f, 1f),
                coordinationStrategy = CoordinationStrategy.valueOf(coordinationStrategy),
                preferredLayout = MergeLayout.valueOf(preferredLayout),
                needsClarification = needsClarification,
                clarificationQuestion = clarificationQuestion
            )
        }
    }
}
