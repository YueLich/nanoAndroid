package com.nano.llm.agent

import com.nano.llm.a2ui.A2UIAction
import org.junit.Test
import org.junit.Assert.*

class AgentRegistryTest {

    private fun createMockAgent(
        id: String,
        name: String,
        capabilities: Set<AgentCapability> = setOf(AgentCapability.SEARCH)
    ): AppAgent {
        return object : BaseAppAgent(id, name) {
            override val capabilities = capabilities

            override fun describeCapabilities() = AgentCapabilityDescription(
                agentId = id,
                supportedIntents = listOf("TEST"),
                supportedEntities = listOf("test_entity"),
                exampleQueries = listOf("test query"),
                responseTypes = setOf(ResponseType.RAW_DATA)
            )

            override suspend fun handleTaskRequest(request: TaskRequestPayload) =
                TaskResponsePayload(status = TaskStatus.SUCCESS, message = "OK from $name")
        }
    }

    @Test
    fun testRegisterAndGetAgent() {
        val registry = AgentRegistry()
        val agent = createMockAgent("test_agent", "Test Agent")

        registry.registerAgent(agent)

        assertEquals(agent, registry.getAgent("test_agent"))
    }

    @Test
    fun testGetNonExistentAgent() {
        val registry = AgentRegistry()
        assertNull(registry.getAgent("nonexistent"))
    }

    @Test
    fun testUnregisterAgent() {
        val registry = AgentRegistry()
        val agent = createMockAgent("agent1", "Agent 1")

        registry.registerAgent(agent)
        assertEquals(1, registry.getAgentCount())

        registry.unregisterAgent("agent1")
        assertEquals(0, registry.getAgentCount())
        assertNull(registry.getAgent("agent1"))
    }

    @Test
    fun testUnregisterNonExistentAgent() {
        val registry = AgentRegistry()
        // Should not throw
        registry.unregisterAgent("nonexistent")
    }

    @Test
    fun testFindAgentsByCapabilitySingle() {
        val registry = AgentRegistry()
        val agent1 = createMockAgent("agent1", "Agent 1", setOf(AgentCapability.SEARCH))
        val agent2 = createMockAgent("agent2", "Agent 2", setOf(AgentCapability.SEARCH, AgentCapability.ORDER))
        val agent3 = createMockAgent("agent3", "Agent 3", setOf(AgentCapability.ORDER))

        registry.registerAgent(agent1)
        registry.registerAgent(agent2)
        registry.registerAgent(agent3)

        val searchAgents = registry.findAgentsByCapability(AgentCapability.SEARCH)
        assertEquals(2, searchAgents.size)
        assertTrue(searchAgents.any { it.agentId == "agent1" })
        assertTrue(searchAgents.any { it.agentId == "agent2" })
    }

    @Test
    fun testFindAgentsByCapabilityNoMatch() {
        val registry = AgentRegistry()
        val agent = createMockAgent("agent1", "Agent 1", setOf(AgentCapability.SEARCH))
        registry.registerAgent(agent)

        val mediaAgents = registry.findAgentsByCapability(AgentCapability.MEDIA)
        assertTrue(mediaAgents.isEmpty())
    }

    @Test
    fun testFindAgentsByCapabilitiesMultiple() {
        val registry = AgentRegistry()
        val agent1 = createMockAgent("agent1", "Agent 1", setOf(AgentCapability.SEARCH))
        val agent2 = createMockAgent("agent2", "Agent 2", setOf(AgentCapability.ORDER))
        val agent3 = createMockAgent("agent3", "Agent 3", setOf(AgentCapability.MEDIA))

        registry.registerAgent(agent1)
        registry.registerAgent(agent2)
        registry.registerAgent(agent3)

        val agents = registry.findAgentsByCapabilities(setOf(AgentCapability.SEARCH, AgentCapability.ORDER))
        assertEquals(2, agents.size)
        assertTrue(agents.any { it.agentId == "agent1" })
        assertTrue(agents.any { it.agentId == "agent2" })
    }

    @Test
    fun testGetAllCapabilities() {
        val registry = AgentRegistry()
        val agent1 = createMockAgent("agent1", "Agent 1", setOf(AgentCapability.SEARCH))
        val agent2 = createMockAgent("agent2", "Agent 2", setOf(AgentCapability.ORDER))

        registry.registerAgent(agent1)
        registry.registerAgent(agent2)

        val capabilities = registry.getAllCapabilities()
        assertEquals(2, capabilities.size)
    }

    @Test
    fun testGetSystemSettingsAgent() {
        val registry = AgentRegistry()
        val settingsAgent = createMockAgent("system_settings", "Settings", setOf(AgentCapability.SETTINGS))
        registry.registerAgent(settingsAgent)

        assertEquals(settingsAgent, registry.getSystemSettingsAgent())
    }

    @Test
    fun testGetSystemSettingsAgentWhenAbsent() {
        val registry = AgentRegistry()
        assertNull(registry.getSystemSettingsAgent())
    }

    @Test
    fun testGetDefaultAgent() {
        val registry = AgentRegistry()
        val defaultAgent = createMockAgent("default", "Default", setOf(AgentCapability.SEARCH))
        registry.registerAgent(defaultAgent)

        assertEquals(defaultAgent, registry.getDefaultAgent())
    }

    @Test
    fun testAgentCount() {
        val registry = AgentRegistry()
        assertEquals(0, registry.getAgentCount())

        registry.registerAgent(createMockAgent("a1", "A1"))
        assertEquals(1, registry.getAgentCount())

        registry.registerAgent(createMockAgent("a2", "A2", setOf(AgentCapability.ORDER)))
        assertEquals(2, registry.getAgentCount())
    }

    @Test
    fun testCapabilityIndexCleanedOnUnregister() {
        val registry = AgentRegistry()
        val agent = createMockAgent("agent1", "Agent 1", setOf(AgentCapability.SEARCH))
        registry.registerAgent(agent)

        assertEquals(1, registry.findAgentsByCapability(AgentCapability.SEARCH).size)

        registry.unregisterAgent("agent1")
        assertEquals(0, registry.findAgentsByCapability(AgentCapability.SEARCH).size)
    }

    @Test
    fun testRegisterOverwritesExistingCapabilities() {
        val registry = AgentRegistry()
        val agent1 = createMockAgent("agent1", "Agent 1 v1", setOf(AgentCapability.SEARCH))
        val agent2 = createMockAgent("agent1", "Agent 1 v2", setOf(AgentCapability.ORDER))

        registry.registerAgent(agent1)
        assertEquals(1, registry.findAgentsByCapability(AgentCapability.SEARCH).size)

        registry.registerAgent(agent2)
        // 旧 SEARCH 能力应已清除
        assertEquals(0, registry.findAgentsByCapability(AgentCapability.SEARCH).size)
        // 新 ORDER 能力应生效
        assertEquals(1, registry.findAgentsByCapability(AgentCapability.ORDER).size)
        assertEquals("Agent 1 v2", registry.getAgent("agent1")?.agentName)
    }

    @Test
    fun testFindAgentsByCapabilitiesEmptyCapSet() {
        val registry = AgentRegistry()
        registry.registerAgent(createMockAgent("agent1", "Agent 1"))

        val agents = registry.findAgentsByCapabilities(emptySet())
        assertTrue(agents.isEmpty())
    }
}
