package com.nano.llm.agent

/**
 * 对话上下文 - 管理多轮对话状态
 */
data class ConversationContext(
    val conversationId: String = "",
    val userQuery: String = "",
    val previousQueries: List<String> = emptyList(),
    val activeAgents: List<String> = emptyList()
) {
    fun withQuery(query: String): ConversationContext {
        return copy(
            userQuery = query,
            previousQueries = previousQueries + listOfNotNull(userQuery.takeIf { it.isNotEmpty() })
        )
    }
}
