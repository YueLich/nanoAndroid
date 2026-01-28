package com.nano.llm.provider

import com.nano.llm.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI API 提供商
 *
 * 使用 OkHttp 调用 OpenAI Chat Completions API。
 * 支持 GPT-4o-mini（默认）等模型。
 */
class OpenAIProvider(private val config: LLMConfig) : LLMProvider {

    override val providerType = ProviderType.OPENAI
    override val isAvailable = config.apiKey?.isNotEmpty() == true

    private val client = OkHttpClient.Builder()
        .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = config.baseUrl ?: "https://api.openai.com"
    private val model = config.model ?: "gpt-4o-mini"

    // ==================== API 请求/响应 数据模型 ====================

    @Serializable
    private data class ApiRequest(
        val model: String,
        val messages: List<ApiMessage>,
        val temperature: Float,
        @SerialName("max_tokens") val maxTokens: Int,
        val stream: Boolean = false
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
        val choices: List<ApiChoice> = emptyList(),
        val usage: ApiUsage? = null
    )

    @Serializable
    private data class ApiChoice(
        val message: ApiMessage? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    private data class ApiUsage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0,
        @SerialName("total_tokens") val totalTokens: Int = 0
    )

    override suspend fun generate(request: LLMRequest): LLMResponse {
        if (!isAvailable) {
            throw LLMProviderException("OpenAI API Key 未配置", providerType)
        }

        val apiRequest = ApiRequest(
            model = request.model ?: model,
            messages = request.messages.map { ApiMessage(it.role.name.lowercase(), it.content) },
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            stream = false
        )

        val body = json.encodeToString(ApiRequest.serializer(), apiRequest)
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw LLMProviderException(
                message = "OpenAI API error: ${response.body?.string()}",
                providerType = providerType,
                statusCode = response.code
            )
        }

        val apiResponse = json.decodeFromString<ApiResponse>(response.body?.string() ?: "")
        val choice = apiResponse.choices.firstOrNull()
            ?: throw LLMProviderException("OpenAI API returned no choices", providerType)

        return LLMResponse(
            content = choice.message?.content ?: "",
            model = apiResponse.model,
            finishReason = choice.finishReason,
            usage = apiResponse.usage?.let { usage ->
                TokenUsage(usage.promptTokens, usage.completionTokens, usage.totalTokens)
            }
        )
    }

    override suspend fun generateWithStream(request: LLMRequest, callback: StreamCallback): LLMResponse {
        // 当前版本使用非流式调用作为兜底；流式 SSE 解析待后续实现
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
