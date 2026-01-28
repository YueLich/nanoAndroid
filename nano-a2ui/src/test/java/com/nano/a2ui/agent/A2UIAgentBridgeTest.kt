package com.nano.a2ui.agent

import com.nano.llm.a2ui.*
import com.nano.llm.agent.*
import org.junit.Assert.*
import org.junit.Test

class A2UIAgentBridgeTest {

    private fun createRegistry(vararg agents: AppAgent): AgentRegistry {
        val registry = AgentRegistry()
        agents.forEach { registry.registerAgent(it) }
        return registry
    }

    private fun createMockAgent(id: String, name: String): AppAgent {
        return object : BaseAppAgent(id, name) {
            override val capabilities = setOf(AgentCapability.SEARCH)
            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = id, supportedIntents = emptyList(),
                supportedEntities = emptyList(), exampleQueries = emptyList(),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )
            override suspend fun handleTaskRequest(request: TaskRequestPayload) =
                TaskResponsePayload(status = TaskStatus.SUCCESS)
        }
    }

    @Test
    fun testRegisterClearAgentSpec() {
        val registry = createRegistry()
        val bridge = A2UIAgentBridge(registry)

        val spec = A2UISpec(root = A2UIText(text = "Test"))
        bridge.registerAgentSpec("agent1", spec)

        assertNotNull(bridge.getAgentSpec("agent1"))

        bridge.clearAgentSpec("agent1")
        assertNull(bridge.getAgentSpec("agent1"))
    }

    @Test
    fun testGetAgentSpec() {
        val bridge = A2UIAgentBridge(createRegistry())

        val spec = A2UISpec(root = A2UIText(text = "Hello"))
        bridge.registerAgentSpec("app1", spec)

        val result = bridge.getAgentSpec("app1")
        assertEquals("Hello", (result!!.root as A2UIText).text)
    }

    @Test
    fun testGetAllActiveSpecs() {
        val bridge = A2UIAgentBridge(createRegistry())

        bridge.registerAgentSpec("a1", A2UISpec(root = A2UIText(text = "Spec1")))
        bridge.registerAgentSpec("a2", A2UISpec(root = A2UIText(text = "Spec2")))

        val all = bridge.getAllActiveSpecs()
        assertEquals(2, all.size)
        assertTrue(all.containsKey("a1"))
        assertTrue(all.containsKey("a2"))
    }

    @Test
    fun testGetAgentSpecNonExistent() {
        val bridge = A2UIAgentBridge(createRegistry())
        assertNull(bridge.getAgentSpec("nonexistent"))
    }

    @Test
    fun testMergeToTabs() {
        val agent1 = createMockAgent("meituan", "美团")
        val agent2 = createMockAgent("eleme", "饿了么")
        val registry = createRegistry(agent1, agent2)
        val bridge = A2UIAgentBridge(registry)

        val specs = mapOf(
            "meituan" to A2UISpec(root = A2UIText(text = "美团结果")),
            "eleme" to A2UISpec(root = A2UIText(text = "饿了么结果"))
        )

        val merged = bridge.mergeToTabs(specs)
        val root = merged.root as A2UITabContent

        assertEquals("merged_tabs", root.id)
        assertEquals(2, root.tabs.size)
        assertEquals("美团", root.tabs[0].label)
        assertEquals("饿了么", root.tabs[1].label)
    }

    @Test
    fun testMergeToUnifiedList() {
        val bridge = A2UIAgentBridge(createRegistry())

        val specs = mapOf(
            "app1" to A2UISpec(root = A2UIContainer(children = listOf(
                A2UIText(text = "Item A"),
                A2UIText(text = "Item B")
            ))),
            "app2" to A2UISpec(root = A2UIContainer(children = listOf(
                A2UIText(text = "Item C")
            )))
        )

        val merged = bridge.mergeToUnifiedList(specs)
        val root = merged.root as A2UIContainer

        assertEquals("unified_list", root.id)
        assertEquals(3, root.children.size) // A, B, C 展平
    }

    @Test
    fun testMergeEmptySpecs() {
        val bridge = A2UIAgentBridge(createRegistry())

        val merged = bridge.mergeToTabs(emptyMap())
        val root = merged.root as A2UITabContent
        assertEquals(0, root.tabs.size)
    }

    @Test
    fun testMergeTabsPreservesAgentNames() {
        val agent = createMockAgent("custom_id", "自定义名称")
        val bridge = A2UIAgentBridge(createRegistry(agent))

        val specs = mapOf(
            "custom_id" to A2UISpec(root = A2UIText(text = "Content"))
        )

        val merged = bridge.mergeToTabs(specs)
        val root = merged.root as A2UITabContent
        assertEquals("自定义名称", root.tabs[0].label)
    }

    @Test
    fun testCreateCrossAgentMessage() {
        val bridge = A2UIAgentBridge(createRegistry())

        val msg = bridge.createCrossAgentMessage("source1", "target1", """{"root":{}}""")

        assertEquals("source1", msg.sourceAgentId)
        assertEquals("target1", msg.targetAgentId)
        assertTrue(msg.messageId.startsWith("a2ui_msg_"))
        assertTrue(msg.timestamp > 0)
    }

    @Test
    fun testCrossAgentMessageBroadcast() {
        val bridge = A2UIAgentBridge(createRegistry())

        val msg = bridge.createCrossAgentMessage("source1", null, """{"root":{}}""")
        assertNull(msg.targetAgentId) // 广播
    }

    @Test
    fun testGetSessionAgents() {
        val bridge = A2UIAgentBridge(createRegistry())

        // 创建跨 Agent 消息追踪会话参与者
        bridge.createCrossAgentMessage("agent1", "agent2", "json1")
        bridge.createCrossAgentMessage("agent1", "agent3", "json2")

        val agents = bridge.getSessionAgents("session_agent1")
        assertEquals(3, agents.size)
        assertTrue(agents.contains("agent1"))
        assertTrue(agents.contains("agent2"))
        assertTrue(agents.contains("agent3"))
    }
}
