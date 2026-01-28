package com.nano.llm.agent

import com.nano.llm.intent.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class AgentCoordinatorTest {

    private fun createTestAgent(
        id: String,
        name: String,
        capabilities: Set<AgentCapability> = setOf(AgentCapability.SEARCH),
        response: TaskResponsePayload = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "Response from $name",
            data = ResponseData("test", listOf(mapOf("name" to id)))
        ),
        shouldFail: Boolean = false
    ): AppAgent {
        return object : BaseAppAgent(id, name) {
            override val capabilities = capabilities

            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = id,
                supportedIntents = listOf("TEST"),
                supportedEntities = emptyList(),
                exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )

            override suspend fun handleTaskRequest(request: TaskRequestPayload): TaskResponsePayload {
                if (shouldFail) throw RuntimeException("Agent $id failed")
                return response
            }
        }
    }

    private fun createUnderstanding(
        strategy: CoordinationStrategy = CoordinationStrategy.PARALLEL,
        timeout: Long? = null
    ): IntentUnderstanding {
        return IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = strategy,
            preferredLayout = MergeLayout.UNIFIED_LIST,
            timeout = timeout
        )
    }

    private val context = ConversationContext(userQuery = "test query")
    private val coordinator = AgentCoordinator()

    @Test
    fun testParallelExecution() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1"),
            createTestAgent("agent2", "Agent 2"),
            createTestAgent("agent3", "Agent 3")
        )
        val understanding = createUnderstanding(CoordinationStrategy.PARALLEL)

        val results = coordinator.coordinate(agents, understanding, context)

        assertEquals(3, results.size)
        assertTrue(results.all { it.success })
    }

    @Test
    fun testParallelResultOrder() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1"),
            createTestAgent("agent2", "Agent 2")
        )
        val understanding = createUnderstanding(CoordinationStrategy.PARALLEL)

        val results = coordinator.coordinate(agents, understanding, context)

        // 结果顺序应与输入顺序一致
        assertEquals("agent1", results[0].agent.agentId)
        assertEquals("agent2", results[1].agent.agentId)
    }

    @Test
    fun testSequentialExecution() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1"),
            createTestAgent("agent2", "Agent 2")
        )
        val understanding = createUnderstanding(CoordinationStrategy.SEQUENTIAL)

        val results = coordinator.coordinate(agents, understanding, context)

        assertEquals(2, results.size)
        assertTrue(results.all { it.success })
    }

    @Test
    fun testFallbackFirstSucceeds() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1"),
            createTestAgent("agent2", "Agent 2")
        )
        val understanding = createUnderstanding(CoordinationStrategy.FALLBACK)

        val results = coordinator.coordinate(agents, understanding, context)

        assertEquals(1, results.size)
        assertEquals("agent1", results[0].agent.agentId)
    }

    @Test
    fun testFallbackFirstFails() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1", shouldFail = true),
            createTestAgent("agent2", "Agent 2")
        )
        val understanding = createUnderstanding(CoordinationStrategy.FALLBACK)

        val results = coordinator.coordinate(agents, understanding, context)

        assertEquals(1, results.size)
        assertEquals("agent2", results[0].agent.agentId)
    }

    @Test
    fun testFallbackAllFail() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1", shouldFail = true),
            createTestAgent("agent2", "Agent 2", shouldFail = true)
        )
        val understanding = createUnderstanding(CoordinationStrategy.FALLBACK)

        val results = coordinator.coordinate(agents, understanding, context)

        assertTrue(results.isEmpty())
    }

    @Test
    fun testRaceExecution() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1"),
            createTestAgent("agent2", "Agent 2")
        )
        val understanding = createUnderstanding(CoordinationStrategy.RACE)

        val results = coordinator.coordinate(agents, understanding, context)

        // Race 只返回一个成功结果
        assertEquals(1, results.size)
        assertTrue(results[0].success)
    }

    @Test
    fun testEmptyAgentList() = runTest {
        val understanding = createUnderstanding()
        val results = coordinator.coordinate(emptyList(), understanding, context)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testParallelWithOneFailure() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1"),
            createTestAgent("agent2", "Agent 2", shouldFail = true),
            createTestAgent("agent3", "Agent 3")
        )
        val understanding = createUnderstanding(CoordinationStrategy.PARALLEL)

        val results = coordinator.coordinate(agents, understanding, context)

        assertEquals(3, results.size)
        assertEquals(2, results.count { it.success })
        assertEquals(1, results.count { !it.success })
    }

    @Test
    fun testSequentialWithFailure() = runTest {
        val agents = listOf(
            createTestAgent("agent1", "Agent 1"),
            createTestAgent("agent2", "Agent 2", shouldFail = true),
            createTestAgent("agent3", "Agent 3")
        )
        val understanding = createUnderstanding(CoordinationStrategy.SEQUENTIAL)

        val results = coordinator.coordinate(agents, understanding, context)

        // 串行执行，失败的也记录
        assertEquals(3, results.size)
        assertFalse(results[1].success)
        assertNotNull(results[1].error)
    }

    @Test
    fun testResponsePayloadPreserved() = runTest {
        val expectedResponse = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            message = "Custom message",
            data = ResponseData("shops", listOf(mapOf("name" to "Shop X")))
        )
        val agent = createTestAgent("agent1", "Agent 1", response = expectedResponse)
        val understanding = createUnderstanding()

        val results = coordinator.coordinate(listOf(agent), understanding, context)

        assertEquals("Custom message", results[0].payload?.message)
        assertEquals("Shop X", results[0].payload?.data?.items?.get(0)?.get("name"))
    }

    @Test
    fun testSingleAgentAllStrategies() = runTest {
        val agent = createTestAgent("solo", "Solo Agent")
        val strategies = listOf(
            CoordinationStrategy.PARALLEL,
            CoordinationStrategy.SEQUENTIAL,
            CoordinationStrategy.RACE,
            CoordinationStrategy.FALLBACK
        )

        for (strategy in strategies) {
            val understanding = createUnderstanding(strategy)
            val results = coordinator.coordinate(listOf(agent), understanding, context)
            assertTrue("Strategy $strategy should return result", results.isNotEmpty())
            assertTrue("Strategy $strategy result should succeed", results[0].success)
        }
    }
}
