package com.nano.llm.provider

import com.nano.llm.model.*

/**
 * LLM 提供商接口 - 统一不同 LLM API 的访问入口
 *
 * 所有 LLM 提供商（OpenAI、Claude、本地模型等）都实现此接口，
 * 使得上层代码可以透明切换底层模型。
 */
interface LLMProvider {

    /** 提供商类型 */
    val providerType: ProviderType

    /** 提供商是否可用（API Key 已配置等） */
    val isAvailable: Boolean

    /**
     * 生成文本响应（同步等待完整响应）
     *
     * @param request LLM 请求，包含消息历史、温度等参数
     * @return 完整的 LLM 响应
     * @throws LLMProviderException 调用失败时抛出
     */
    suspend fun generate(request: LLMRequest): LLMResponse

    /**
     * 流式生成文本响应
     *
     * 通过 StreamCallback 逐 token 推送，适用于实时交互场景。
     *
     * @param request LLM 请求
     * @param callback 流式回调，依次触发 onToken → onComplete / onError
     * @return 完整响应（等同于所有 token 拼接结果）
     */
    suspend fun generateWithStream(request: LLMRequest, callback: StreamCallback): LLMResponse
}

/**
 * LLM 提供商异常
 */
class LLMProviderException(
    message: String,
    val providerType: ProviderType,
    val statusCode: Int = 0,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * LLM 提供商工厂 - 根据配置创建对应 Provider
 */
object LLMProviderFactory {

    /**
     * 根据 LLMConfig 创建 Provider 实例
     */
    fun create(config: LLMConfig): LLMProvider = when (config.providerType) {
        ProviderType.OPENAI -> OpenAIProvider(config)
        ProviderType.CLAUDE -> ClaudeProvider(config)
        ProviderType.MOCK -> createMockProviderWithDemoData()
        ProviderType.LOCAL -> MockLLMProvider() // 本地模型占位
    }

    /**
     * 创建带演示数据的 MockLLMProvider
     * 支持 UI 提示的测试场景：计算器、笔记本
     */
    private fun createMockProviderWithDemoData(): MockLLMProvider {
        val demoResponseMap = mapOf(
            // 计算器关键词 → JSON (符合 IntentParser 要求)
            "计算" to """
            {
                "intentType": "APP_SEARCH",
                "targetApps": ["calculator"],
                "broadcastCapability": "SEARCH",
                "action": "calculate",
                "entities": {"expression": ""},
                "confidence": 0.95,
                "coordinationStrategy": "RACE",
                "preferredLayout": "UNIFIED_LIST",
                "needsClarification": false
            }
            """.trimIndent(),

            "算" to """
            {
                "intentType": "APP_SEARCH",
                "targetApps": ["calculator"],
                "action": "calculate",
                "entities": {"expression": ""},
                "confidence": 0.90,
                "coordinationStrategy": "RACE",
                "preferredLayout": "UNIFIED_LIST",
                "needsClarification": false
            }
            """.trimIndent(),

            // 笔记本关键词 → JSON
            "笔记" to """
            {
                "intentType": "APP_SEARCH",
                "targetApps": ["notepad"],
                "broadcastCapability": "SETTINGS",
                "action": "add_note",
                "entities": {"intent": "add_note", "title": "新笔记", "content": ""},
                "confidence": 0.92,
                "coordinationStrategy": "RACE",
                "preferredLayout": "UNIFIED_LIST",
                "needsClarification": false
            }
            """.trimIndent(),

            "新增" to """
            {
                "intentType": "APP_SEARCH",
                "targetApps": ["notepad"],
                "action": "add_note",
                "entities": {"intent": "add_note", "title": "无标题", "content": ""},
                "confidence": 0.88,
                "coordinationStrategy": "RACE",
                "preferredLayout": "UNIFIED_LIST",
                "needsClarification": false
            }
            """.trimIndent(),

            "记录" to """
            {
                "intentType": "APP_SEARCH",
                "targetApps": ["notepad"],
                "action": "add_note",
                "entities": {"intent": "add_note"},
                "confidence": 0.85,
                "coordinationStrategy": "RACE",
                "preferredLayout": "UNIFIED_LIST",
                "needsClarification": false
            }
            """.trimIndent(),

            // 航班查询（缺少参数） → 需要澄清
            "查询航班" to """
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
            """.trimIndent(),

            "航班" to """
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
            """.trimIndent(),

            // 航班查询（完整参数） → 直接查询
            "北京到上海" to """
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
                "needsClarification": false
            }
            """.trimIndent(),

            "机票" to """
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
                "clarificationQuestion": "好的，我可以帮您查询机票。请告诉我：\n1. 出发城市\n2. 到达城市\n3. 出发日期（如：明天、2026-02-03）"
            }
            """.trimIndent()
        )

        // 默认响应：提示用户正确用法（有效 JSON）
        val defaultResponse = """
        {
            "intentType": "GENERAL_CHAT",
            "targetApps": [],
            "action": "chat",
            "entities": {},
            "confidence": 0.5,
            "coordinationStrategy": "FALLBACK",
            "preferredLayout": "UNIFIED_LIST",
            "needsClarification": true,
            "clarificationQuestion": "我可以帮你：\n• 计算数学表达式 - 试试：'计算 2 + 3'\n• 管理笔记 - 试试：'新增笔记'\n• 查询航班 - 试试：'查询北京到上海的航班'"
        }
        """.trimIndent()

        return MockLLMProvider(
            responseMap = demoResponseMap,
            defaultResponse = defaultResponse,
            delayMs = 100
        )
    }
}
