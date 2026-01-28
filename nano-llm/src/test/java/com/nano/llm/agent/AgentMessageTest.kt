package com.nano.llm.agent

import org.junit.Test
import org.junit.Assert.*

class AgentMessageTest {

    @Test
    fun testCreateAgentIdentity() {
        val identity = AgentIdentity(AgentType.SYSTEM, "system_agent", "System Agent")
        assertEquals(AgentType.SYSTEM, identity.type)
        assertEquals("system_agent", identity.id)
        assertEquals("System Agent", identity.name)
    }

    @Test
    fun testAgentTypeValues() {
        assertEquals(2, AgentType.values().size)
        assertNotNull(AgentType.SYSTEM)
        assertNotNull(AgentType.APP)
    }

    @Test
    fun testCreateIntentInfo() {
        val intent = IntentInfo("APP_SEARCH", "search", 0.95f)
        assertEquals("APP_SEARCH", intent.type)
        assertEquals("search", intent.action)
        assertEquals(0.95f, intent.confidence)
    }

    @Test
    fun testCreateTaskRequestPayload() {
        val intent = IntentInfo("APP_SEARCH", "search", 0.95f)
        val payload = TaskRequestPayload(
            intent = intent,
            entities = mapOf("food" to "黄焖鸡"),
            userQuery = "搜索黄焖鸡",
            expectedResponseType = ResponseType.A2UI_JSON
        )

        assertEquals("APP_SEARCH", payload.intent.type)
        assertEquals("黄焖鸡", payload.entities["food"])
        assertEquals(ResponseType.A2UI_JSON, payload.expectedResponseType)
        assertNull(payload.constraints)
    }

    @Test
    fun testTaskRequestPayloadWithConstraints() {
        val constraints = TaskConstraints(maxItems = 10, timeout = 5000, requiredFields = listOf("name", "price"))
        val payload = TaskRequestPayload(
            intent = IntentInfo("APP_SEARCH", "search", 0.9f),
            entities = emptyMap(),
            userQuery = "test",
            expectedResponseType = ResponseType.RAW_DATA,
            constraints = constraints
        )

        val c = payload.constraints!!
        assertEquals(10, c.maxItems)
        assertEquals(5000L, c.timeout)
        assertEquals(2, c.requiredFields!!.size)
    }

    @Test
    fun testCreateTaskResponsePayload() {
        val data = ResponseData(
            type = "shop_list",
            items = listOf(mapOf("name" to "Shop A", "price" to "10")),
            metadata = mapOf("total" to "5")
        )
        val payload = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            data = data,
            message = "Found 5 shops"
        )

        val d = payload.data!!
        assertEquals(TaskStatus.SUCCESS, payload.status)
        assertEquals("shop_list", d.type)
        assertEquals(1, d.items.size)
        assertEquals("Found 5 shops", payload.message)
    }

    @Test
    fun testTaskResponsePayloadWithFollowUpActions() {
        val actions = listOf(
            FollowUpAction("f1", "按评分筛选", "filter", mapOf("by" to "rating")),
            FollowUpAction("f2", "按距离筛选", "filter", mapOf("by" to "distance"))
        )
        val payload = TaskResponsePayload(
            status = TaskStatus.SUCCESS,
            followUpActions = actions
        )

        val result = payload.followUpActions!!
        assertEquals(2, result.size)
        assertEquals("按评分筛选", result[0].label)
    }

    @Test
    fun testCreateAgentMessage() {
        val from = AgentIdentity(AgentType.SYSTEM, "system", "System")
        val to = AgentIdentity(AgentType.APP, "meituan", "美团")
        val payload = TaskRequestPayload(
            intent = IntentInfo("SEARCH", "search", 0.9f),
            entities = emptyMap(),
            userQuery = "test",
            expectedResponseType = ResponseType.A2UI_JSON
        )

        val message = AgentMessage(
            messageId = "msg_001",
            from = from,
            to = to,
            type = AgentMessageType.TASK_REQUEST,
            payload = payload
        )

        assertEquals("msg_001", message.messageId)
        assertEquals(AgentMessageType.TASK_REQUEST, message.type)
        assertEquals("system", message.from.id)
        assertEquals("meituan", message.to.id)
        assertNotNull(message.timestamp)
    }

    @Test
    fun testAgentMessageTimestampAutoSet() {
        val before = System.currentTimeMillis()
        val message = AgentMessage(
            messageId = "msg_ts",
            from = AgentIdentity(AgentType.SYSTEM, "s", "S"),
            to = AgentIdentity(AgentType.APP, "a", "A"),
            type = AgentMessageType.TASK_REQUEST,
            payload = TaskRequestPayload(
                IntentInfo("T", "a", 0.5f), emptyMap(), "q", ResponseType.RAW_DATA
            )
        )
        val after = System.currentTimeMillis()

        assertTrue(message.timestamp in before..after)
    }

    @Test
    fun testResponseDataEquality() {
        val data1 = ResponseData("type1", listOf(mapOf("a" to "b")))
        val data2 = ResponseData("type1", listOf(mapOf("a" to "b")))
        assertEquals(data1, data2)
    }

    @Test
    fun testFollowUpAction() {
        val action = FollowUpAction(
            id = "filter_rating",
            label = "按评分筛选",
            actionType = "filter",
            params = mapOf("by" to "rating")
        )

        assertEquals("filter_rating", action.id)
        assertEquals("按评分筛选", action.label)
        assertEquals("rating", action.params!!["by"])
    }

    @Test
    fun testTaskStatusValues() {
        assertEquals(6, TaskStatus.values().size)
        assertNotNull(TaskStatus.SUCCESS)
        assertNotNull(TaskStatus.FAILED)
        assertNotNull(TaskStatus.IN_PROGRESS)
        assertNotNull(TaskStatus.NEED_MORE_INFO)
    }

    @Test
    fun testResponseTypeValues() {
        assertEquals(3, ResponseType.values().size)
        assertNotNull(ResponseType.RAW_DATA)
        assertNotNull(ResponseType.A2UI_JSON)
        assertNotNull(ResponseType.HYBRID)
    }

    @Test
    fun testAgentMessageTypeValues() {
        assertEquals(10, AgentMessageType.values().size)
        assertNotNull(AgentMessageType.TASK_REQUEST)
        assertNotNull(AgentMessageType.CAPABILITY_QUERY)
    }

    @Test
    fun testQueryRequestPayload() {
        val payload = QueryRequestPayload(
            query = "what's nearby",
            entities = mapOf("location" to "here")
        )
        assertEquals("what's nearby", payload.query)
        assertEquals("here", payload.entities["location"])
    }

    @Test
    fun testActionRequestPayload() {
        val payload = ActionRequestPayload(
            actionId = "open_shop",
            params = mapOf("shopId" to "001")
        )
        assertEquals("open_shop", payload.actionId)
        assertEquals("001", payload.params["shopId"])
    }
}
