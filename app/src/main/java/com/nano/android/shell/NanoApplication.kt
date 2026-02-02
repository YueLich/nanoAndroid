package com.nano.android.shell

import android.app.Application
import com.nano.android.BuildConfig
import com.nano.framework.server.NanoSystemServer
import com.nano.kernel.NanoLog
import com.nano.kernel.binder.NanoServiceManager
import com.nano.kernel.handler.NanoLooper
import com.nano.llm.model.LLMConfig
import com.nano.llm.model.ProviderType
import com.nano.llm.service.NanoLLMService
import com.nano.sample.calculator.CalculatorAgent
import com.nano.sample.notepad.NotepadAgent
import com.nano.sample.flight.CtripAgent
import com.nano.sample.flight.ChinaSouthernAgent
import com.nano.sample.web.WebAgent

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

        // 配置 NanoLog 使用 Android Log
        NanoLog.setLogOutput(AndroidLogOutput())

        NanoLog.i(TAG, "============================================")
        NanoLog.i(TAG, "NanoAndroid starting...")
        NanoLog.i(TAG, "============================================")

        // Prepare main looper
        NanoLooper.prepareMainLooper()

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

        // 1. 创建系统服务器（使用 Android 桥接的 NanoContext）
        val nanoContext = AndroidNanoContext(this)
        systemServer = NanoSystemServer(nanoContext)

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

        // 从 BuildConfig 读取配置（支持多种 Provider）
        val providerName = BuildConfig.LLM_PROVIDER
        val providerType = when (providerName) {
            "groq" -> ProviderType.OPENAI  // Groq 兼容 OpenAI API
            "openrouter" -> ProviderType.OPENAI
            "together" -> ProviderType.OPENAI
            "openai" -> ProviderType.OPENAI
            "claude" -> ProviderType.CLAUDE
            "local" -> ProviderType.LOCAL
            "mock" -> ProviderType.MOCK
            else -> {
                NanoLog.w(TAG, "Unknown LLM provider: $providerName, falling back to MOCK")
                ProviderType.MOCK
            }
        }

        val baseUrl = when (providerName) {
            "groq" -> "https://api.groq.com/openai/v1"
            "openrouter" -> "https://openrouter.ai/api/v1"
            "together" -> "https://api.together.xyz/v1"
            else -> BuildConfig.LLM_BASE_URL.takeIf { it.isNotEmpty() }
        }

        val model = BuildConfig.LLM_MODEL.takeIf { it.isNotEmpty() } ?: when (providerName) {
            "groq" -> "mixtral-8x7b-32768"
            "openrouter" -> "google/gemini-2.0-flash-exp:free"
            "together" -> "meta-llama/Llama-3-8b-chat-hf"
            "openai" -> "gpt-3.5-turbo"
            "claude" -> "claude-3-haiku-20240307"
            else -> null
        }

        val apiKey = BuildConfig.LLM_API_KEY.takeIf { it.isNotEmpty() }
        val config = LLMConfig(
            providerType = providerType,
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model
        )

        NanoLog.i(TAG, "========== LLM Configuration ==========")
        NanoLog.i(TAG, "Provider Name: $providerName")
        NanoLog.i(TAG, "Provider Type: $providerType")
        NanoLog.i(TAG, "API Key: ${if (apiKey.isNullOrEmpty()) "NOT SET" else "SET (${apiKey.length} chars)"}")
        NanoLog.i(TAG, "Base URL: ${baseUrl ?: "DEFAULT"}")
        NanoLog.i(TAG, "Model: ${model ?: "DEFAULT"}")
        NanoLog.i(TAG, "=======================================")

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
        NanoLog.i(TAG, "Registered Agents: Calculator, Notepad, Ctrip, ChinaSouthern, WebAgent")
        NanoLog.i(TAG, "============================================")
    }

    /**
     * 注册示例 Agents
     */
    private fun registerSampleAgents() {
        try {
            // 1. 注册计算器 Agent
            val calculatorAgent = CalculatorAgent()
            llmService.registerAgent(calculatorAgent)
            NanoLog.i(TAG, "✓ Registered CalculatorAgent: ${calculatorAgent.agentId}")

            // 2. 注册笔记本 Agent
            val notepadAgent = NotepadAgent()
            llmService.registerAgent(notepadAgent)
            NanoLog.i(TAG, "✓ Registered NotepadAgent: ${notepadAgent.agentId}")

            // 3. 注册携程 Agent
            val ctripAgent = CtripAgent()
            llmService.registerAgent(ctripAgent)
            NanoLog.i(TAG, "✓ Registered CtripAgent: ${ctripAgent.agentId}")

            // 4. 注册南方航空 Agent
            val chinaSouthernAgent = ChinaSouthernAgent()
            llmService.registerAgent(chinaSouthernAgent)
            NanoLog.i(TAG, "✓ Registered ChinaSouthernAgent: ${chinaSouthernAgent.agentId}")

            // 5. 注册网页浏览器 Agent
            val webAgent = WebAgent()
            llmService.registerAgent(webAgent)
            NanoLog.i(TAG, "✓ Registered WebAgent: ${webAgent.agentId}")

            // 记录已注册的 Agent 能力
            NanoLog.i(TAG, "✓ Agent Capabilities:")
            NanoLog.i(TAG, "  - calculator: calculate")
            NanoLog.i(TAG, "  - notepad: add_note, list_notes, view_note, edit_note, delete_note")
            NanoLog.i(TAG, "  - ctrip: search_flight, book_flight")
            NanoLog.i(TAG, "  - china_southern: search_flight, book_flight")
            NanoLog.i(TAG, "  - web_agent: search_flight, navigate_web, extract_data")

        } catch (e: Exception) {
            NanoLog.e(TAG, "Failed to register sample agents", e)
        }
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
