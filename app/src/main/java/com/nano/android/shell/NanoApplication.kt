package com.nano.android.shell

import android.app.Application
import com.nano.framework.server.NanoSystemServer
import com.nano.kernel.NanoLog
import com.nano.kernel.binder.NanoServiceManager
import com.nano.llm.model.LLMConfig
import com.nano.llm.model.ProviderType
import com.nano.llm.service.NanoLLMService
import com.nano.sample.calculator.CalculatorAgent
import com.nano.sample.notepad.NotepadAgent

/**
 * NanoAndroid 应用入口
 *
 * 负责初始化整个 NanoAndroid 系统：
 * 1. 启动 NanoSystemServer（PMS、AMS、WMS）
 * 2. 注册 NanoLLMService 作为系统服务
 * 3. 注册示例 Agents（Calculator、Notepad）
 */
class NanoApplication : Application() {

    companion object {
        private const val TAG = "NanoApplication"

        @Volatile
        private var instance: NanoApplication? = null

        fun getInstance(): NanoApplication =
            instance ?: throw IllegalStateException("NanoApplication not initialized")
    }

    // 系统服务器
    private lateinit var systemServer: NanoSystemServer

    // LLM 服务
    private lateinit var llmService: NanoLLMService

    override fun onCreate() {
        super.onCreate()
        instance = this

        NanoLog.i(TAG, "============================================")
        NanoLog.i(TAG, "NanoAndroid starting...")
        NanoLog.i(TAG, "============================================")

        // 初始化系统服务
        initializeSystemServer()
    }

    /**
     * 初始化系统服务器
     *
     * 流程：
     * 1. 创建 NanoSystemServer
     * 2. 注册外部服务工厂（NanoLLMService）
     * 3. 注册系统就绪回调（注册 Sample Agents）
     * 4. 启动 SystemServer
     */
    private fun initializeSystemServer() {
        NanoLog.i(TAG, "Initializing NanoSystemServer...")

        // 1. 创建系统服务器
        systemServer = NanoSystemServer(this)

        // 2. 注册 LLM 服务工厂（避免 nano-framework 循环依赖）
        systemServer.registerExternalService(NanoSystemServer.LLM_SERVICE) {
            createLLMService()
        }

        // 3. 注册系统就绪回调，启动后注册 Sample Agents
        systemServer.registerSystemReadyCallback {
            onSystemReady()
        }

        // 4. 启动系统服务器（异步在后台线程启动）
        systemServer.run()

        NanoLog.i(TAG, "NanoSystemServer initialization requested")
    }

    /**
     * 创建 LLM 服务实例
     */
    private fun createLLMService(): NanoLLMService {
        NanoLog.i(TAG, "Creating NanoLLMService...")

        // 使用 MockLLMProvider（开发测试阶段）
        // 生产环境可切换为 OpenAI / Claude / Local
        val config = LLMConfig(
            providerType = ProviderType.MOCK,
            apiKey = "" // Mock 不需要 API key
        )

        llmService = NanoLLMService(config)
        llmService.start()

        NanoLog.i(TAG, "NanoLLMService created and started")
        return llmService
    }

    /**
     * 系统就绪回调
     *
     * 所有系统服务（PMS、AMS、WMS、LLM）启动后调用。
     * 此时可以安全地注册应用级 Agents。
     */
    private fun onSystemReady() {
        NanoLog.i(TAG, "System ready, registering sample agents...")

        // 从 ServiceManager 获取 LLM 服务（验证注册成功）
        val llmBinder = NanoServiceManager.getService(NanoSystemServer.LLM_SERVICE)
        if (llmBinder == null) {
            NanoLog.e(TAG, "Failed to get LLM service from ServiceManager!")
            return
        }

        // 注册示例 Agents
        registerSampleAgents()

        NanoLog.i(TAG, "============================================")
        NanoLog.i(TAG, "NanoAndroid fully initialized ✓")
        NanoLog.i(TAG, "Registered Agents: Calculator, Notepad")
        NanoLog.i(TAG, "============================================")
    }

    /**
     * 注册示例 Agents
     */
    private fun registerSampleAgents() {
        // 注册计算器 Agent
        val calculatorAgent = CalculatorAgent()
        llmService.registerAgent(calculatorAgent)
        NanoLog.i(TAG, "Registered CalculatorAgent: ${calculatorAgent.agentId}")

        // 注册笔记本 Agent
        val notepadAgent = NotepadAgent()
        llmService.registerAgent(notepadAgent)
        NanoLog.i(TAG, "Registered NotepadAgent: ${notepadAgent.agentId}")
    }

    override fun onTerminate() {
        super.onTerminate()
        NanoLog.i(TAG, "NanoAndroid terminating...")
    }

    /**
     * 获取 LLM 服务实例
     *
     * Activity 可通过此方法获取 LLMService 来调用自然语言 API
     */
    fun getLLMService(): NanoLLMService {
        if (!::llmService.isInitialized) {
            throw IllegalStateException("LLM Service not initialized yet")
        }
        return llmService
    }
}
