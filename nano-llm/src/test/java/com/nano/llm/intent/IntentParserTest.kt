package com.nano.llm.intent

import com.nano.llm.agent.AgentCapability
import com.nano.llm.model.*
import com.nano.llm.provider.MockLLMProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class IntentParserTest {

    /** 构建返回预设 JSON 的 MockProvider */
    private fun createProvider(responseJson: String): MockLLMProvider {
        return MockLLMProvider(
            responseMap = mapOf("__ANY__" to responseJson),
            defaultResponse = responseJson
        )
    }

    private fun createParser(responseJson: String): IntentParser {
        return IntentParser(createProvider(responseJson))
    }

    @Test
    fun testParseSearchIntent() = runTest {
        val json = """
        {
            "intentType": "APP_SEARCH",
            "targetApps": [],
            "broadcastCapability": "SEARCH",
            "action": "search_food",
            "entities": {"keyword": "黄焖鸡", "location": "附近"},
            "confidence": 0.92,
            "coordinationStrategy": "PARALLEL",
            "preferredLayout": "UNIFIED_LIST",
            "needsClarification": false
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("帮我搜索附近的黄焖鸡")

        assertEquals(IntentType.APP_SEARCH, result.intentType)
        assertEquals(AgentCapability.SEARCH, result.broadcastCapability)
        assertEquals("search_food", result.action)
        assertEquals("黄焖鸡", result.entities["keyword"])
        assertEquals("附近", result.entities["location"])
        assertAlmostEquals(0.92f, result.confidence, 0.01f)
        assertEquals(CoordinationStrategy.PARALLEL, result.coordinationStrategy)
        assertFalse(result.needsClarification)
    }

    @Test
    fun testParseOrderIntent() = runTest {
        val json = """
        {
            "intentType": "APP_ORDER",
            "targetApps": ["meituan"],
            "action": "place_order",
            "entities": {"item": "宫保鸡丁", "quantity": "2"},
            "confidence": 0.88,
            "coordinationStrategy": "SEQUENTIAL",
            "preferredLayout": "CARDS"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("在美团点两份宫保鸡丁")

        assertEquals(IntentType.APP_ORDER, result.intentType)
        assertEquals(listOf("meituan"), result.targetApps)
        assertEquals("place_order", result.action)
        assertEquals(CoordinationStrategy.SEQUENTIAL, result.coordinationStrategy)
        assertEquals(MergeLayout.CARDS, result.preferredLayout)
    }

    @Test
    fun testParseSystemSettingsIntent() = runTest {
        val json = """
        {
            "intentType": "SYSTEM_SETTINGS",
            "targetApps": [],
            "action": "change_wifi",
            "entities": {"network": "家里WiFi"},
            "confidence": 0.95,
            "coordinationStrategy": "RACE",
            "preferredLayout": "UNIFIED_LIST"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("连接家里的WiFi")

        assertEquals(IntentType.SYSTEM_SETTINGS, result.intentType)
        assertTrue(result.isSystemIntent())
        assertEquals("change_wifi", result.action)
    }

    @Test
    fun testParseGeneralChat() = runTest {
        val json = """
        {
            "intentType": "GENERAL_CHAT",
            "targetApps": [],
            "action": "chat",
            "entities": {},
            "confidence": 0.7,
            "coordinationStrategy": "FALLBACK",
            "preferredLayout": "UNIFIED_LIST"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("你好，聊聊天吧")

        assertEquals(IntentType.GENERAL_CHAT, result.intentType)
        assertFalse(result.isSystemIntent())
        assertEquals(CoordinationStrategy.FALLBACK, result.coordinationStrategy)
    }

    @Test
    fun testParseNeedsClarification() = runTest {
        val json = """
        {
            "intentType": "GENERAL_CHAT",
            "targetApps": [],
            "action": "clarify",
            "entities": {},
            "confidence": 0.3,
            "coordinationStrategy": "FALLBACK",
            "preferredLayout": "UNIFIED_LIST",
            "needsClarification": true,
            "clarificationQuestion": "你想搜索什么？"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("嗯...那个...")

        assertTrue(result.needsClarification)
        assertEquals("你想搜索什么？", result.clarificationQuestion)
    }

    @Test
    fun testParseFallbackOnInvalidJSON() = runTest {
        val parser = createParser("这不是有效的JSON")
        val result = parser.parse("任意输入")

        // 解析失败应返回兜底意图
        assertEquals(IntentType.GENERAL_CHAT, result.intentType)
        assertEquals(0.1f, result.confidence)
        assertTrue(result.needsClarification)
        assertNotNull(result.clarificationQuestion)
    }

    @Test
    fun testParseFallbackOnMissingFields() = runTest {
        // 缺少必需字段
        val parser = createParser("""{"intentType": "INVALID_TYPE"}""")
        val result = parser.parse("测试")

        assertEquals(IntentType.GENERAL_CHAT, result.intentType)
        assertTrue(result.needsClarification)
    }

    @Test
    fun testParseTargetMultipleApps() = runTest {
        val json = """
        {
            "intentType": "APP_SEARCH",
            "targetApps": ["meituan", "eleme", "dianping"],
            "action": "search_restaurant",
            "entities": {"cuisine": "日料"},
            "confidence": 0.85,
            "coordinationStrategy": "PARALLEL",
            "preferredLayout": "TABS"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("在美团饿了么大众点评搜索日料")

        assertEquals(3, result.targetApps.size)
        assertTrue(result.targetApps.contains("meituan"))
        assertTrue(result.targetApps.contains("eleme"))
        assertEquals(MergeLayout.TABS, result.preferredLayout)
    }

    @Test
    fun testParseConfidenceClamped() = runTest {
        // confidence 超出范围应被截断
        val json = """
        {
            "intentType": "APP_SEARCH",
            "targetApps": [],
            "action": "search",
            "entities": {},
            "confidence": 1.5,
            "coordinationStrategy": "PARALLEL",
            "preferredLayout": "UNIFIED_LIST"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("测试")

        assertEquals(1.0f, result.confidence)
    }

    @Test
    fun testParseConfidenceNegativeClamped() = runTest {
        val json = """
        {
            "intentType": "APP_SEARCH",
            "targetApps": [],
            "action": "search",
            "entities": {},
            "confidence": -0.5,
            "coordinationStrategy": "PARALLEL",
            "preferredLayout": "UNIFIED_LIST"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("测试")

        assertEquals(0.0f, result.confidence)
    }

    @Test
    fun testParseNullBroadcastCapability() = runTest {
        val json = """
        {
            "intentType": "APP_NAVIGATE",
            "targetApps": ["maps"],
            "broadcastCapability": "null",
            "action": "navigate",
            "entities": {"destination": "北京南站"},
            "confidence": 0.9,
            "coordinationStrategy": "RACE",
            "preferredLayout": "UNIFIED_LIST"
        }
        """.trimIndent()

        val parser = createParser(json)
        val result = parser.parse("导航到北京南站")

        assertNull(result.broadcastCapability)
        assertEquals(listOf("maps"), result.targetApps)
    }

    @Test
    fun testParseWithConversationHistory() = runTest {
        val json = """
        {
            "intentType": "APP_ORDER",
            "targetApps": ["meituan"],
            "action": "reorder",
            "entities": {"item": "之前的菜"},
            "confidence": 0.8,
            "coordinationStrategy": "SEQUENTIAL",
            "preferredLayout": "CARDS"
        }
        """.trimIndent()

        val provider = MockLLMProvider(defaultResponse = json)
        val parser = IntentParser(provider)

        val history = listOf(
            LLMMessage(MessageRole.USER, "美团点菜"),
            LLMMessage(MessageRole.ASSISTANT, "你想点什么？")
        )

        val result = parser.parse("再点一次", conversationHistory = history)

        assertEquals(IntentType.APP_ORDER, result.intentType)
        assertEquals("reorder", result.action)
    }

    @Test
    fun testParseResponseDirectly() {
        val parser = IntentParser(MockLLMProvider())

        val json = """
        {
            "intentType": "APP_SEARCH",
            "targetApps": ["test"],
            "action": "direct_test",
            "entities": {"key": "value"},
            "confidence": 0.75,
            "coordinationStrategy": "PARALLEL",
            "preferredLayout": "UNIFIED_LIST"
        }
        """.trimIndent()

        val result = parser.parseResponse(json)

        assertEquals(IntentType.APP_SEARCH, result.intentType)
        assertEquals("direct_test", result.action)
        assertEquals("value", result.entities["key"])
    }

    @Test
    fun testParseResponseWithCodeBlock() {
        val parser = IntentParser(MockLLMProvider())

        // LLM 可能将 JSON 包裹在代码块中
        val wrappedJson = """```json
        {
            "intentType": "GENERAL_CHAT",
            "targetApps": [],
            "action": "wrapped",
            "entities": {},
            "confidence": 0.6,
            "coordinationStrategy": "FALLBACK",
            "preferredLayout": "UNIFIED_LIST"
        }
        ```"""

        val result = parser.parseResponse(wrappedJson)

        assertEquals(IntentType.GENERAL_CHAT, result.intentType)
        assertEquals("wrapped", result.action)
    }

    private fun assertAlmostEquals(expected: Float, actual: Float, delta: Float) {
        assertTrue(
            "Expected $expected but was $actual (delta $delta)",
            Math.abs(expected - actual) <= delta
        )
    }
}
