package com.nano.a2ui.bridge

import com.nano.a2ui.protocol.A2UIProtocol
import com.nano.llm.a2ui.A2UIAction
import com.nano.llm.a2ui.ActionType
import com.nano.llm.agent.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class A2UIEventBridgeTest {

    private fun createBridge(): A2UIEventBridge = A2UIEventBridge()

    private fun createMockAgent(
        id: String,
        response: TaskResponsePayload = TaskResponsePayload(status = TaskStatus.SUCCESS, message = "OK from $id")
    ): AppAgent {
        return object : BaseAppAgent(id, id) {
            override val capabilities = setOf(AgentCapability.SEARCH)

            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = id, supportedIntents = emptyList(),
                supportedEntities = emptyList(), exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )

            override suspend fun handleTaskRequest(request: TaskRequestPayload) = response

            override suspend fun handleUIAction(action: A2UIAction) = response
        }
    }

    @Test
    fun testRegisterUnregisterAgent() {
        val bridge = createBridge()
        val agent = createMockAgent("agent1")

        bridge.registerAgent(agent)
        assertEquals(1, bridge.getRegisteredAgentCount())

        bridge.unregisterAgent("agent1")
        assertEquals(0, bridge.getRegisteredAgentCount())
    }

    @Test
    fun testDispatchClickEvent() = runTest {
        val bridge = createBridge()
        bridge.registerAgent(createMockAgent("calc"))

        val event = bridge.createClickEvent(
            "btn1",
            A2UIAction(ActionType.AGENT_CALL, "calc", "compute")
        )

        val result = bridge.dispatchEvent(event)
        assertTrue(result)
    }

    @Test
    fun testDispatchToNonExistentAgent() = runTest {
        val bridge = createBridge()

        val event = bridge.createClickEvent(
            "btn1",
            A2UIAction(ActionType.AGENT_CALL, "nonexistent", "method")
        )

        val result = bridge.dispatchEvent(event)
        assertFalse(result)
    }

    @Test
    fun testDispatchWithoutAction() = runTest {
        val bridge = createBridge()
        bridge.registerAgent(createMockAgent("agent1"))

        val event = A2UIProtocol.A2UIEvent(
            eventId = "evt1",
            sourceComponent = "comp1",
            eventType = A2UIProtocol.EventType.CLICK,
            action = null  // 无 action
        )

        val result = bridge.dispatchEvent(event)
        assertFalse(result)
    }

    @Test
    fun testCreateClickEvent() {
        val bridge = createBridge()
        val action = A2UIAction(ActionType.AGENT_CALL, "target1", "click")

        val event = bridge.createClickEvent("component1", action)

        assertEquals("component1", event.sourceComponent)
        assertEquals(A2UIProtocol.EventType.CLICK, event.eventType)
        assertEquals("target1", event.action!!.target)
        assertTrue(event.eventId.startsWith("evt_click_"))
    }

    @Test
    fun testCreateItemSelectEvent() {
        val bridge = createBridge()
        val action = A2UIAction(ActionType.AGENT_CALL, "list_agent", "select")

        val event = bridge.createItemSelectEvent(
            "list1", "item_42", action,
            mapOf("price" to "9.9")
        )

        assertEquals(A2UIProtocol.EventType.ITEM_SELECT, event.eventType)
        assertEquals("item_42", event.userData["itemId"])
        assertEquals("9.9", event.userData["price"])
    }

    @Test
    fun testExecuteAction() = runTest {
        val bridge = createBridge()
        val expectedResponse = TaskResponsePayload(status = TaskStatus.SUCCESS, message = "Executed!")
        bridge.registerAgent(createMockAgent("exec_agent", expectedResponse))

        val action = A2UIAction(ActionType.AGENT_CALL, "exec_agent", "run")
        val result = bridge.executeAction(action)

        assertNotNull(result)
        assertEquals(TaskStatus.SUCCESS, result!!.status)
    }

    @Test
    fun testExecuteActionNonExistentAgent() = runTest {
        val bridge = createBridge()

        val action = A2UIAction(ActionType.AGENT_CALL, "missing", "run")
        val result = bridge.executeAction(action)

        assertNull(result)
    }

    @Test
    fun testGlobalCallbackOnSuccess() = runTest {
        val bridge = createBridge()
        bridge.registerAgent(createMockAgent("agent1"))

        var callbackCalled = false
        bridge.addGlobalCallback(object : A2UIEventBridge.EventCallback {
            override fun onResponse(response: TaskResponsePayload) { callbackCalled = true }
            override fun onError(error: Exception) {}
        })

        val event = bridge.createClickEvent("btn1", A2UIAction(ActionType.AGENT_CALL, "agent1", "go"))
        bridge.dispatchEvent(event)

        assertTrue(callbackCalled)
    }

    @Test
    fun testGlobalCallbackOnError() = runTest {
        val bridge = createBridge()

        // 创建会抛异常的 Agent
        val errorAgent = object : BaseAppAgent("error_agent", "Error") {
            override val capabilities = setOf(AgentCapability.SEARCH)
            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = "error_agent", supportedIntents = emptyList(),
                supportedEntities = emptyList(), exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )
            override suspend fun handleTaskRequest(request: TaskRequestPayload) = TaskResponsePayload(status = TaskStatus.FAILED)
            override suspend fun handleUIAction(action: A2UIAction): TaskResponsePayload {
                throw RuntimeException("Agent error")
            }
        }
        bridge.registerAgent(errorAgent)

        var errorCaught = false
        bridge.addGlobalCallback(object : A2UIEventBridge.EventCallback {
            override fun onResponse(response: TaskResponsePayload) {}
            override fun onError(error: Exception) { errorCaught = true }
        })

        val event = bridge.createClickEvent("btn1", A2UIAction(ActionType.AGENT_CALL, "error_agent", "fail"))
        bridge.dispatchEvent(event)

        assertTrue(errorCaught)
    }

    @Test
    fun testComponentCallbackOnSuccess() = runTest {
        val bridge = createBridge()
        bridge.registerAgent(createMockAgent("agent1"))

        var componentCallbackCalled = false
        bridge.setComponentCallback("my_component", object : A2UIEventBridge.EventCallback {
            override fun onResponse(response: TaskResponsePayload) { componentCallbackCalled = true }
            override fun onError(error: Exception) {}
        })

        val event = A2UIProtocol.A2UIEvent(
            eventId = "evt1", sourceComponent = "my_component",
            eventType = A2UIProtocol.EventType.CLICK,
            action = A2UIAction(ActionType.AGENT_CALL, "agent1", "go")
        )
        bridge.dispatchEvent(event)

        assertTrue(componentCallbackCalled)
    }

    @Test
    fun testMultipleAgentsEventRouting() = runTest {
        val bridge = createBridge()

        val response1 = TaskResponsePayload(status = TaskStatus.SUCCESS, message = "From A")
        val response2 = TaskResponsePayload(status = TaskStatus.SUCCESS, message = "From B")
        bridge.registerAgent(createMockAgent("agentA", response1))
        bridge.registerAgent(createMockAgent("agentB", response2))

        // 路由到 A
        val result1 = bridge.executeAction(A2UIAction(ActionType.AGENT_CALL, "agentA", "run"))
        assertEquals("From A", result1!!.message)

        // 路由到 B
        val result2 = bridge.executeAction(A2UIAction(ActionType.AGENT_CALL, "agentB", "run"))
        assertEquals("From B", result2!!.message)
    }
}
