package com.nano.llm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * LLM 消息角色
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    FUNCTION
}

/**
 * LLM 消息
 */
@Serializable
data class LLMMessage(
    val role: MessageRole,
    val content: String,
    val name: String? = null
)

/**
 * LLM 请求
 */
@Serializable
data class LLMRequest(
    val messages: List<LLMMessage>,
    val model: String? = null,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val stream: Boolean = false
)

/**
 * LLM 响应
 */
@Serializable
data class LLMResponse(
    val content: String,
    val model: String? = null,
    val finishReason: String? = null,
    val usage: TokenUsage? = null
)

/**
 * Token 使用统计
 */
@Serializable
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * LLM 配置
 */
data class LLMConfig(
    val providerType: ProviderType,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val model: String? = null,
    val timeout: Long = 30000
)

/**
 * LLM 提供商类型
 */
enum class ProviderType {
    OPENAI,
    CLAUDE,
    LOCAL,
    MOCK  // 用于测试
}

/**
 * 流式响应回调
 */
interface StreamCallback {
    fun onToken(token: String)
    fun onComplete(response: LLMResponse)
    fun onError(error: Throwable)
}
