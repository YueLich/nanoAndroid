package com.nano.llm.intent

import com.nano.kernel.NanoLog
import com.nano.llm.agent.AgentCapability
import com.nano.llm.model.*
import com.nano.llm.provider.LLMProvider
import com.nano.llm.provider.LLMProviderException
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
        private const val TAG = "IntentParser"

        /** 意图解析系统提示模板 */
        const val INTENT_PARSING_PROMPT = """你是一个意图解析助手，将用户的自然语言输入转换为结构化的意图对象。

## 已注册的 Agent
{{AGENT_CAPABILITIES}}

## 输出格式（严格 JSON，不要输出任何其他文本）
{
  "intentType": "APP_SEARCH | GENERAL_CHAT | SYSTEM_SETTINGS",
  "targetApps": ["已注册Agent ID列表"],
  "broadcastCapability": "SEARCH | ORDER | NAVIGATION | null",
  "action": "操作类型",
  "entities": {
    "intent": "具体意图",
    "expression": "计算表达式（如适用）",
    "title": "标题（如适用）",
    "content": "内容（如适用）"
  },
  "confidence": 0.0到1.0之间的浮点数,
  "coordinationStrategy": "RACE | PARALLEL | SEQUENTIAL | FALLBACK",
  "preferredLayout": "UNIFIED_LIST | SPLIT_VIEW | TABS",
  "needsClarification": true或false,
  "clarificationQuestion": "澄清问题（如需要）"
}

## 字段说明
- intentType: 意图类型
  * APP_SEARCH: 用户想使用某个具体应用（如计算器、笔记本）
  * GENERAL_CHAT: 通用对话，无法匹配到具体应用
  * SYSTEM_SETTINGS: 系统设置相关

- targetApps: 目标应用的 ID 列表
  * **必须**是已注册 Agent 列表中的 ID
  * 如："calculator"、"notepad"
  * 用户没有明确指定应用时为空数组

- broadcastCapability: 广播能力（用于多平台查询）
  * SEARCH: 查询能力（航班查询、商品搜索等）
  * ORDER: 订单能力（下单、支付等）
  * NAVIGATION: 导航能力（网页浏览等）
  * null: 不需要广播
  * 使用时 targetApps 通常为空，让系统自动选择所有支持该能力的Agent

- action: 具体操作
  * 计算器: "calculate"
  * 笔记本: "add_note"、"list_notes"、"view_note"、"edit_note"、"delete_note"
  * 航班: "search_flight"、"book_flight"

- entities: 提取的关键信息
  * expression: 数学表达式（用于计算器）
  * title: 笔记标题
  * content: 笔记内容
  * departure: 出发城市（用于航班查询）
  * arrival: 到达城市（用于航班查询）
  * date: 出发日期（用于航班查询，格式：YYYY-MM-DD）
  * intent: 原始意图描述

- confidence: 置信度（0.0-1.0）
  * 0.9+: 非常确定
  * 0.7-0.9: 比较确定
  * 0.5-0.7: 不太确定
  * < 0.5: 需要澄清

- coordinationStrategy: 协调策略
  * RACE: 简单查询，第一个响应即可
  * PARALLEL: 并行执行多个操作
  * SEQUENTIAL: 顺序执行（后续操作依赖前面结果）
  * FALLBACK: 兜底策略

- needsClarification: 是否需要澄清
  * 当用户意图不明确、缺少必要参数、或可能有多种理解时设为 true

## 示例

### 示例 1：简单计算
用户输入："计算 2 + 3"
输出：
{
  "intentType": "APP_SEARCH",
  "targetApps": ["calculator"],
  "broadcastCapability": null,
  "action": "calculate",
  "entities": {
    "expression": "2 + 3",
    "intent": "计算 2 + 3"
  },
  "confidence": 0.95,
  "coordinationStrategy": "RACE",
  "preferredLayout": "UNIFIED_LIST",
  "needsClarification": false,
  "clarificationQuestion": null
}

### 示例 2：新增笔记
用户输入："新增一个笔记，标题是今天的想法"
输出：
{
  "intentType": "APP_SEARCH",
  "targetApps": ["notepad"],
  "broadcastCapability": null,
  "action": "add_note",
  "entities": {
    "intent": "add_note",
    "title": "今天的想法",
    "content": ""
  },
  "confidence": 0.92,
  "coordinationStrategy": "RACE",
  "preferredLayout": "UNIFIED_LIST",
  "needsClarification": false,
  "clarificationQuestion": null
}

### 示例 3：列出所有笔记
用户输入："显示我的所有笔记"
输出：
{
  "intentType": "APP_SEARCH",
  "targetApps": ["notepad"],
  "broadcastCapability": null,
  "action": "list_notes",
  "entities": {
    "intent": "list_notes"
  },
  "confidence": 0.98,
  "coordinationStrategy": "RACE",
  "preferredLayout": "UNIFIED_LIST",
  "needsClarification": false,
  "clarificationQuestion": null
}

### 示例 4：航班查询（多平台协作）
用户输入："帮我查询明天北京到上海的机票"
输出：
{
  "intentType": "APP_SEARCH",
  "targetApps": [],
  "broadcastCapability": "SEARCH",
  "action": "search_flight",
  "entities": {
    "departure": "北京",
    "arrival": "上海",
    "date": "2026-02-03"
  },
  "confidence": 0.95,
  "coordinationStrategy": "PARALLEL",
  "preferredLayout": "UNIFIED_LIST",
  "needsClarification": false,
  "clarificationQuestion": null
}
说明：
- 使用 broadcastCapability: "SEARCH" 自动查找所有支持查询的Agent
- 使用 PARALLEL 策略并行查询多个平台（携程、南航、网页浏览器）
- 系统会自动去重、排序、合并多平台数据

### 示例 5：缺少参数的航班查询
用户输入："查询航班"
输出：
{
  "intentType": "APP_SEARCH",
  "targetApps": [],
  "broadcastCapability": "SEARCH",
  "action": "search_flight",
  "entities": {},
  "confidence": 0.6,
  "coordinationStrategy": "PARALLEL",
  "preferredLayout": "UNIFIED_LIST",
  "needsClarification": true,
  "clarificationQuestion": "好的，我可以帮您查询航班。请告诉我：\n1. 出发城市\n2. 到达城市\n3. 出发日期（如：明天、2026-02-03）"
}
说明：
- 用户意图明确（查询航班），但缺少必要参数
- 设置 needsClarification=true 要求用户补充信息
- clarificationQuestion 明确列出缺少的参数

### 示例 6：不明确的意图
用户输入："帮我处理一下"
输出：
{
  "intentType": "GENERAL_CHAT",
  "targetApps": [],
  "broadcastCapability": null,
  "action": "chat",
  "entities": {
    "intent": "帮我处理一下"
  },
  "confidence": 0.2,
  "coordinationStrategy": "FALLBACK",
  "preferredLayout": "UNIFIED_LIST",
  "needsClarification": true,
  "clarificationQuestion": "您想让我帮您处理什么？比如计算数学题、记录笔记等。"
}

## 重要提醒
1. targetApps 中的 ID 必须来自已注册 Agent 列表
2. 只输出 JSON，不要包含任何其他文本、解释或 markdown 标记
3. 确保 JSON 格式正确，所有字段都存在
4. confidence 必须是 0.0 到 1.0 之间的数字
5. 如果不确定用户意图，设置 needsClarification=true 并提供澄清问题"""
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
        NanoLog.i(TAG, "Parsing user input: \"$userInput\"")
        NanoLog.i(TAG, "Provider available: ${provider.isAvailable}")

        // 检查 Provider 是否可用
        if (!provider.isAvailable) {
            NanoLog.w(TAG, "⚠️  LLM Provider not available, returning fallback intent")
            return getFallbackIntent(userInput, "LLM Provider 未配置或不可用")
        }

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

        return try {
            NanoLog.i(TAG, "Calling LLM provider (${provider.providerType})...")
            val response = provider.generate(
                LLMRequest(
                    messages = messages,
                    temperature = 0.1f, // 低温度确保解析结果一致性
                    maxTokens = 512
                )
            )
            NanoLog.i(TAG, "LLM response received (${response.content.length} chars)")
            NanoLog.d(TAG, "LLM response: ${response.content.take(200)}")

            parseResponse(response.content)
        } catch (e: LLMProviderException) {
            NanoLog.e(TAG, "LLM Provider error: ${e.message}", e)
            getFallbackIntent(userInput, "LLM 调用失败: ${e.message}")
        } catch (e: Exception) {
            NanoLog.e(TAG, "Unexpected error during LLM call: ${e.message}", e)
            getFallbackIntent(userInput, "意图解析失败: ${e.message}")
        }
    }

    /**
     * 获取兜底意图
     */
    private fun getFallbackIntent(userInput: String, reason: String): IntentUnderstanding {
        NanoLog.w(TAG, "Returning fallback intent: $reason")
        return IntentUnderstanding(
            intentType = IntentType.GENERAL_CHAT,
            action = "chat",
            entities = mapOf("input" to userInput),
            confidence = 0.1f,
            coordinationStrategy = CoordinationStrategy.FALLBACK,
            preferredLayout = MergeLayout.UNIFIED_LIST,
            needsClarification = true,
            clarificationQuestion = "抱歉，我无法理解你的意思。原因：$reason\n\n你可以试试：\n- 计算 2 + 3\n- 新增笔记：学习 Android"
        )
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

            NanoLog.d(TAG, "Parsing JSON: ${cleanJson.take(200)}")
            val parsed = json.decodeFromString<ParsedIntent>(cleanJson)
            val understanding = parsed.toIntentUnderstanding()
            NanoLog.i(TAG, "✓ Intent parsed: ${understanding.intentType}, action=${understanding.action}, confidence=${understanding.confidence}")
            understanding
        } catch (e: Exception) {
            // 解析失败，返回兜底意图
            NanoLog.e(TAG, "Failed to parse LLM response: ${e.message}", e)
            NanoLog.e(TAG, "Raw response: ${jsonContent.take(500)}")
            IntentUnderstanding(
                intentType = IntentType.GENERAL_CHAT,
                action = "chat",
                confidence = 0.1f,
                coordinationStrategy = CoordinationStrategy.FALLBACK,
                preferredLayout = MergeLayout.UNIFIED_LIST,
                needsClarification = true,
                clarificationQuestion = "抱歉，我没有理解你的意思（解析错误：${e.message?.take(50)}）。\n\n你可以试试：\n- 计算 2 + 3\n- 新增笔记：学习 Android"
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
