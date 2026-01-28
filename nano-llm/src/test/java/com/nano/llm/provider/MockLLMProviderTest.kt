package com.nano.llm.provider

import com.nano.llm.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MockLLMProviderTest {

    @Test
    fun testProviderType() {
        val provider = MockLLMProvider()
        assertEquals(ProviderType.MOCK, provider.providerType)
    }

    @Test
    fun testIsAlwaysAvailable() {
        val provider = MockLLMProvider()
        assertTrue(provider.isAvailable)
    }

    @Test
    fun testGenerateDefaultResponse() = runTest {
        val provider = MockLLMProvider(defaultResponse = "默认响应")
        val request = LLMRequest(
            messages = listOf(LLMMessage(MessageRole.USER, "你好"))
        )

        val response = provider.generate(request)

        assertEquals("默认响应", response.content)
        assertEquals("mock-model", response.model)
        assertEquals("stop", response.finishReason)
    }

    @Test
    fun testGenerateWithMappedResponse() = runTest {
        val provider = MockLLMProvider(
            responseMap = mapOf(
                "搜索" to "找到10个结果",
                "天气" to "明天晴天"
            ),
            defaultResponse = "未知"
        )

        val searchRequest = LLMRequest(
            messages = listOf(LLMMessage(MessageRole.USER, "帮我搜索美食"))
        )
        val weatherRequest = LLMRequest(
            messages = listOf(LLMMessage(MessageRole.USER, "查看天气预报"))
        )
        val unknownRequest = LLMRequest(
            messages = listOf(LLMMessage(MessageRole.USER, "其他问题"))
        )

        assertEquals("找到10个结果", provider.generate(searchRequest).content)
        assertEquals("明天晴天", provider.generate(weatherRequest).content)
        assertEquals("未知", provider.generate(unknownRequest).content)
    }

    @Test
    fun testGenerateTokenUsageCalculation() = runTest {
        val provider = MockLLMProvider(defaultResponse = "Hello")
        val request = LLMRequest(
            messages = listOf(LLMMessage(MessageRole.USER, "test message"))
        )

        val response = provider.generate(request)

        assertNotNull(response.usage)
        assertTrue(response.usage!!.promptTokens > 0)
        assertTrue(response.usage!!.completionTokens > 0)
        assertEquals(response.usage!!.totalTokens, response.usage!!.promptTokens + response.usage!!.completionTokens)
    }

    @Test
    fun testGenerateWithStreamCallbackSequence() = runTest {
        val provider = MockLLMProvider(defaultResponse = "Stream test response", delayMs = 10)
        val request = LLMRequest(
            messages = listOf(LLMMessage(MessageRole.USER, "stream"))
        )

        val tokens = mutableListOf<String>()
        var completedResponse: LLMResponse? = null
        var caughtError: Throwable? = null

        val callback = object : StreamCallback {
            override fun onToken(token: String) { tokens.add(token) }
            override fun onComplete(response: LLMResponse) { completedResponse = response }
            override fun onError(error: Throwable) { caughtError = error }
        }

        val result = provider.generateWithStream(request, callback)

        // 验证流式 token 被逐块推送
        assertTrue(tokens.isNotEmpty())
        // 所有 token 拼接后等于完整响应
        assertEquals(result.content, tokens.joinToString(""))
        // onComplete 被调用
        assertNotNull(completedResponse)
        assertEquals(result.content, completedResponse!!.content)
        // 无错误
        assertNull(caughtError)
    }

    @Test
    fun testGenerateWithMultipleMessages() = runTest {
        val provider = MockLLMProvider(defaultResponse = "Response")
        val request = LLMRequest(
            messages = listOf(
                LLMMessage(MessageRole.SYSTEM, "你是助手"),
                LLMMessage(MessageRole.USER, "第一问"),
                LLMMessage(MessageRole.ASSISTANT, "第一答"),
                LLMMessage(MessageRole.USER, "第二问")
            )
        )

        val response = provider.generate(request)

        // 匹配最后一条 USER 消息
        assertEquals("Response", response.content)
        assertNotNull(response.usage)
        // prompt tokens 应该包含所有消息
        assertTrue(response.usage!!.promptTokens > 0)
    }

    @Test
    fun testGenerateWithEmptyMessages() = runTest {
        val provider = MockLLMProvider(defaultResponse = "Empty")
        val request = LLMRequest(messages = emptyList())

        val response = provider.generate(request)

        assertEquals("Empty", response.content)
    }

    @Test
    fun testGenerateMatchesFirstKeyword() = runTest {
        // 当用户输入包含多个关键词，匹配第一个
        val provider = MockLLMProvider(
            responseMap = mapOf(
                "A" to "matched A",
                "B" to "matched B"
            )
        )
        val request = LLMRequest(
            messages = listOf(LLMMessage(MessageRole.USER, "包含A和B"))
        )

        val response = provider.generate(request)
        assertEquals("matched A", response.content)
    }
}
