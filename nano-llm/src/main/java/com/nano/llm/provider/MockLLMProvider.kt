package com.nano.llm.provider

import com.nano.llm.model.*
import kotlinx.coroutines.delay

/**
 * Mock LLM 提供商 - 用于单元测试和开发调试
 *
 * 通过 responseMap 匹配用户输入中的关键词来返回预设响应，
 * 无需真实的 API Key 和网络调用。
 */
class MockLLMProvider(
    /** 关键词 → 响应映射，用户输入包含 key 时返回对应 value */
    private val responseMap: Map<String, String> = emptyMap(),
    /** 无匹配时的默认响应 */
    private val defaultResponse: String = "Mock response",
    /** 模拟延迟（毫秒），仿真网络响应时间 */
    private val delayMs: Long = 100
) : LLMProvider {

    override val providerType = ProviderType.MOCK
    override val isAvailable = true

    override suspend fun generate(request: LLMRequest): LLMResponse {
        delay(delayMs)

        val userMessage = request.messages
            .lastOrNull { it.role == MessageRole.USER }?.content ?: ""

        val content = responseMap.entries
            .firstOrNull { (key, _) -> userMessage.contains(key) }?.value
            ?: defaultResponse

        return LLMResponse(
            content = content,
            model = "mock-model",
            finishReason = "stop",
            usage = TokenUsage(
                promptTokens = request.messages.sumOf { it.content.length / 4 },
                completionTokens = content.length / 4,
                totalTokens = (request.messages.sumOf { it.content.length } + content.length) / 4
            )
        )
    }

    override suspend fun generateWithStream(request: LLMRequest, callback: StreamCallback): LLMResponse {
        return try {
            val response = generate(request)
            // 将响应分成小块逐步推送
            response.content.chunked(5).forEach { chunk ->
                callback.onToken(chunk)
                delay(50)
            }
            callback.onComplete(response)
            response
        } catch (e: Exception) {
            callback.onError(e)
            throw e
        }
    }
}
