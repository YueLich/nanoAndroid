package com.nano.llm.api

import com.nano.llm.agent.*
import com.nano.llm.a2ui.*
import com.nano.llm.intent.*
import com.nano.llm.model.*
import com.nano.llm.provider.MockLLMProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NaturalLanguageAPITest {

    /** 构建测试用的 NaturalLanguageAPI */
    private fun createAPI(
        agents: List<AppAgent> = emptyList(),
        llmResponse: String = """{"intentType":"GENERAL_CHAT","targetApps":[],"action":"chat","entities":{},"confidence":0.5,"coordinationStrategy":"FALLBACK","preferredLayout":"UNIFIED_LIST"}"""
    ): NaturalLanguageAPI {
        val registry = AgentRegistry()
        agents.forEach { registry.registerAgent(it) }

        val provider = MockLLMProvider(defaultResponse = llmResponse)
        val intentParser = IntentParser(provider)
        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator())

        return NaturalLanguageAPI(
            agentRegistry = registry,
            systemAgent = systemAgent,
            intentParser = intentParser
        )
    }

    /** 创建 Mock Agent */
    private fun createMockAgent(
        id: String,
        name: String,
        capabilities: Set<AgentCapability> = setOf(AgentCapability.SEARCH),
        response: TaskResponsePayload = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "来自 $name 的响应",
            data = ResponseData("test", listOf(mapOf("name" to id)))
        )
    ): AppAgent {
        return object : BaseAppAgent(id, name) {
            override val capabilities = capabilities

            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = id,
                supportedIntents = listOf("APP_SEARCH"),
                supportedEntities = listOf("food"),
                exampleQueries = listOf("搜索$name"),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )

            override suspend fun handleTaskRequest(request: TaskRequestPayload) = response
        }
    }

    @Test
    fun testProcessUserInputWithNoAgents() = runTest {
        // 意图解析返回 APP_SEARCH，但没有注册的 Agent
        val json = """{"intentType":"APP_SEARCH","targetApps":["nonexistent"],"action":"search","entities":{},"confidence":0.9,"coordinationStrategy":"PARALLEL","preferredLayout":"UNIFIED_LIST"}"""
        val api = createAPI(agents = emptyList(), llmResponse = json)

        val context = ConversationContext(conversationId = "test-1", userQuery = "")
        val result = api.processUserInput("搜索美食", context)

        assertEquals(TaskStatus.FAILED, result.conversationState)
        assertTrue(result.message.contains("没有找到"))
    }

    @Test
    fun testProcessUserInputNeedsClarification() = runTest {
        val json = """{"intentType":"GENERAL_CHAT","targetApps":[],"action":"clarify","entities":{},"confidence":0.2,"coordinationStrategy":"FALLBACK","preferredLayout":"UNIFIED_LIST","needsClarification":true,"clarificationQuestion":"你能说得更具体一点吗？"}"""
        val api = createAPI(llmResponse = json)

        val context = ConversationContext(conversationId = "test-2")
        val result = api.processUserInput("嗯...", context)

        assertEquals(TaskStatus.NEED_MORE_INFO, result.conversationState)
        assertEquals("你能说得更具体一点吗？", result.message)
    }

    @Test
    fun testProcessUserInputWithSingleAgent() = runTest {
        val json = """{"intentType":"APP_SEARCH","targetApps":["meituan"],"action":"search","entities":{"keyword":"黄焖鸡"},"confidence":0.92,"coordinationStrategy":"PARALLEL","preferredLayout":"UNIFIED_LIST"}"""

        val agent = createMockAgent("meituan", "美团", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "美团找到了5家黄焖鸡",
            data = ResponseData("shops", listOf(
                mapOf("name" to "Shop A", "rating" to "4.5"),
                mapOf("name" to "Shop B", "rating" to "4.2")
            ))
        ))

        val api = createAPI(agents = listOf(agent), llmResponse = json)
        val context = ConversationContext(conversationId = "test-3")

        val result = api.processUserInput("美团搜索黄焖鸡", context)

        assertEquals(TaskStatus.SUCCESS, result.conversationState)
        assertTrue(result.participatingAgents.contains("meituan"))
    }

    @Test
    fun testProcessUserInputWithMultipleAgents() = runTest {
        val json = """{"intentType":"APP_SEARCH","targetApps":[],"broadcastCapability":"SEARCH","action":"search","entities":{"keyword":"日料"},"confidence":0.9,"coordinationStrategy":"PARALLEL","preferredLayout":"TABS"}"""

        val agent1 = createMockAgent("meituan", "美团", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "美团：3家日料",
            data = ResponseData("shops", listOf(mapOf("name" to "日料A")))
        ))
        val agent2 = createMockAgent("eleme", "饿了么", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "饿了么：2家日料",
            data = ResponseData("shops", listOf(mapOf("name" to "日料B")))
        ))

        val api = createAPI(agents = listOf(agent1, agent2), llmResponse = json)
        val context = ConversationContext(conversationId = "test-4")

        val result = api.processUserInput("搜索附近日料", context)

        assertEquals(TaskStatus.SUCCESS, result.conversationState)
        assertEquals(2, result.participatingAgents.size)
    }

    @Test
    fun testProcessIntentDirect() = runTest {
        val agent = createMockAgent("meituan", "美团")
        val api = createAPI(agents = listOf(agent))

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("meituan"),
            action = "search",
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
        val context = ConversationContext(conversationId = "test-5", userQuery = "直接调用")

        val result = api.processIntent(understanding, context)

        assertEquals(TaskStatus.SUCCESS, result.conversationState)
        assertTrue(result.participatingAgents.contains("meituan"))
    }

    @Test
    fun testProcessIntentWithFollowUpActions() = runTest {
        val agent = createMockAgent("meituan", "美团", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "搜索完成",
            followUpActions = listOf(
                FollowUpAction("f1", "按评分排序", "sort"),
                FollowUpAction("f2", "价格区间筛选", "filter")
            )
        ))
        val api = createAPI(agents = listOf(agent))

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("meituan"),
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
        val context = ConversationContext(userQuery = "搜索美食")

        val result = api.processIntent(understanding, context)

        assertEquals(2, result.followUpSuggestions.size)
        assertTrue(result.followUpSuggestions.contains("按评分排序"))
        assertTrue(result.followUpSuggestions.contains("价格区间筛选"))
    }

    @Test
    fun testProcessIntentSystemSettings() = runTest {
        val settingsAgent = createMockAgent(
            "system_settings", "设置",
            capabilities = setOf(AgentCapability.SETTINGS),
            response = TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "已切换WiFi"
            )
        )
        val api = createAPI(agents = listOf(settingsAgent))

        val understanding = IntentUnderstanding(
            intentType = IntentType.SYSTEM_SETTINGS,
            action = "change_wifi",
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.RACE,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
        val context = ConversationContext(userQuery = "切换WiFi")

        val result = api.processIntent(understanding, context)

        assertEquals(TaskStatus.SUCCESS, result.conversationState)
        assertTrue(result.participatingAgents.contains("system_settings"))
    }

    @Test
    fun testProcessUserInputCapabilityDescriptionsIncluded() = runTest {
        // 验证 Agent 能力描述被传递给意图解析器
        val json = """{"intentType":"GENERAL_CHAT","targetApps":[],"action":"chat","entities":{},"confidence":0.5,"coordinationStrategy":"FALLBACK","preferredLayout":"UNIFIED_LIST"}"""

        // 创建有能力描述的 Agent
        val agent = object : BaseAppAgent("test_app", "测试应用") {
            override val capabilities = setOf(AgentCapability.SEARCH)

            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = "test_app",
                supportedIntents = listOf("APP_SEARCH"),
                supportedEntities = listOf("food", "restaurant"),
                exampleQueries = listOf("搜索美食", "找餐厅"),
                responseTypes = setOf(ResponseType.A2UI_JSON)
            )

            override suspend fun handleTaskRequest(request: TaskRequestPayload) = TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "OK"
            )
        }

        val api = createAPI(agents = listOf(agent), llmResponse = json)
        val context = ConversationContext(userQuery = "")

        // 不应抛出异常，能力信息被正确传递
        val result = api.processUserInput("测试", context)
        assertNotNull(result)
    }
}
