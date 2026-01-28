package com.nano.llm.agent

import com.nano.llm.a2ui.*
import com.nano.llm.intent.*
import org.junit.Test
import org.junit.Assert.*

class ResponseAggregatorTest {

    private val aggregator = ResponseAggregator()

    private fun createMockAgent(id: String, name: String): AppAgent {
        return object : BaseAppAgent(id, name) {
            override val capabilities = setOf(AgentCapability.SEARCH)

            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = id,
                supportedIntents = listOf("TEST"),
                supportedEntities = emptyList(),
                exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )

            override suspend fun handleTaskRequest(request: TaskRequestPayload) =
                TaskResponsePayload(status = TaskStatus.SUCCESS)
        }
    }

    private fun createUnderstanding(): IntentUnderstanding {
        return IntentUnderstanding(
            intentType = IntentType.APP_SEARCH,
            action = "search",
            confidence = 0.9f,
            coordinationStrategy = CoordinationStrategy.PARALLEL,
            preferredLayout = MergeLayout.UNIFIED_LIST
        )
    }

    @Test
    fun testAggregateSingleRawDataResponse() {
        val agent = createMockAgent("agent1", "Agent 1")
        val response = AgentResponse(
            agent = agent,
            payload = TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                data = ResponseData("shops", listOf(
                    mapOf("name" to "Shop A", "price" to "10"),
                    mapOf("name" to "Shop B", "price" to "15")
                ))
            ),
            success = true
        )

        val result = aggregator.aggregate(listOf(response), createUnderstanding())

        assertEquals(1, result.participatingAgents.size)
        assertEquals("agent1", result.participatingAgents[0])
        assertTrue(result.a2uiResponses.isEmpty())
        assertEquals(1, result.rawDataResponses.size)
    }

    @Test
    fun testAggregateMultipleRawDataResponses() {
        val agent1 = createMockAgent("agent1", "美团")
        val agent2 = createMockAgent("agent2", "饿了么")

        val responses = listOf(
            AgentResponse(
                agent = agent1,
                payload = TaskResponsePayload(
                    status = TaskStatus.SUCCESS,
                    data = ResponseData("shops", listOf(
                        mapOf("name" to "Shop A", "price" to "10"),
                        mapOf("name" to "Shop B", "price" to "15")
                    ))
                ),
                success = true
            ),
            AgentResponse(
                agent = agent2,
                payload = TaskResponsePayload(
                    status = TaskStatus.SUCCESS,
                    data = ResponseData("shops", listOf(
                        mapOf("name" to "Shop A", "price" to "12"),
                        mapOf("name" to "Shop C", "price" to "20")
                    ))
                ),
                success = true
            )
        )

        val result = aggregator.aggregate(responses, createUnderstanding())

        val merged = result.mergedData!!
        assertEquals(2, result.participatingAgents.size)
        // Shop A 去重后应该只有 3 个唯一项
        assertEquals(3, merged.uniqueCount)
        assertEquals(4, merged.totalCount)
    }

    @Test
    fun testAggregateA2UIResponses() {
        val agent = createMockAgent("agent1", "Agent 1")
        val a2ui = A2UISpec(root = A2UIText(text = "Test"))

        val response = AgentResponse(
            agent = agent,
            payload = TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                a2ui = a2ui,
                message = "A2UI response"
            ),
            success = true
        )

        val result = aggregator.aggregate(listOf(response), createUnderstanding())

        assertEquals(1, result.a2uiResponses.size)
        assertEquals("agent1", result.a2uiResponses[0].second)
        assertTrue(result.rawDataResponses.isEmpty())
        assertNull(result.mergedData)
    }

    @Test
    fun testAggregateFailedResponses() {
        val agent1 = createMockAgent("agent1", "Agent 1")
        val agent2 = createMockAgent("agent2", "Agent 2")

        val responses = listOf(
            AgentResponse(
                agent = agent1,
                payload = TaskResponsePayload(status = TaskStatus.SUCCESS, message = "OK"),
                success = true
            ),
            AgentResponse(
                agent = agent2,
                error = RuntimeException("failed"),
                success = false
            )
        )

        val result = aggregator.aggregate(responses, createUnderstanding())

        assertEquals(1, result.participatingAgents.size)
        assertEquals(1, result.failedAgents.size)
        assertEquals("agent2", result.failedAgents[0])
        assertEquals(TaskStatus.SUCCESS, result.overallState)
    }

    @Test
    fun testAggregateAllFailed() {
        val agent = createMockAgent("agent1", "Agent 1")

        val responses = listOf(
            AgentResponse(agent = agent, error = RuntimeException("fail"), success = false)
        )

        val result = aggregator.aggregate(responses, createUnderstanding())

        assertEquals(TaskStatus.FAILED, result.overallState)
        assertTrue(result.participatingAgents.isEmpty())
        assertEquals("抱歉，没有找到相关结果", result.summary)
    }

    @Test
    fun testAggregatePartialSuccess() {
        val agent1 = createMockAgent("agent1", "Agent 1")
        val agent2 = createMockAgent("agent2", "Agent 2")

        val responses = listOf(
            AgentResponse(
                agent = agent1,
                payload = TaskResponsePayload(status = TaskStatus.SUCCESS, message = "OK"),
                success = true
            ),
            AgentResponse(
                agent = agent2,
                payload = TaskResponsePayload(status = TaskStatus.FAILED, message = "Error"),
                success = true
            )
        )

        val result = aggregator.aggregate(responses, createUnderstanding())

        assertEquals(TaskStatus.PARTIAL, result.overallState)
    }

    @Test
    fun testSummaryMultipleSources() {
        val agent1 = createMockAgent("agent1", "美团")
        val agent2 = createMockAgent("agent2", "饿了么")

        val responses = listOf(
            AgentResponse(
                agent = agent1,
                payload = TaskResponsePayload(
                    status = TaskStatus.SUCCESS,
                    data = ResponseData("shops", listOf(mapOf("name" to "A"), mapOf("name" to "B")))
                ),
                success = true
            ),
            AgentResponse(
                agent = agent2,
                payload = TaskResponsePayload(
                    status = TaskStatus.SUCCESS,
                    data = ResponseData("shops", listOf(mapOf("name" to "C")))
                ),
                success = true
            )
        )

        val result = aggregator.aggregate(responses, createUnderstanding())

        assertTrue(result.summary.contains("美团"))
        assertTrue(result.summary.contains("饿了么"))
        assertTrue(result.summary.contains("3"))
    }

    @Test
    fun testSummarySingleSource() {
        val agent = createMockAgent("agent1", "美团")

        val responses = listOf(
            AgentResponse(
                agent = agent,
                payload = TaskResponsePayload(
                    status = TaskStatus.SUCCESS,
                    message = "找到 5 家店",
                    data = ResponseData("shops", listOf(mapOf("name" to "A")))
                ),
                success = true
            )
        )

        val result = aggregator.aggregate(responses, createUnderstanding())
        assertEquals("找到 5 家店", result.summary)
    }

    @Test
    fun testFollowUpActionsAggregated() {
        val agent1 = createMockAgent("agent1", "Agent 1")
        val agent2 = createMockAgent("agent2", "Agent 2")

        val responses = listOf(
            AgentResponse(
                agent = agent1,
                payload = TaskResponsePayload(
                    status = TaskStatus.SUCCESS,
                    followUpActions = listOf(
                        FollowUpAction("f1", "按评分", "filter")
                    )
                ),
                success = true
            ),
            AgentResponse(
                agent = agent2,
                payload = TaskResponsePayload(
                    status = TaskStatus.SUCCESS,
                    followUpActions = listOf(
                        FollowUpAction("f2", "按距离", "filter")
                    )
                ),
                success = true
            )
        )

        val result = aggregator.aggregate(responses, createUnderstanding())

        assertEquals(2, result.allFollowUpActions.size)
    }

    @Test
    fun testMergeDataDedup() {
        val rawResponses = listOf(
            ResponseData("shops", listOf(
                mapOf("name" to "Shop A", "price" to "10"),
                mapOf("name" to "Shop B", "price" to "15")
            )) to "meituan",
            ResponseData("shops", listOf(
                mapOf("name" to "Shop A", "price" to "12"),
                mapOf("name" to "Shop C", "price" to "20")
            )) to "eleme"
        )

        val merged = aggregator.mergeData(rawResponses)

        assertEquals(4, merged.totalCount)
        assertEquals(3, merged.uniqueCount)  // A, B, C
        assertEquals(2, merged.sources.size)

        // Shop A 应该有两个来源
        val shopA = merged.items.find { it.key == "Shop A" }
        assertNotNull(shopA)
        assertEquals(2, shopA!!.sources.size)
        assertTrue(shopA.sources.contains("meituan"))
        assertTrue(shopA.sources.contains("eleme"))
    }

    @Test
    fun testMergeDataNoDuplicates() {
        val rawResponses = listOf(
            ResponseData("shops", listOf(
                mapOf("name" to "Shop A"),
                mapOf("name" to "Shop B")
            )) to "agent1",
            ResponseData("shops", listOf(
                mapOf("name" to "Shop C"),
                mapOf("name" to "Shop D")
            )) to "agent2"
        )

        val merged = aggregator.mergeData(rawResponses)

        assertEquals(4, merged.totalCount)
        assertEquals(4, merged.uniqueCount)
    }

    @Test
    fun testMergeDataEmptyResponses() {
        val merged = aggregator.mergeData(emptyList())
        assertEquals(0, merged.totalCount)
        assertEquals(0, merged.uniqueCount)
        assertTrue(merged.items.isEmpty())
    }

    @Test
    fun testMergeDataSingleSource() {
        val rawResponses = listOf(
            ResponseData("shops", listOf(
                mapOf("name" to "Shop A"),
                mapOf("name" to "Shop B")
            )) to "meituan"
        )

        val merged = aggregator.mergeData(rawResponses)

        assertEquals(2, merged.totalCount)
        assertEquals(2, merged.uniqueCount)
        assertEquals(1, merged.sources.size)
    }

    @Test
    fun testEmptyResponses() {
        val result = aggregator.aggregate(emptyList(), createUnderstanding())

        assertTrue(result.participatingAgents.isEmpty())
        assertTrue(result.failedAgents.isEmpty())
        assertEquals(TaskStatus.FAILED, result.overallState)
    }

    @Test
    fun testMergeDataByIdFallback() {
        // 当没有 name 字段时，使用 id 作为去重键
        val rawResponses = listOf(
            ResponseData("items", listOf(
                mapOf("id" to "001", "title" to "Item A")
            )) to "source1",
            ResponseData("items", listOf(
                mapOf("id" to "001", "title" to "Item A Copy")
            )) to "source2"
        )

        val merged = aggregator.mergeData(rawResponses)

        assertEquals(2, merged.totalCount)
        assertEquals(1, merged.uniqueCount)  // 同一 id 去重
    }
}
