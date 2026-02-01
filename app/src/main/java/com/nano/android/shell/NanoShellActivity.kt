package com.nano.android.shell

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nano.a2ui.bridge.A2UIRenderer
import com.nano.llm.a2ui.A2UISpec
import com.nano.android.R
import com.nano.framework.ui.ChatMessage
import com.nano.framework.ui.ILauncherView
import com.nano.kernel.NanoLog
import kotlinx.coroutines.*

/**
 * NanoAndroid Shell Activity
 *
 * 实现 ILauncherView 接口，提供对话流界面：
 * 1. 双容器布局：A2UI 渲染容器（上）+ 聊天消息列表（中）+ 输入框（下）
 * 2. 使用 LauncherUI 作为核心控制器
 * 3. 复用 A2UIRenderer 和 NanoViewConverter 渲染 A2UI
 */
class NanoShellActivity : ComponentActivity(), ILauncherView {

    companion object {
        private const val TAG = "NanoShellActivity"
    }

    // UI 组件
    private lateinit var recyclerChat: RecyclerView
    private lateinit var containerA2UI: FrameLayout
    private lateinit var etUserInput: EditText
    private lateinit var btnSend: Button

    // Adapter
    private lateinit var adapter: ChatMessageAdapter

    // 核心控制器
    private lateinit var launcherUI: LauncherUI

    // Activity 级别协程作用域
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // A2UI 动作监听器（由 LauncherUI 设置）
    private var onA2UIActionListener: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NanoLog.i(TAG, "NanoShellActivity onCreate")

        // 设置布局
        setContentView(R.layout.activity_shell)

        // 初始化 UI
        initializeViews()

        // 延迟初始化 LauncherUI（等待系统就绪）
        activityScope.launch {
            delay(1000) // 给系统 1 秒启动时间
            initializeLauncherUI()
        }
    }

    private fun initializeViews() {
        // 1. 绑定视图
        recyclerChat = findViewById(R.id.recyclerChat)
        containerA2UI = findViewById(R.id.containerA2UI)
        etUserInput = findViewById(R.id.etUserInput)
        btnSend = findViewById(R.id.btnSend)

        // 2. 初始化 RecyclerView + Adapter
        adapter = ChatMessageAdapter { suggestion ->
            // 点击建议 Chip 时，作为新的用户输入发送
            etUserInput.setText(suggestion)
            activityScope.launch {
                launcherUI.onUserSendMessage(suggestion)
            }
        }
        recyclerChat.adapter = adapter
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.itemAnimator = null // 禁用动画，提升性能

        // 3. 初始状态：隐藏 A2UI 容器
        showA2UIContainer(false)

        // 4. 设置发送按钮点击事件（临时，最终由 setOnSendListener 设置）
        btnSend.setOnClickListener {
            val input = etUserInput.text.toString().trim()
            if (input.isNotEmpty()) {
                activityScope.launch {
                    launcherUI.onUserSendMessage(input)
                }
            }
        }
    }

    private suspend fun initializeLauncherUI() {
        try {
            // 获取 NaturalLanguageAPI
            val app = application as NanoApplication
            val llmService = app.getLLMService()
            val api = llmService.getNaturalLanguageAPI()

            // 创建 LauncherUI
            launcherUI = LauncherUI(api, this)

            // 绑定事件监听器
            setOnSendListener { input ->
                activityScope.launch { launcherUI.onUserSendMessage(input) }
            }

            // 添加欢迎消息
            withContext(Dispatchers.Main) {
                val welcomeMsg = ChatMessage.SystemMessage(
                    id = "welcome_${System.currentTimeMillis()}",
                    timestamp = System.currentTimeMillis(),
                    message = "欢迎使用 NanoAndroid！您可以使用自然语言与系统交互。",
                    suggestions = listOf("计算 2 + 3", "新增笔记：学习 Android")
                )
                addMessage(welcomeMsg)
            }

            NanoLog.i(TAG, "LauncherUI initialized, ready for input")
        } catch (e: Exception) {
            NanoLog.e(TAG, "Failed to initialize LauncherUI", e)
            withContext(Dispatchers.Main) {
                val errorMsg = ChatMessage.SystemMessage(
                    id = "error_${System.currentTimeMillis()}",
                    timestamp = System.currentTimeMillis(),
                    message = "系统初始化失败：${e.message}"
                )
                addMessage(errorMsg)
            }
        }
    }

    // ==================== ILauncherView 实现（聊天消息管理）====================

    override fun addMessage(message: ChatMessage) {
        adapter.addMessage(message)
    }

    override fun updateMessage(messageId: String, message: ChatMessage) {
        adapter.updateMessage(messageId, message)
    }

    override fun removeMessage(messageId: String) {
        adapter.removeMessage(messageId)
    }

    override fun scrollChatToBottom() {
        recyclerChat.smoothScrollToPosition(adapter.itemCount - 1)
    }

    override fun clearInput() {
        etUserInput.text.clear()
    }

    // ==================== ILauncherView 实现（A2UI 渲染）====================

    override fun renderA2UI(spec: Any, agentName: String) {
        try {
            NanoLog.i(TAG, "Rendering A2UI from agent: $agentName")

            // 类型转换
            if (spec !is A2UISpec) {
                throw IllegalArgumentException("spec must be A2UISpec")
            }

            // 1. 清空容器
            containerA2UI.removeAllViews()

            // 2. 渲染 A2UI：A2UISpec → NanoView → Android View
            val renderer = A2UIRenderer()
            val nanoView = renderer.render(spec)

            val converter = NanoViewConverter(this) { action ->
                // A2UI 按钮点击回调
                onA2UIActionListener?.invoke(action)
            }
            val androidView = converter.convert(nanoView)

            // 3. 添加到容器
            containerA2UI.addView(androidView)

            NanoLog.i(TAG, "A2UI rendered successfully")
        } catch (e: Exception) {
            NanoLog.e(TAG, "Failed to render A2UI", e)
            // 降级显示错误
            val errorView = TextView(this).apply {
                text = "Failed to render A2UI: ${e.message}"
                setTextColor(Color.RED)
                setPadding(16, 16, 16, 16)
            }
            containerA2UI.removeAllViews()
            containerA2UI.addView(errorView)
        }
    }

    override fun clearA2UI() {
        containerA2UI.removeAllViews()
    }

    override fun showA2UIContainer(show: Boolean) {
        containerA2UI.visibility = if (show) View.VISIBLE else View.GONE

        // 动态调整布局权重
        val params = containerA2UI.layoutParams as LinearLayout.LayoutParams
        params.weight = if (show) 0.5f else 0f
        containerA2UI.layoutParams = params

        // 调整聊天容器权重
        val chatParams = recyclerChat.layoutParams as LinearLayout.LayoutParams
        chatParams.weight = if (show) 0.5f else 1f
        recyclerChat.layoutParams = chatParams
    }

    // ==================== ILauncherView 实现（用户交互回调）====================

    override fun setOnSendListener(listener: (String) -> Unit) {
        btnSend.setOnClickListener {
            val input = etUserInput.text.toString().trim()
            if (input.isNotEmpty()) {
                listener(input)
            }
        }
    }

    override fun setOnA2UIActionListener(listener: (String) -> Unit) {
        onA2UIActionListener = listener
    }

    // ==================== Activity 生命周期 ====================

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        NanoLog.i(TAG, "NanoShellActivity onDestroy")
    }
}
