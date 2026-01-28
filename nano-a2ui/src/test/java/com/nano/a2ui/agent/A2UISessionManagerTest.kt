package com.nano.a2ui.agent

import com.nano.llm.a2ui.A2UISpec
import com.nano.llm.a2ui.A2UIText
import org.junit.Assert.*
import org.junit.Test

class A2UISessionManagerTest {

    private fun createManager(): A2UISessionManager = A2UISessionManager()

    @Test
    fun testCreateSession() {
        val manager = createManager()
        val session = manager.createSession("s1", "conv1")

        assertEquals("s1", session.sessionId)
        assertEquals("conv1", session.conversationId)
        assertEquals(A2UISessionManager.SessionState.IDLE, session.state)
        assertTrue(session.createdAt > 0)
    }

    @Test
    fun testGetSession() {
        val manager = createManager()
        manager.createSession("s1", "conv1")

        val session = manager.getSession("s1")
        assertNotNull(session)
        assertEquals("s1", session!!.sessionId)
    }

    @Test
    fun testGetSessionNonExistent() {
        val manager = createManager()
        assertNull(manager.getSession("nonexistent"))
    }

    @Test
    fun testGetSessionByConversation() {
        val manager = createManager()
        manager.createSession("s1", "conv1")

        val session = manager.getSessionByConversation("conv1")
        assertNotNull(session)
        assertEquals("s1", session!!.sessionId)
    }

    @Test
    fun testGetSessionByConversationNonExistent() {
        val manager = createManager()
        assertNull(manager.getSessionByConversation("unknown"))
    }

    @Test
    fun testActivateSession() {
        val manager = createManager()
        manager.createSession("s1", "conv1")

        val spec = A2UISpec(root = A2UIText(text = "Active"))
        val session = manager.activateSession("s1", spec)

        assertNotNull(session)
        assertEquals(A2UISessionManager.SessionState.ACTIVE, session!!.state)
        assertNotNull(session.currentSpec)
        assertTrue(session.isActive())
    }

    @Test
    fun testActivateNonExistentSession() {
        val manager = createManager()
        val result = manager.activateSession("nonexistent", A2UISpec(root = A2UIText(text = "X")))
        assertNull(result)
    }

    @Test
    fun testUpdateSessionSpec() {
        val manager = createManager()
        manager.createSession("s1", "conv1")
        manager.activateSession("s1", A2UISpec(root = A2UIText(text = "Original")))

        val newSpec = A2UISpec(root = A2UIText(text = "Updated"))
        val session = manager.updateSessionSpec("s1", newSpec)

        assertNotNull(session)
        assertEquals("Updated", (session!!.currentSpec!!.root as A2UIText).text)
    }

    @Test
    fun testAddAgent() {
        val manager = createManager()
        manager.createSession("s1", "conv1")

        assertTrue(manager.addAgent("s1", "agent1"))
        assertTrue(manager.addAgent("s1", "agent2"))

        // 重复添加不影响
        assertTrue(manager.addAgent("s1", "agent1"))

        val session = manager.getSession("s1")!!
        assertEquals(2, session.participatingAgents.size)
    }

    @Test
    fun testAddAgentToNonExistentSession() {
        val manager = createManager()
        assertFalse(manager.addAgent("nonexistent", "agent1"))
    }

    @Test
    fun testRecordAction() {
        val manager = createManager()
        manager.createSession("s1", "conv1")

        assertTrue(manager.recordAction("s1", "click_btn1"))
        assertTrue(manager.recordAction("s1", "select_item"))

        val session = manager.getSession("s1")!!
        assertEquals(2, session.userActions.size)
        assertEquals(A2UISessionManager.SessionState.ACTIVE, session.state)
    }

    @Test
    fun testCompleteSession() {
        val manager = createManager()
        manager.createSession("s1", "conv1")
        manager.activateSession("s1", A2UISpec(root = A2UIText(text = "Done")))

        val session = manager.completeSession("s1")

        assertNotNull(session)
        assertEquals(A2UISessionManager.SessionState.COMPLETED, session!!.state)
        assertTrue(session.isTerminal())
        assertFalse(session.isActive())
    }

    @Test
    fun testExpireSession() {
        val manager = createManager()
        manager.createSession("s1", "conv1")

        val session = manager.expireSession("s1")

        assertNotNull(session)
        assertEquals(A2UISessionManager.SessionState.EXPIRED, session!!.state)
        assertTrue(session.isTerminal())
    }

    @Test
    fun testCleanupTerminalSessions() {
        val manager = createManager()

        manager.createSession("s1", "conv1")
        manager.createSession("s2", "conv2")
        manager.createSession("s3", "conv3")

        // s1 完成，s2 过期，s3 保持活跃
        manager.completeSession("s1")
        manager.expireSession("s2")
        manager.activateSession("s3", A2UISpec(root = A2UIText(text = "Active")))

        manager.cleanupTerminalSessions()

        assertNull(manager.getSession("s1"))
        assertNull(manager.getSession("s2"))
        assertNotNull(manager.getSession("s3"))
    }

    @Test
    fun testGetActiveSessionCount() {
        val manager = createManager()

        manager.createSession("s1", "conv1")
        manager.createSession("s2", "conv2")

        assertEquals(0, manager.getActiveSessionCount())

        manager.activateSession("s1", A2UISpec(root = A2UIText(text = "A")))
        assertEquals(1, manager.getActiveSessionCount())

        manager.activateSession("s2", A2UISpec(root = A2UIText(text = "B")))
        assertEquals(2, manager.getActiveSessionCount())

        manager.completeSession("s1")
        assertEquals(1, manager.getActiveSessionCount())
    }
}
