package com.nano.llm.agent

import com.nano.llm.a2ui.A2UIAction
import com.nano.llm.provider.LLMProvider

/**
 * App Agent 基类 - 提供通用实现
 */
abstract class BaseAppAgent(
    override val agentId: String,
    override val agentName: String
) : AppAgent {

    /**
     * LLM Provider 实例
     *
     * Agent 注册到 LLMService 时自动注入。
     * Agent 可以调用此 provider 执行 LLM 推理任务，如：
     * - 生成文本摘要
     * - 提取关键信息
     * - 智能回复
     * - 内容理解
     *
     * 使用示例：
     * ```kotlin
     * val response = llmProvider?.generate(
     *     LLMRequest(
     *         messages = listOf(
     *             LLMMessage(role = MessageRole.USER, content = "总结这段文本...")
     *         )
     *     )
     * )
     * ```
     */
    protected var llmProvider: LLMProvider? = null
        private set

    /**
     * 设置 LLM Provider（由 LLMService 调用）
     *
     * @param provider LLM 提供商实例
     */
    fun setLLMProvider(provider: LLMProvider) {
        this.llmProvider = provider
    }

    /**
     * 检查 LLM 是否可用
     */
    protected fun isLLMAvailable(): Boolean {
        return llmProvider?.isAvailable == true
    }

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
