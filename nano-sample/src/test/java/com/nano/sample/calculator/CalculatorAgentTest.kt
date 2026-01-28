package com.nano.sample.calculator

import com.nano.llm.agent.*
import com.nano.llm.a2ui.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CalculatorAgentTest {

    private fun createAgent(): CalculatorAgent = CalculatorAgent()

    // --- 基本运算 ---

    @Test
    fun testAddition() {
        val agent = createAgent()
        assertEquals(5.0, agent.evaluate("2 + 3"), 1e-9)
    }

    @Test
    fun testSubtraction() {
        val agent = createAgent()
        assertEquals(7.0, agent.evaluate("10 - 3"), 1e-9)
    }

    @Test
    fun testMultiplication() {
        val agent = createAgent()
        assertEquals(12.0, agent.evaluate("3 * 4"), 1e-9)
    }

    @Test
    fun testDivision() {
        val agent = createAgent()
        assertEquals(2.5, agent.evaluate("5 / 2"), 1e-9)
    }

    @Test
    fun testModulo() {
        val agent = createAgent()
        assertEquals(1.0, agent.evaluate("7 % 3"), 1e-9)
    }

    @Test
    fun testPower() {
        val agent = createAgent()
        assertEquals(8.0, agent.evaluate("2 ^ 3"), 1e-9)
    }

    // --- 运算符优先级 ---

    @Test
    fun testOperatorPrecedence() {
        val agent = createAgent()
        // 乘法优先于加法
        assertEquals(14.0, agent.evaluate("2 + 3 * 4"), 1e-9)
    }

    @Test
    fun testComplexPrecedence() {
        val agent = createAgent()
        assertEquals(7.0, agent.evaluate("1 + 2 * 3"), 1e-9)
    }

    // --- 括号 ---

    @Test
    fun testParentheses() {
        val agent = createAgent()
        assertEquals(20.0, agent.evaluate("(2 + 3) * 4"), 1e-9)
    }

    @Test
    fun testNestedParentheses() {
        val agent = createAgent()
        assertEquals(11.0, agent.evaluate("(1 + (2 + 3)) * (4 - 2) - 1"), 1e-9)
    }

    // --- 一元运算 ---

    @Test
    fun testUnaryMinus() {
        val agent = createAgent()
        assertEquals(-5.0, agent.evaluate("-5"), 1e-9)
    }

    @Test
    fun testUnaryMinusInExpression() {
        val agent = createAgent()
        assertEquals(-1.0, agent.evaluate("-3 + 2"), 1e-9)
    }

    // --- 错误处理 ---

    @Test(expected = CalculationException::class)
    fun testDivisionByZero() {
        val agent = createAgent()
        agent.evaluate("5 / 0")
    }

    @Test(expected = CalculationException::class)
    fun testModByZero() {
        val agent = createAgent()
        agent.evaluate("5 % 0")
    }

    @Test(expected = CalculationException::class)
    fun testEmptyExpression() {
        val agent = createAgent()
        agent.evaluate("")
    }

    @Test(expected = CalculationException::class)
    fun testInvalidCharacter() {
        val agent = createAgent()
        agent.evaluate("2 + @")
    }

    @Test(expected = CalculationException::class)
    fun testUnmatchedParen() {
        val agent = createAgent()
        agent.evaluate("(2 + 3")
    }

    // --- handleUIAction 测试 ---

    @Test
    fun testHandleUIActionSuccess() = runTest {
        val agent = createAgent()
        val action = A2UIAction(
            ActionType.AGENT_CALL, "calculator", "compute",
            mapOf("expression" to "4 * 5")
        )
        val response = agent.handleUIAction(action)

        assertEquals(TaskStatus.SUCCESS, response.status)
        assertNotNull(response.a2ui)
        assertTrue(response.data!!.items[0]["result"] == "20")
    }

    @Test
    fun testHandleUIActionInvalid() = runTest {
        val agent = createAgent()
        val action = A2UIAction(
            ActionType.AGENT_CALL, "calculator", "compute",
            mapOf("expression" to "2 +")
        )
        val response = agent.handleUIAction(action)

        assertEquals(TaskStatus.FAILED, response.status)
    }

    @Test
    fun testHandleUIActionMissingExpression() = runTest {
        val agent = createAgent()
        val action = A2UIAction(ActionType.AGENT_CALL, "calculator", "compute")
        val response = agent.handleUIAction(action)

        assertEquals(TaskStatus.FAILED, response.status)
    }

    // --- handleRequest (通过 AgentMessage 调用) ---

    @Test
    fun testHandleRequestSuccess() = runTest {
        val agent = createAgent()
        val request = AgentMessage(
            messageId = "msg_1",
            from = AgentIdentity(AgentType.SYSTEM, "system", "System"),
            to = AgentIdentity(AgentType.APP, "calculator", "计算器"),
            type = AgentMessageType.TASK_REQUEST,
            payload = TaskRequestPayload(
                intent = IntentInfo("calculate", "compute", 0.9f),
                entities = mapOf("expression" to "10 - 3"),
                userQuery = "10 - 3",
                expectedResponseType = ResponseType.A2UI_JSON
            )
        )
        val response = agent.handleRequest(request)

        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals("7", response.data!!.items[0]["result"])
    }

    @Test
    fun testHandleRequestFallbackToQuery() = runTest {
        val agent = createAgent()
        val request = AgentMessage(
            messageId = "msg_2",
            from = AgentIdentity(AgentType.SYSTEM, "system", "System"),
            to = AgentIdentity(AgentType.APP, "calculator", "计算器"),
            type = AgentMessageType.TASK_REQUEST,
            payload = TaskRequestPayload(
                intent = IntentInfo("calculate", "compute", 0.9f),
                entities = emptyMap(),
                userQuery = "2 + 8",
                expectedResponseType = ResponseType.RAW_DATA
            )
        )
        val response = agent.handleRequest(request)

        assertEquals(TaskStatus.SUCCESS, response.status)
        assertEquals("10", response.data!!.items[0]["result"])
    }

    @Test
    fun testHandleRequestInvalidExpression() = runTest {
        val agent = createAgent()
        val request = AgentMessage(
            messageId = "msg_3",
            from = AgentIdentity(AgentType.SYSTEM, "system", "System"),
            to = AgentIdentity(AgentType.APP, "calculator", "计算器"),
            type = AgentMessageType.TASK_REQUEST,
            payload = TaskRequestPayload(
                intent = IntentInfo("calculate", "compute", 0.9f),
                entities = mapOf("expression" to "abc"),
                userQuery = "abc",
                expectedResponseType = ResponseType.RAW_DATA
            )
        )
        val response = agent.handleRequest(request)

        assertEquals(TaskStatus.FAILED, response.status)
    }

    // --- Agent 元数据 ---

    @Test
    fun testAgentIdentity() {
        val agent = createAgent()
        assertEquals("calculator", agent.agentId)
        assertEquals("计算器", agent.agentName)
        assertTrue(agent.capabilities.contains(AgentCapability.SEARCH))
    }

    @Test
    fun testDescribeCapabilities() {
        val agent = createAgent()
        val desc = agent.describeCapabilities()

        assertEquals("calculator", desc.agentId)
        assertTrue(desc.supportedIntents.contains("calculate"))
        assertTrue(desc.responseTypes.contains(ResponseType.A2UI_JSON))
    }

    // --- 浮点数 ---

    @Test
    fun testFloatArithmetic() {
        val agent = createAgent()
        assertEquals(3.14, agent.evaluate("1.14 + 2"), 1e-9)
    }

    @Test
    fun testPowerRightAssociative() {
        val agent = createAgent()
        // 2^3^2 = 2^(3^2) = 2^9 = 512（右结合）
        assertEquals(512.0, agent.evaluate("2 ^ 3 ^ 2"), 1e-9)
    }

    // --- formatResult 辅助 ---

    @Test
    fun testFormatResultInteger() {
        val agent = createAgent()
        assertEquals("5", agent.formatResult(5.0))
    }

    @Test
    fun testFormatResultDecimal() {
        val agent = createAgent()
        val result = agent.formatResult(3.14159)
        assertTrue(result.startsWith("3.14159"))
    }
}
