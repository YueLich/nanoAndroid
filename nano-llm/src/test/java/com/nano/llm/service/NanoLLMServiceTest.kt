package com.nano.llm.service

import com.nano.llm.agent.*
import com.nano.llm.a2ui.A2UIAction
import com.nano.llm.model.*
import org.junit.Assert.*
import org.junit.Test

class NanoLLMServiceTest {

    /** 创建带 Mock Provider 的 LLMService */
    private fun createService(): NanoLLMService {
        return NanoLLMService(
            config = LLMConfig(
                providerType = ProviderType.MOCK
            )
        )
    }

    /** 创建测试用 Mock Agent */
    private fun createMockAgent(
        id: String,
        name: String,
        capabilities: Set<AgentCapability> = setOf(AgentCapability.SEARCH)
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

            override suspend fun handleTaskRequest(request: TaskRequestPayload) = TaskResponsePayload(
                status = TaskStatus.SUCCESS,
                message = "Response from $name"
            )
        }
    }

    @Test
    fun testServiceStartStop() {
        val service = createService()

        assertFalse(service.isRunning())
        service.start()
        assertTrue(service.isRunning())
        service.stop()
        assertFalse(service.isRunning())
    }

    @Test
    fun testServiceStartInitializesComponents() {
        val service = createService()
        service.start()

        // 注册表应该可用
        assertNotNull(service.getAgentRegistry())
        // NaturalLanguageAPI 应该可用
        assertNotNull(service.getNaturalLanguageAPI())

        service.stop()
    }

    @Test
    fun testRegisterAgent() {
        val service = createService()
        service.start()

        val agent = createMockAgent("test_app", "测试应用")
        service.registerAgent(agent)

        assertEquals(1, service.getAgentRegistry().getAgentCount())
        assertNotNull(service.getAgentRegistry().getAgent("test_app"))

        service.stop()
    }

    @Test
    fun testRegisterMultipleAgents() {
        val service = createService()
        service.start()

        service.registerAgent(createMockAgent("app1", "应用1"))
        service.registerAgent(createMockAgent("app2", "应用2", setOf(AgentCapability.ORDER)))
        service.registerAgent(createMockAgent("app3", "应用3", setOf(AgentCapability.MEDIA)))

        assertEquals(3, service.getAgentRegistry().getAgentCount())

        service.stop()
    }

    @Test
    fun testUnregisterAgent() {
        val service = createService()
        service.start()

        service.registerAgent(createMockAgent("app1", "应用1"))
        service.registerAgent(createMockAgent("app2", "应用2"))
        assertEquals(2, service.getAgentRegistry().getAgentCount())

        service.unregisterAgent("app1")
        assertEquals(1, service.getAgentRegistry().getAgentCount())
        assertNull(service.getAgentRegistry().getAgent("app1"))
        assertNotNull(service.getAgentRegistry().getAgent("app2"))

        service.stop()
    }

    @Test
    fun testUnregisterNonExistentAgent() {
        val service = createService()
        service.start()

        // 不应抛出异常
        service.unregisterAgent("nonexistent")
        assertEquals(0, service.getAgentRegistry().getAgentCount())

        service.stop()
    }

    @Test
    fun testServiceSystemReady() {
        val service = createService()
        service.start()

        // systemReady 不应抛出异常
        service.systemReady()

        assertTrue(service.isRunning())
        service.stop()
    }

    @Test
    fun testBinderDescriptor() {
        val service = createService()
        service.start()

        assertEquals(NanoLLMService.DESCRIPTOR, "com.nano.llm.INanoLLMService")
        // 确认 service 是 NanoBinder 实例
        assertNotNull(service.asBinder())

        service.stop()
    }

    @Test
    fun testTransactCodeConstants() {
        // 验证事务码不重叠
        val codes = listOf(
            NanoLLMService.TRANSACT_PROCESS_INPUT,
            NanoLLMService.TRANSACT_REGISTER_AGENT,
            NanoLLMService.TRANSACT_UNREGISTER_AGENT,
            NanoLLMService.TRANSACT_GET_CAPABILITIES,
            NanoLLMService.TRANSACT_GET_STATUS
        )

        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun testAgentRegistryCapabilityQuery() {
        val service = createService()
        service.start()

        val searchAgent = createMockAgent("search_app", "搜索应用", setOf(AgentCapability.SEARCH))
        val orderAgent = createMockAgent("order_app", "点餐应用", setOf(AgentCapability.ORDER))
        service.registerAgent(searchAgent)
        service.registerAgent(orderAgent)

        val searchAgents = service.getAgentRegistry().findAgentsByCapability(AgentCapability.SEARCH)
        assertEquals(1, searchAgents.size)
        assertEquals("search_app", searchAgents[0].agentId)

        service.stop()
    }
}
