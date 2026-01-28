package com.nano.android.shell

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.nano.android.R
import com.nano.kernel.NanoLog
import com.nano.llm.agent.ConversationContext
import com.nano.llm.intent.*
import com.nano.llm.service.NanoLLMService
import kotlinx.coroutines.*

/**
 * NanoAndroid Shell Activity
 *
 * 这是 NanoAndroid 的主 Activity，提供：
 * 1. 系统状态展示
 * 2. 自然语言输入测试界面
 * 3. Agent 响应展示
 */
class NanoShellActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NanoShellActivity"
    }

    // UI 组件
    private lateinit var tvSystemStatus: TextView
    private lateinit var tvAgents: TextView
    private lateinit var etUserInput: EditText
    private lateinit var btnProcess: Button
    private lateinit var tvResponse: TextView

    // LLM 服务
    private lateinit var llmService: NanoLLMService

    // 对话上下文
    private var conversationContext = ConversationContext(
        conversationId = "shell_conv_${System.currentTimeMillis()}"
    )

    // Activity 级别协程作用域
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NanoLog.i(TAG, "NanoShellActivity onCreate")

        // 设置布局
        setContentView(R.layout.activity_shell)

        // 初始化 UI
        initializeViews()

        // 延迟获取 LLM 服务（等待系统就绪）
        activityScope.launch {
            delay(1000) // 给系统 1 秒启动时间
            initializeLLMService()
        }
    }

    private fun initializeViews() {
        tvSystemStatus = findViewById(R.id.tvSystemStatus)
        tvAgents = findViewById(R.id.tvAgents)
        etUserInput = findViewById(R.id.etUserInput)
        btnProcess = findViewById(R.id.btnProcess)
        tvResponse = findViewById(R.id.tvResponse)

        btnProcess.setOnClickListener {
            val input = etUserInput.text.toString().trim()
            if (input.isNotEmpty()) {
                processUserInput(input)
            } else {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun initializeLLMService() {
        try {
            val app = application as NanoApplication
            llmService = app.getLLMService()

            // 更新 UI
            withContext(Dispatchers.Main) {
                tvSystemStatus.text = "System Status: ✓ Ready"
                val registry = llmService.getAgentRegistry()
                val agents = registry.getAllCapabilities()
                tvAgents.text = "Agents: ${agents.joinToString(", ") { it.agentId }} (${agents.size} total)"
                btnProcess.isEnabled = true
            }

            NanoLog.i(TAG, "LLM Service initialized, ready for input")
        } catch (e: Exception) {
            NanoLog.e(TAG, "Failed to initialize LLM service", e)
            withContext(Dispatchers.Main) {
                tvSystemStatus.text = "System Status: ✗ Error - ${e.message}"
                btnProcess.isEnabled = false
            }
        }
    }

    private fun processUserInput(input: String) {
        NanoLog.i(TAG, "Processing user input: $input")

        // 禁用按钮防止重复提交
        btnProcess.isEnabled = false
        tvResponse.text = "Processing..."

        activityScope.launch {
            try {
                // 更新对话上下文
                conversationContext = conversationContext.withQuery(input)

                // 调用 NaturalLanguageAPI 处理输入
                val api = llmService.getNaturalLanguageAPI()
                val response = api.processUserInput(
                    userInput = input,
                    context = conversationContext,
                    conversationHistory = emptyList()
                )

                // 显示响应
                withContext(Dispatchers.Main) {
                    val displayText = buildString {
                        appendLine("═══ Response ═══")
                        appendLine("Message: ${response.message}")
                        appendLine()
                        appendLine("State: ${response.conversationState}")
                        appendLine()
                        if (response.participatingAgents.isNotEmpty()) {
                            appendLine("Agents: ${response.participatingAgents.joinToString(", ")}")
                            appendLine()
                        }
                        if (response.followUpSuggestions.isNotEmpty()) {
                            appendLine("Suggestions:")
                            response.followUpSuggestions.forEach { suggestion ->
                                appendLine("  • $suggestion")
                            }
                            appendLine()
                        }
                        val a2ui = response.a2ui
                        if (a2ui != null) {
                            appendLine("A2UI Spec: ✓ Available")
                            appendLine("  Root type: ${a2ui.root::class.simpleName}")
                        }
                    }

                    tvResponse.text = displayText
                    btnProcess.isEnabled = true

                    // 清空输入框（可选）
                    // etUserInput.text.clear()
                }

                NanoLog.i(TAG, "Response displayed: ${response.message}")
            } catch (e: Exception) {
                NanoLog.e(TAG, "Error processing input", e)
                withContext(Dispatchers.Main) {
                    tvResponse.text = "Error: ${e.message}\n\n${e.stackTraceToString()}"
                    btnProcess.isEnabled = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NanoLog.i(TAG, "NanoShellActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        NanoLog.i(TAG, "NanoShellActivity onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        NanoLog.i(TAG, "NanoShellActivity onDestroy")
    }
}
