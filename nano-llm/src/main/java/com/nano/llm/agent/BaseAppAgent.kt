package com.nano.llm.agent

import com.nano.llm.a2ui.A2UIAction

/**
 * App Agent 基类 - 提供通用实现
 */
abstract class BaseAppAgent(
    override val agentId: String,
    override val agentName: String
) : AppAgent {

    override suspend fun handleRequest(request: AgentMessage): TaskResponsePayload {
        return when (request.type) {
            AgentMessageType.TASK_REQUEST -> handleTaskRequest(
                request.payload as TaskRequestPayload
            )
            AgentMessageType.QUERY_REQUEST -> handleQueryRequest(
                request.payload as QueryRequestPayload
            )
            AgentMessageType.ACTION_REQUEST -> handleActionRequest(
                request.payload as ActionRequestPayload
            )
            else -> TaskResponsePayload(
                status = TaskStatus.FAILED,
                message = "Unsupported message type: ${request.type}"
            )
        }
    }

    /** 处理任务请求 - 子类实现 */
    protected abstract suspend fun handleTaskRequest(
        request: TaskRequestPayload
    ): TaskResponsePayload

    /** 处理查询请求 */
    protected open suspend fun handleQueryRequest(
        request: QueryRequestPayload
    ): TaskResponsePayload = TaskResponsePayload(
        status = TaskStatus.FAILED,
        message = "Query not supported"
    )

    /** 处理动作请求 */
    protected open suspend fun handleActionRequest(
        request: ActionRequestPayload
    ): TaskResponsePayload = TaskResponsePayload(
        status = TaskStatus.FAILED,
        message = "Action not supported"
    )

    override suspend fun handleUIAction(action: A2UIAction): TaskResponsePayload {
        return TaskResponsePayload(
            status = TaskStatus.FAILED,
            message = "UI action not supported: ${action.method}"
        )
    }
}
