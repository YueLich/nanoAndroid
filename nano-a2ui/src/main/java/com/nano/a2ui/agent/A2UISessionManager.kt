package com.nano.a2ui.agent

import com.nano.llm.a2ui.A2UISpec
import java.util.concurrent.ConcurrentHashMap

/**
 * A2UI 会话管理器 - 管理用户与 A2UI 交互的会话生命周期
 *
 * 会话状态机:
 *   IDLE → ACTIVE → WAITING → COMPLETED
 *                       ↓
 *                    EXPIRED
 *
 * 每个会话关联一个对话（conversationId），追踪：
 * - 当前活跃的 A2UISpec
 * - 参与的 Agent 列表
 * - 用户操作历史
 */
class A2UISessionManager {

    /** 会话状态 */
    enum class SessionState {
        IDLE,
        ACTIVE,
        WAITING,
        COMPLETED,
        EXPIRED
    }

    /** 会话数据 */
    data class A2UISession(
        val sessionId: String,
        val conversationId: String,
        var state: SessionState = SessionState.IDLE,
        var currentSpec: A2UISpec? = null,
        val participatingAgents: MutableList<String> = mutableListOf(),
        val userActions: MutableList<String> = mutableListOf(),
        val createdAt: Long = System.currentTimeMillis(),
        var updatedAt: Long = System.currentTimeMillis()
    ) {
        fun isActive(): Boolean = state == SessionState.ACTIVE || state == SessionState.WAITING
        fun isTerminal(): Boolean = state == SessionState.COMPLETED || state == SessionState.EXPIRED
    }

    /** 会话 ID → 会话 */
    private val sessions = ConcurrentHashMap<String, A2UISession>()

    /** 对话 ID → 会话 ID */
    private val conversationSessions = ConcurrentHashMap<String, String>()

    // ==================== 会话 CRUD ====================

    fun createSession(sessionId: String, conversationId: String): A2UISession {
        val session = A2UISession(
            sessionId = sessionId,
            conversationId = conversationId,
            state = SessionState.IDLE
        )
        sessions[sessionId] = session
        conversationSessions[conversationId] = sessionId
        return session
    }

    fun getSession(sessionId: String): A2UISession? = sessions[sessionId]

    fun getSessionByConversation(conversationId: String): A2UISession? {
        val sessionId = conversationSessions[conversationId] ?: return null
        return sessions[sessionId]
    }

    // ==================== 状态转换 ====================

    fun activateSession(sessionId: String, spec: A2UISpec): A2UISession? {
        val session = sessions[sessionId] ?: return null
        session.state = SessionState.ACTIVE
        session.currentSpec = spec
        session.updatedAt = System.currentTimeMillis()
        return session
    }

    fun updateSessionSpec(sessionId: String, spec: A2UISpec): A2UISession? {
        val session = sessions[sessionId] ?: return null
        session.currentSpec = spec
        session.updatedAt = System.currentTimeMillis()
        return session
    }

    fun completeSession(sessionId: String): A2UISession? {
        val session = sessions[sessionId] ?: return null
        session.state = SessionState.COMPLETED
        session.updatedAt = System.currentTimeMillis()
        return session
    }

    fun expireSession(sessionId: String): A2UISession? {
        val session = sessions[sessionId] ?: return null
        session.state = SessionState.EXPIRED
        session.updatedAt = System.currentTimeMillis()
        return session
    }

    // ==================== 参与者和操作记录 ====================

    fun addAgent(sessionId: String, agentId: String): Boolean {
        val session = sessions[sessionId] ?: return false
        if (agentId !in session.participatingAgents) {
            session.participatingAgents.add(agentId)
        }
        return true
    }

    fun recordAction(sessionId: String, action: String): Boolean {
        val session = sessions[sessionId] ?: return false
        session.userActions.add(action)
        session.state = SessionState.ACTIVE
        session.updatedAt = System.currentTimeMillis()
        return true
    }

    // ==================== 清理和查询 ====================

    fun cleanupTerminalSessions() {
        val toRemove = sessions.entries
            .filter { it.value.isTerminal() }
            .map { it.key }
        toRemove.forEach { sessionId ->
            val session = sessions.remove(sessionId)
            session?.let { conversationSessions.remove(it.conversationId) }
        }
    }

    fun getActiveSessionCount(): Int = sessions.values.count { it.isActive() }

    fun getAllSessions(): List<A2UISession> = sessions.values.toList()
}
