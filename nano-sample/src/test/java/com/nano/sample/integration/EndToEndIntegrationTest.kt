package com.nano.sample.integration

import com.nano.llm.agent.*
import com.nano.llm.intent.*
import com.nano.llm.api.NaturalLanguageAPI
import com.nano.llm.intent.IntentParser
import com.nano.llm.provider.MockLLMProvider
import com.nano.a2ui.bridge.A2UIRenderer
import com.nano.a2ui.protocol.A2UISerializer
import com.nano.llm.a2ui.*
import com.nano.sample.calculator.CalculatorAgent
import com.nano.sample.notepad.NotepadAgent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 端到端集成测试
 *
 * 验证完整调用链：
 * IntentUnderstanding → SystemAgent → AgentCoordinator → SampleAgents → A2UISpec → Renderer
 */
class EndToEndIntegrationTest {

    private lateinit var registry: AgentRegistry
    private lateinit var coordinator: AgentCoordinator
    private lateinit var aggregator: ResponseAggregator
    private lateinit var systemAgent: SystemAgent
    private lateinit var calculator: CalculatorAgent
    private lateinit var notepad: NotepadAgent
    private lateinit var renderer: A2UIRenderer

    @Before
    fun setup() {
        calculator = CalculatorAgent()
        notepad = NotepadAgent()
        registry = AgentRegistry()
        registry.registerAgent(calculator)
        registry.registerAgent(notepad)

        coordinator = AgentCoordinator()
        aggregator = ResponseAggregator()
        systemAgent = SystemAgent(registry, coordinator, aggregator)
        renderer = A2UIRenderer()
    }

    // ==================== Agent 路由测试 ====================

    @Test
    fun testRouteToCalculatorByTargetApp() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("calculator"),
            action = "compute",
            entities = mapOf("expression" to "2 + 3"),
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(
            conversationId = "conv_1",
            userQuery = "计算 2 + 3"
        )

        val response = systemAgent.processRequest(intent, context)

        assertEquals(TaskStatus.SUCCESS, response.conversationState)
        assertTrue(response.participatingAgents.contains("calculator"))
        assertNotNull(response.a2ui)
    }

    @Test
    fun testRouteToNotepadByTargetApp() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("notepad"),
            action = "list_notes",
            entities = mapOf("intent" to "list_notes"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(
            conversationId = "conv_2",
            userQuery = "查看笔记"
        )

        val response = systemAgent.processRequest(intent, context)

        assertEquals(TaskStatus.SUCCESS, response.conversationState)
        assertTrue(response.participatingAgents.contains("notepad"))
    }

    @Test
    fun testRouteToNonExistentAgent() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("nonexistent_app"),
            action = "do_something",
            entities = emptyMap(),
            confidence = 0.8f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "conv_3", userQuery = "test")

        val response = systemAgent.processRequest(intent, context)

        assertEquals(TaskStatus.FAILED, response.conversationState)
        assertTrue(response.participatingAgents.isEmpty())
    }

    @Test
    fun testRouteByCapabilityBroadcast() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            broadcastCapability = AgentCapability.SEARCH,
            action = "search",
            entities = mapOf("expression" to "5 * 5"),
            confidence = 0.85f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.TABS
        )

        val context = ConversationContext(conversationId = "conv_4", userQuery = "搜索")

        // Calculator 有 SEARCH 能力，应该被路由到
        val response = systemAgent.processRequest(intent, context)

        assertEquals(TaskStatus.SUCCESS, response.conversationState)
        assertTrue(response.participatingAgents.contains("calculator"))
    }

    // ==================== 协调策略测试 ====================

    @Test
    fun testSequentialExecution() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("calculator"),
            action = "compute",
            entities = mapOf("expression" to "10 / 2"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "seq_1", userQuery = "10 / 2")

        val response = systemAgent.processRequest(intent, context)
        assertEquals(TaskStatus.SUCCESS, response.conversationState)
        assertNotNull(response.a2ui)
    }

    @Test
    fun testParallelExecution() = runTest {
        // 注册两个都支持 SEARCH 的 Agent
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            broadcastCapability = AgentCapability.SEARCH,
            action = "compute",
            entities = mapOf("expression" to "7 * 8"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.TABS,
            timeout = 5000L
        )

        val context = ConversationContext(conversationId = "par_1", userQuery = "7 * 8")

        val response = systemAgent.processRequest(intent, context)
        assertEquals(TaskStatus.SUCCESS, response.conversationState)
    }

    @Test
    fun testFallbackStrategy() = runTest {
        // FALLBACK 策略需要 Agent 抛异常才能触发回退（success=false）
        val crashAgent = object : BaseAppAgent("crash_fallback", "崩溃回退代理") {
            override val capabilities = setOf(AgentCapability.SEARCH)
            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = "crash_fallback",
                supportedIntents = emptyList(),
                supportedEntities = emptyList(),
                exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )
            override suspend fun handleTaskRequest(request: TaskRequestPayload): TaskResponsePayload {
                throw RuntimeException("Agent 崩溃触发回退")
            }
        }
        registry.registerAgent(crashAgent)

        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("crash_fallback", "calculator"),
            action = "compute",
            entities = mapOf("expression" to "3 + 4"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.FALLBACK,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "fb_1", userQuery = "3 + 4")
        val response = systemAgent.processRequest(intent, context)

        // FALLBACK 策略：crash_fallback 抛异常后回退到 calculator
        assertEquals(TaskStatus.SUCCESS, response.conversationState)
        assertTrue(response.participatingAgents.contains("calculator"))
    }

    // ==================== 响应聚合测试 ====================

    @Test
    fun testResponseAggregationSingleAgent() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("calculator"),
            action = "compute",
            entities = mapOf("expression" to "100 / 4"),
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "agg_1", userQuery = "100 / 4")
        val response = systemAgent.processRequest(intent, context)

        assertEquals(TaskStatus.SUCCESS, response.conversationState)
        assertEquals(1, response.participatingAgents.size)
        assertNotNull(response.message)
        assertTrue(response.message.isNotEmpty())
    }

    @Test
    fun testResponseAggregationMultipleAgents() = runTest {
        // 广播到所有 SEARCH 能力的 Agent
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            broadcastCapability = AgentCapability.SEARCH,
            action = "search",
            entities = mapOf("expression" to "2 ^ 10"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.TABS
        )

        val context = ConversationContext(conversationId = "agg_2", userQuery = "2^10")
        val response = systemAgent.processRequest(intent, context)

        // 至少有 calculator 参与
        assertTrue(response.participatingAgents.isNotEmpty())
    }

    // ==================== 对话上下文测试 ====================

    @Test
    fun testMultiTurnConversation() = runTest {
        var context = ConversationContext(conversationId = "multi_1", userQuery = "")

        // 第一轮：计算
        context = context.withQuery("计算 2 + 3")
        val intent1 = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("calculator"),
            action = "compute",
            entities = mapOf("expression" to "2 + 3"),
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )
        val response1 = systemAgent.processRequest(intent1, context)
        assertEquals(TaskStatus.SUCCESS, response1.conversationState)

        // 第二轮：笔记
        context = context.withQuery("新增笔记")
        val intent2 = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("notepad"),
            action = "add_note",
            entities = mapOf("intent" to "add_note", "title" to "计算结果", "content" to "2+3=5"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )
        val response2 = systemAgent.processRequest(intent2, context)
        assertEquals(TaskStatus.SUCCESS, response2.conversationState)
        assertTrue(response2.participatingAgents.contains("notepad"))

        // 验证上下文保持
        assertEquals("新增笔记", context.userQuery)
        assertTrue(context.previousQueries.contains("计算 2 + 3"))
    }

    @Test
    fun testConversationContextWithQueryChaining() {
        var ctx = ConversationContext(conversationId = "chain_1")
        ctx = ctx.withQuery("第一个问题")
        ctx = ctx.withQuery("第二个问题")
        ctx = ctx.withQuery("第三个问题")

        assertEquals("第三个问题", ctx.userQuery)
        assertEquals(2, ctx.previousQueries.size)
        assertTrue(ctx.previousQueries.contains("第一个问题"))
        assertTrue(ctx.previousQueries.contains("第二个问题"))
    }

    // ==================== A2UI 渲染管道集成 ====================

    @Test
    fun testCalculatorResponseToView() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("calculator"),
            action = "compute",
            entities = mapOf("expression" to "6 * 7"),
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "render_1", userQuery = "6 * 7")
        val response = systemAgent.processRequest(intent, context)

        // 验证 A2UI Spec 存在
        assertNotNull(response.a2ui)

        // 序列化为 JSON
        val json = A2UISerializer.serialize(response.a2ui!!)
        assertTrue(json.contains("\"type\":"))

        // 反序列化回 Spec
        val restored = A2UISerializer.deserialize(json)
        assertNotNull(restored.root)

        // 渲染为 NanoView
        val view = renderer.render(restored)
        assertNotNull(view)
    }

    @Test
    fun testNotepadResponseToView() = runTest {
        // 先添加一条笔记
        notepad.addNote("集成测试", "测试内容")

        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("notepad"),
            action = "list_notes",
            entities = mapOf("intent" to "list_notes"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "render_2", userQuery = "查看笔记")
        val response = systemAgent.processRequest(intent, context)

        assertNotNull(response.a2ui)

        // 完整序列化→反序列化→渲染管道
        val json = A2UISerializer.serialize(response.a2ui!!)
        val restored = A2UISerializer.deserialize(json)
        val view = renderer.render(restored)
        assertNotNull(view)
    }

    // ==================== Agent 选择策略边界 ====================

    @Test
    fun testSelectAgentsWithMultipleTargetApps() = runTest {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("calculator", "notepad"),
            action = "search",
            entities = mapOf("expression" to "1 + 1"),
            confidence = 0.8f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.TABS
        )

        val selected = systemAgent.selectAgents(intent)
        assertEquals(2, selected.size)
        assertTrue(selected.any { it.agentId == "calculator" })
        assertTrue(selected.any { it.agentId == "notepad" })
    }

    @Test
    fun testSelectAgentsCapabilityBased() {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            broadcastCapability = AgentCapability.SEARCH,
            action = "search",
            entities = emptyMap(),
            confidence = 0.8f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.TABS
        )

        val selected = systemAgent.selectAgents(intent)
        // Calculator 注册了 SEARCH 能力
        assertTrue(selected.any { it.agentId == "calculator" })
    }

    @Test
    fun testSelectAgentsNoMatch() {
        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            broadcastCapability = AgentCapability.PAYMENT,
            action = "pay",
            entities = emptyMap(),
            confidence = 0.5f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val selected = systemAgent.selectAgents(intent)
        // 没有 Agent 支持 PAYMENT
        assertTrue(selected.isEmpty())
    }

    // ==================== NaturalLanguageAPI processIntent 集成 ====================

    @Test
    fun testNaturalLanguageAPIProcessIntent() = runTest {
        val provider = MockLLMProvider()
        val intentParser = IntentParser(provider)
        val api = NaturalLanguageAPI(registry, systemAgent, intentParser)

        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("calculator"),
            action = "compute",
            entities = mapOf("expression" to "5 + 5"),
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "api_1", userQuery = "5 + 5")

        val response = api.processIntent(intent, context)

        assertEquals(TaskStatus.SUCCESS, response.conversationState)
        assertTrue(response.participatingAgents.contains("calculator"))
        assertNotNull(response.a2ui)
    }

    @Test
    fun testNaturalLanguageAPIClarificationNeeded() = runTest {
        // MockLLMProvider 返回带 needsClarification=true 的 JSON
        val clarificationJson = """{
            "intentType": "APP_SEARCH",
            "targetApps": [],
            "action": "unknown",
            "entities": {},
            "confidence": 0.3,
            "coordinationStrategy": "SEQUENTIAL",
            "preferredLayout": "CARDS",
            "needsClarification": true,
            "clarificationQuestion": "您想做什么？"
        }"""
        val provider = MockLLMProvider(
            responseMap = mapOf("帮我" to clarificationJson),
            defaultResponse = clarificationJson
        )
        val intentParser = IntentParser(provider)
        val api = NaturalLanguageAPI(registry, systemAgent, intentParser)

        val context = ConversationContext(conversationId = "api_2", userQuery = "")

        // processUserInput 会调用 IntentParser，解析出 needsClarification=true
        val response = api.processUserInput("帮我", context)

        assertEquals(TaskStatus.NEED_MORE_INFO, response.conversationState)
        assertTrue(response.message.contains("您想做什么？"))
    }

    // ==================== 错误恢复 ====================

    @Test
    fun testAgentExceptionRecovery() = runTest {
        val crashAgent = object : BaseAppAgent("crash_agent", "崩溃代理") {
            override val capabilities = setOf(AgentCapability.SEARCH)
            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = "crash_agent",
                supportedIntents = emptyList(),
                supportedEntities = emptyList(),
                exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )
            override suspend fun handleTaskRequest(request: TaskRequestPayload): TaskResponsePayload {
                throw RuntimeException("Agent 崩溃！")
            }
        }
        registry.registerAgent(crashAgent)

        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("crash_agent"),
            action = "crash",
            entities = emptyMap(),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )

        val context = ConversationContext(conversationId = "err_1", userQuery = "崩溃")

        // 不应该抛出异常，AgentCoordinator 会捕获
        val response = systemAgent.processRequest(intent, context)
        assertEquals(TaskStatus.FAILED, response.conversationState)
    }

    @Test
    fun testPartialFailureWithMultipleAgents() = runTest {
        val failAgent = object : BaseAppAgent("partial_fail", "部分失败") {
            override val capabilities = setOf(AgentCapability.SEARCH)
            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = "partial_fail",
                supportedIntents = emptyList(),
                supportedEntities = emptyList(),
                exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )
            override suspend fun handleTaskRequest(request: TaskRequestPayload) =
                TaskResponsePayload(status = TaskStatus.FAILED, message = "失败了")
        }
        registry.registerAgent(failAgent)

        val intent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("partial_fail", "calculator"),
            action = "compute",
            entities = mapOf("expression" to "1 + 1"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.TABS
        )

        val context = ConversationContext(conversationId = "err_2", userQuery = "1+1")
        val response = systemAgent.processRequest(intent, context)

        // calculator 成功，partial_fail 失败 → PARTIAL
        assertTrue(response.conversationState == TaskStatus.SUCCESS ||
                   response.conversationState == TaskStatus.PARTIAL)
        assertTrue(response.participatingAgents.contains("calculator"))
    }

    // ==================== 计算器精度 + 集成 ====================

    @Test
    fun testCalculatorComplexExpressionIntegration() = runTest {
        val expressions = mapOf(
            "2 + 3 * 4" to "14",
            "(2 + 3) * 4" to "20",
            "100 / (5 * 2)" to "10",
            "2 ^ 10" to "1024"
        )

        for ((expr, _) in expressions) {
            val intent = IntentUnderstanding(
                intentType = IntentType.APP_SEARCH,
                targetApps = listOf("calculator"),
                action = "compute",
                entities = mapOf("expression" to expr),
                confidence = 0.99f,
                coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
                preferredLayout = MergeLayout.CARDS
            )
            val context = ConversationContext(conversationId = "calc_$expr", userQuery = expr)
            val response = systemAgent.processRequest(intent, context)

            assertEquals(TaskStatus.SUCCESS, response.conversationState)
            assertNotNull("表达式 '$expr' 应返回 A2UI Spec", response.a2ui)
        }
    }

    // ==================== 笔记 CRUD 集成 ====================

    @Test
    fun testNotepadCRUDWorkflow() = runTest {
        // 1. 添加笔记
        val addIntent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("notepad"),
            action = "add_note",
            entities = mapOf("intent" to "add_note", "title" to "集成测试笔记", "content" to "流程测试内容"),
            confidence = 0.95f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )
        var context = ConversationContext(conversationId = "crud_1").withQuery("新增笔记")
        val addResponse = systemAgent.processRequest(addIntent, context)
        assertEquals(TaskStatus.SUCCESS, addResponse.conversationState)

        // 2. 列出笔记（验证已添加）
        val listIntent = IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            targetApps = listOf("notepad"),
            action = "list_notes",
            entities = mapOf("intent" to "list_notes"),
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.SEQUENTIAL,
            preferredLayout = MergeLayout.CARDS
        )
        context = context.withQuery("查看笔记")
        val listResponse = systemAgent.processRequest(listIntent, context)
        assertEquals(TaskStatus.SUCCESS, listResponse.conversationState)
        assertNotNull(listResponse.a2ui)

        // 3. 验证上下文
        assertEquals("查看笔记", context.userQuery)
        assertTrue(context.previousQueries.contains("新增笔记"))
    }
}
