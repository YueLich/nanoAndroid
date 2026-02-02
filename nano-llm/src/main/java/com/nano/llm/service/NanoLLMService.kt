package com.nano.llm.service

import com.nano.kernel.NanoLog
import com.nano.kernel.binder.NanoBinder
import com.nano.kernel.binder.NanoParcel
import com.nano.llm.agent.*
import com.nano.llm.api.NaturalLanguageAPI
import com.nano.llm.intent.IntentParser
import com.nano.llm.model.LLMConfig
import com.nano.llm.model.ProviderType
import com.nano.llm.provider.LLMProvider
import com.nano.llm.provider.LLMProviderFactory
import kotlinx.coroutines.*

/**
 * NanoLLMService - LLM 系统服务
 *
 * 作为系统服务通过 NanoBinder 暴露，负责：
 * 1. 管理 LLM Provider 生命周期
 * 2. 管理 Agent 注册表
 * 3. 提供自然语言处理能力
 *
 * 通过 NanoServiceManager 注册后，其他模块可通过 Binder 事务调用，
 * 也可直接获取实例调用 suspend 方法。
 *
 * Binder 事务码：
 * - TRANSACT_PROCESS_INPUT: 处理用户自然语言输入
 * - TRANSACT_REGISTER_AGENT: 注册 App Agent
 * - TRANSACT_UNREGISTER_AGENT: 注销 App Agent
 * - TRANSACT_GET_CAPABILITIES: 获取所有 Agent 能力
 * - TRANSACT_GET_STATUS: 获取服务状态
 */
class NanoLLMService(
    private val config: LLMConfig = LLMConfig(providerType = ProviderType.MOCK)
) : NanoBinder() {

    companion object {
        private const val TAG = "NanoLLMService"

        /** 服务描述符 */
        const val DESCRIPTOR = "com.nano.llm.INanoLLMService"

        // 事务码定义
        const val TRANSACT_PROCESS_INPUT = FIRST_CALL_TRANSACTION
        const val TRANSACT_REGISTER_AGENT = FIRST_CALL_TRANSACTION + 1
        const val TRANSACT_UNREGISTER_AGENT = FIRST_CALL_TRANSACTION + 2
        const val TRANSACT_GET_CAPABILITIES = FIRST_CALL_TRANSACTION + 3
        const val TRANSACT_GET_STATUS = FIRST_CALL_TRANSACTION + 4
    }

    /** LLM 提供商实例 */
    private lateinit var provider: LLMProvider

    /** Agent 注册表 */
    private val agentRegistry = AgentRegistry()

    /** 意图解析器 */
    private lateinit var intentParser: IntentParser

    /** 自然语言 API */
    private lateinit var naturalLanguageAPI: NaturalLanguageAPI

    /** 服务级别协程作用域 */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 服务运行状态 */
    @Volatile
    private var isRunning = false

    init {
        attachInterface(this, DESCRIPTOR)
    }

    /**
     * 启动 LLM 服务
     *
     * 初始化 Provider → IntentParser → NaturalLanguageAPI
     */
    fun start() {
        NanoLog.i(TAG, "Starting NanoLLMService...")

        provider = LLMProviderFactory.create(config)

        NanoLog.i(TAG, "========== LLM Provider Status ==========")
        NanoLog.i(TAG, "Provider Type: ${provider.providerType}")
        NanoLog.i(TAG, "Provider Available: ${provider.isAvailable}")
        if (!provider.isAvailable) {
            NanoLog.w(TAG, "⚠️  LLM Provider is NOT available!")
            NanoLog.w(TAG, "⚠️  System will use mock responses")
            NanoLog.w(TAG, "⚠️  Please configure API key in local.properties")
        }
        NanoLog.i(TAG, "========================================")

        intentParser = IntentParser(provider)

        naturalLanguageAPI = NaturalLanguageAPI(
            agentRegistry = agentRegistry,
            systemAgent = SystemAgent(agentRegistry, AgentCoordinator(), ResponseAggregator()),
            intentParser = intentParser
        )

        isRunning = true
        NanoLog.i(TAG, "NanoLLMService started")
    }

    /**
     * 系统就绪回调
     *
     * 在 NanoSystemServer 所有服务启动后调用。
     */
    fun systemReady() {
        NanoLog.i(TAG, "NanoLLMService system ready")
    }

    /**
     * 停止服务并释放资源
     */
    fun stop() {
        NanoLog.i(TAG, "Stopping NanoLLMService...")
        isRunning = false
        serviceScope.cancel()
        NanoLog.i(TAG, "NanoLLMService stopped")
    }

    // ==================== Agent 管理 ====================

    /** 注册 App Agent */
    fun registerAgent(agent: AppAgent) {
        agentRegistry.registerAgent(agent)

        // 如果是 BaseAppAgent，注入 LLM Provider
        if (agent is com.nano.llm.agent.BaseAppAgent) {
            agent.setLLMProvider(provider)
            NanoLog.i(TAG, "Agent registered with LLM support: ${agent.agentId}")
        } else {
            NanoLog.i(TAG, "Agent registered: ${agent.agentId}")
        }
    }

    /** 注销 App Agent */
    fun unregisterAgent(agentId: String) {
        agentRegistry.unregisterAgent(agentId)
        NanoLog.i(TAG, "Agent unregistered: $agentId")
    }

    /** 获取 Agent 注册表 */
    fun getAgentRegistry(): AgentRegistry = agentRegistry

    /** 获取自然语言 API */
    fun getNaturalLanguageAPI(): NaturalLanguageAPI = naturalLanguageAPI

    /** 服务是否运行中 */
    fun isRunning(): Boolean = isRunning

    // ==================== Binder 事务处理 ====================

    override fun onTransact(
        code: Int,
        data: NanoParcel,
        reply: NanoParcel?,
        flags: Int
    ): Boolean {
        data.enforceInterface(DESCRIPTOR)

        return when (code) {
            TRANSACT_PROCESS_INPUT -> {
                val input = data.readString() ?: ""
                val conversationId = data.readString() ?: ""

                // 同步阻塞等待结果
                val result = serviceScope.async {
                    val context = ConversationContext(
                        conversationId = conversationId,
                        userQuery = input
                    )
                    naturalLanguageAPI.processUserInput(input, context)
                }

                // 将结果写入 reply（使用 runBlocking 等待）
                try {
                    val response = kotlinx.coroutines.runBlocking { result.await() }
                    reply?.writeString(response.message)
                    reply?.writeBoolean(response.a2ui != null)
                    reply?.writeInt(response.participatingAgents.size)
                } catch (e: Exception) {
                    NanoLog.e(TAG, "processInput failed", e)
                    reply?.writeString("Error: ${e.message}")
                    reply?.writeBoolean(false)
                    reply?.writeInt(0)
                }
                true
            }

            TRANSACT_REGISTER_AGENT -> {
                // Agent 注册通过直接调用 registerAgent() 完成
                // Binder 事务仅返回确认
                reply?.writeBoolean(true)
                true
            }

            TRANSACT_UNREGISTER_AGENT -> {
                val agentId = data.readString() ?: ""
                unregisterAgent(agentId)
                reply?.writeBoolean(true)
                true
            }

            TRANSACT_GET_CAPABILITIES -> {
                val capabilities = agentRegistry.getAllCapabilities()
                reply?.writeInt(capabilities.size)
                capabilities.forEach { cap ->
                    reply?.writeString(cap.agentId)
                    reply?.writeString(cap.exampleQueries.joinToString(","))
                }
                true
            }

            TRANSACT_GET_STATUS -> {
                reply?.writeBoolean(isRunning)
                reply?.writeString(provider.providerType.name)
                reply?.writeBoolean(provider.isAvailable)
                reply?.writeInt(agentRegistry.getAgentCount())
                true
            }

            else -> false
        }
    }
}
