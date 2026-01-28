package com.nano.llm.provider

import com.nano.llm.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Claude (Anthropic) API 提供商
 *
 * 使用 OkHttp 调用 Anthropic Messages API。
 * 注意：Anthropic 的 system 消息需要单独传入，不作为 messages 数组的一部分。
 */
class ClaudeProvider(private val config: LLMConfig) : LLMProvider {

    override val providerType = ProviderType.CLAUDE
    override val isAvailable = config.apiKey?.isNotEmpty() == true

    private val client = OkHttpClient.Builder()
        .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = config.baseUrl ?: "https://api.anthropic.com"
    private val model = config.model ?: "claude-3-5-sonnet-20241022"
    private val apiVersion = "2023-06-01"

    // ==================== API 请求/响应 数据模型 ====================

    @Serializable
    private data class ApiRequest(
        val model: String,
        val max_tokens: Int,
        val messages: List<ApiMessage>,
        val system: String? = null
    )

    @Serializable
    private data class ApiMessage(
        val role: String,
        val content: String
    )

    @Serializable
    private data class ApiResponse(
        val id: String = "",
        val model: String = "",
        val content: List<ApiContentBlock> = emptyList(),
        val stop_reason: String? = null,
        val usage: ApiUsage? = null
    )

    @Serializable
    private data class ApiContentBlock(
        val type: String = "",
        val text: String = ""
    )

    @Serializable
    private data class ApiUsage(
        val input_tokens: Int = 0,
        val output_tokens: Int = 0
    )

    override suspend fun generate(request: LLMRequest): LLMResponse {
        if (!isAvailable) {
            throw LLMProviderException("Claude API Key 未配置", providerType)
        }

        // Anthropic API 将 system 消息单独提取
        val systemMessage = request.messages
            .find { it.role == MessageRole.SYSTEM }?.content
        val userMessages = request.messages
            .filter { it.role != MessageRole.SYSTEM }
            .map { ApiMessage(it.role.name.lowercase(), it.content) }

        val apiRequest = ApiRequest(
            model = request.model ?: model,
            max_tokens = request.maxTokens,
            messages = userMessages,
            system = systemMessage
        )

        val body = json.encodeToString(ApiRequest.serializer(), apiRequest)
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1/messages")
            .header("x-api-key", config.apiKey ?: "")
            .header("anthropic-version", apiVersion)
            .header("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw LLMProviderException(
                message = "Claude API error: ${response.body?.string()}",
                providerType = providerType,
                statusCode = response.code
            )
        }

        val apiResponse = json.decodeFromString<ApiResponse>(response.body?.string() ?: "")
        val textContent = apiResponse.content.firstOrNull { it.type == "text" }
            ?: throw LLMProviderException("Claude API returned no text content", providerType)

        return LLMResponse(
            content = textContent.text,
            model = apiResponse.model,
            finishReason = apiResponse.stop_reason,
            usage = apiResponse.usage?.let { usage ->
                TokenUsage(usage.input_tokens, usage.output_tokens, usage.input_tokens + usage.output_tokens)
            }
        )
    }

    override suspend fun generateWithStream(request: LLMRequest, callback: StreamCallback): LLMResponse {
        return try {
            val response = generate(request)
            callback.onComplete(response)
            response
        } catch (e: Exception) {
            callback.onError(e)
            throw e
        }
    }
}
