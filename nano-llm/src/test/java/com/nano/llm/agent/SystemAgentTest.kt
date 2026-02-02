package com.nano.llm.agent

import com.nano.llm.a2ui.*
import com.nano.llm.intent.*
import com.nano.llm.model.*
import com.nano.llm.provider.LLMProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class SystemAgentTest {

    private val mockLLMProvider = object : LLMProvider {
        override val providerType = ProviderType.MOCK
        override val isAvailable = true

        override suspend fun generate(request: LLMRequest): LLMResponse {
            return LLMResponse(
                content = "Mock response",
                model = "mock-model",
                finishReason = "stop"
            )
        }

        override suspend fun generateWithStream(request: LLMRequest, callback: StreamCallback): LLMResponse {
            return generate(request)
        }
    }

    private fun createMockAgent(
        id: String,
        name: String,
        capabilities: Set<AgentCapability> = setOf(AgentCapability.SEARCH),
        response: TaskResponsePayload = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "Response from $name",
            data = ResponseData("test", listOf(mapOf("name" to id)))
        )
    ): AppAgent {
        return object : BaseAppAgent(id, name) {
            override val capabilities = capabilities

            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = id,
                supportedIntents = listOf("APP_SEARCH"),
                supportedEntities = listOf("food"),
                exampleQueries = listOf("搜索$id"),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )

            override suspend fun handleTaskRequest(request: TaskRequestPayload) = response
        }
    }

    private fun createSystemAgent(vararg agents: AppAgent): SystemAgent {
        val registry = AgentRegistry()
        agents.forEach { registry.registerAgent(it) }
        return SystemAgent(
            agentRegistry = registry,
            agentCoordinator = AgentCoordinator(),
            responseAggregator = ResponseAggregator(),
            llmProvider = mockLLMProvider
        )
    }

    private fun createRegistry(vararg agents: AppAgent): AgentRegistry {
        val registry = AgentRegistry()
        agents.forEach { registry.registerAgent(it) }
        return registry
    }

    @Test
    fun testSelectAgentsByTargetApps() {
        val agent1 = createMockAgent("meituan", "美团")
        val agent2 = createMockAgent("eleme", "饿了么")
        val registry = createRegistry(agent1, agent2)

        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("meituan"),
            action = "search",
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )

        val selected = systemAgent.selectAgents(understanding)

        assertEquals(1, selected.size)
        assertEquals("meituan", selected[0].agentId)
    }

    @Test
    fun testSelectAgentsByMultipleTargetApps() {
        val agent1 = createMockAgent("meituan", "美团")
        val agent2 = createMockAgent("eleme", "饿了么")
        val agent3 = createMockAgent("dianping", "大众点评")
        val registry = createRegistry(agent1, agent2, agent3)

        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("meituan", "eleme"),
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.TABS
        )

        val selected = systemAgent.selectAgents(understanding)

        assertEquals(2, selected.size)
        assertTrue(selected.any { it.agentId == "meituan" })
        assertTrue(selected.any { it.agentId == "eleme" })
    }

    @Test
    fun testSelectAgentsByBroadcastCapability() {
        val agent1 = createMockAgent("meituan", "美团", setOf(AgentCapability.SEARCH))
        val agent2 = createMockAgent("eleme", "饿了么", setOf(AgentCapability.SEARCH))
        val agent3 = createMockAgent("music", "音乐", setOf(AgentCapability.MEDIA))
        val registry = createRegistry(agent1, agent2, agent3)

        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            broadcastCapability = AgentCapability.SEARCH,
            action = "search_nearby",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )

        val selected = systemAgent.selectAgents(understanding)

        assertEquals(2, selected.size)
        assertTrue(selected.any { it.agentId == "meituan" })
        assertTrue(selected.any { it.agentId == "eleme" })
    }

    @Test
    fun testSelectAgentsSystemIntent() {
        val settingsAgent = createMockAgent("system_settings", "设置", setOf(AgentCapability.SETTINGS))
        val searchAgent = createMockAgent("meituan", "美团", setOf(AgentCapability.SEARCH))
        val registry = createRegistry(settingsAgent, searchAgent)

        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.SYSTEM_SETTINGS,
            action = "change_wifi",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )

        val selected = systemAgent.selectAgents(understanding)

        assertEquals(1, selected.size)
        assertEquals("system_settings", selected[0].agentId)
    }

    @Test
    fun testSelectAgentsFallbackToDefault() {
        val defaultAgent = createMockAgent("default", "Default Agent")
        val registry = createRegistry(defaultAgent)

        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.GENERAL_CHAT,
            action = "chat",
            confidence = 0.7f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )

        val selected = systemAgent.selectAgents(understanding)

        assertEquals(1, selected.size)
        assertEquals("default", selected[0].agentId)
    }

    @Test
    fun testSelectAgentsEmptyWhenNoMatch() {
        val agent = createMockAgent("meituan", "美团", setOf(AgentCapability.SEARCH))
        val registry = createRegistry(agent)

        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.GENERAL_CHAT,
            action = "chat",
            confidence = 0.5f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )

        val selected = systemAgent.selectAgents(understanding)

        assertTrue(selected.isEmpty())
    }

    @Test
    fun testSelectAgentsNonExistentTargetApp() {
        val agent = createMockAgent("meituan", "美团")
        val registry = createRegistry(agent)

        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("nonexistent_app"),
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )

        val selected = systemAgent.selectAgents(understanding)

        assertTrue(selected.isEmpty())
    }

    @Test
    fun testProcessRequestNoAgentsAvailable() = runTest {
        val registry = AgentRegistry()  // 空注册表
        val systemAgent = SystemAgent(registry, AgentCoordinator(), ResponseAggregator(), mockLLMProvider)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("meituan"),
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
        val context = ConversationContext(userQuery = "test")

        val result = systemAgent.processRequest(understanding, context)

        assertEquals(TaskStatus.FAILED, result.conversationState)
        assertTrue(result.message.contains("没有找到"))
    }

    @Test
    fun testProcessRequestSingleAgent() = runTest {
        val a2ui = A2UISpec(root = A2UIText(text = "Results"))
        val agent = createMockAgent("meituan", "美团", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "找到 5 家店",
            a2ui = a2ui,
            data = ResponseData("shops", listOf(mapOf("name" to "Shop A")))
        ))
        val systemAgent = createSystemAgent(agent)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("meituan"),
            action = "search",
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
        val context = ConversationContext(userQuery = "搜索黄焖鸡")

        val result = systemAgent.processRequest(understanding, context)

        assertEquals(TaskStatus.SUCCESS, result.conversationState)
        assertNotNull(result.a2ui)
        assertTrue(result.participatingAgents.contains("meituan"))
    }

    @Test
    fun testProcessRequestMultipleAgents() = runTest {
        val agent1 = createMockAgent("meituan", "美团", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "美团找到 3 家",
            data = ResponseData("shops", listOf(mapOf("name" to "Shop A"), mapOf("name" to "Shop B")))
        ))
        val agent2 = createMockAgent("eleme", "饿了么", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "饿了么找到 2 家",
            data = ResponseData("shops", listOf(mapOf("name" to "Shop C")))
        ))
        val systemAgent = createSystemAgent(agent1, agent2)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            broadcastCapability = AgentCapability.SEARCH,
            action = "search_nearby",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
        val context = ConversationContext(userQuery = "附近有什么好吃的")

        val result = systemAgent.processRequest(understanding, context)

        assertEquals(TaskStatus.SUCCESS, result.conversationState)
        assertEquals(2, result.participatingAgents.size)
        assertTrue(result.message.contains("美团"))
        assertTrue(result.message.contains("饿了么"))
    }

    @Test
    fun testProcessRequestWithFollowUpSuggestions() = runTest {
        val agent = createMockAgent("meituan", "美团", response = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "OK",
            followUpActions = listOf(
                FollowUpAction("f1", "按评分筛选", "filter"),
                FollowUpAction("f2", "按价格筛选", "filter")
            )
        ))
        val systemAgent = createSystemAgent(agent)

        val understanding = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("meituan"),
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
        val context = ConversationContext(userQuery = "search")

        val result = systemAgent.processRequest(understanding, context)

        assertEquals(2, result.followUpSuggestions.size)
        assertTrue(result.followUpSuggestions.contains("按评分筛选"))
        assertTrue(result.followUpSuggestions.contains("按价格筛选"))
    }
}
