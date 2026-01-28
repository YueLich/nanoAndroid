package com.nano.llm.agent

import com.nano.llm.a2ui.A2UISpec

/** Agent 身份标识 */
data class AgentIdentity(
    val type: AgentType,
    val id: String,
    val name: String
)

/** Agent 类型 */
enum class AgentType {
    SYSTEM,
    APP
}

/** Agent 消息类型 */
enum class AgentMessageType {
    TASK_REQUEST,
    QUERY_REQUEST,
    ACTION_REQUEST,
    TASK_RESPONSE,
    QUERY_RESPONSE,
    ACTION_RESPONSE,
    STATUS_UPDATE,
    ERROR,
    CAPABILITY_QUERY,
    CAPABILITY_RESPONSE
}

/** Agent 消息基类 */
open class AgentPayload

/** 任务请求 */
data class TaskRequestPayload(
    val intent: IntentInfo,
    val entities: Map<String, String>,
    val userQuery: String,
    val expectedResponseType: ResponseType,
    val constraints: TaskConstraints? = null
) : AgentPayload()

/** 查询请求 */
data class QueryRequestPayload(
    val query: String,
    val entities: Map<String, String> = emptyMap()
) : AgentPayload()

/** 动作请求 */
data class ActionRequestPayload(
    val actionId: String,
    val params: Map<String, String> = emptyMap()
) : AgentPayload()

/** 意图信息 */
data class IntentInfo(
    val type: String,
    val action: String,
    val confidence: Float
)

/** 响应类型 */
enum class ResponseType {
    RAW_DATA,
    A2UI_JSON,
    HYBRID
}

/** 任务约束 */
data class TaskConstraints(
    val maxItems: Int? = null,
    val timeout: Long? = null,
    val requiredFields: List<String>? = null
)

/** 任务响应 */
data class TaskResponsePayload(
    val status: TaskStatus,
    val data: ResponseData? = null,
    val a2ui: A2UISpec? = null,
    val message: String? = null,
    val followUpActions: List<FollowUpAction>? = null
) : AgentPayload()

/** 任务状态 */
enum class TaskStatus {
    SUCCESS,
    PARTIAL,
    NEED_MORE_INFO,
    NEED_CONFIRMATION,
    FAILED,
    IN_PROGRESS
}

/** 响应数据 */
data class ResponseData(
    val type: String,
    val items: List<Map<String, String>>,
    val metadata: Map<String, String>? = null
)

/** 后续可用动作 */
data class FollowUpAction(
    val id: String,
    val label: String,
    val actionType: String,
    val params: Map<String, String>? = null
)

/** Agent 间通信消息 */
data class AgentMessage(
    val messageId: String,
    val from: AgentIdentity,
    val to: AgentIdentity,
    val type: AgentMessageType,
    val payload: AgentPayload,
    val timestamp: Long = System.currentTimeMillis()
)
