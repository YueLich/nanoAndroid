package com.nano.framework.wm

import android.content.Context
import android.graphics.Rect
import com.nano.framework.am.NanoActivityManagerService
import com.nano.kernel.NanoLog
import com.nano.kernel.binder.NanoBinder
import com.nano.kernel.binder.NanoParcel
import java.util.concurrent.ConcurrentHashMap

/**
 * NanoWindowManagerService - 窗口管理服务
 *
 * 负责：
 * 1. 窗口的创建和销毁
 * 2. Z-order（层级）管理
 * 3. 窗口布局计算
 * 4. 焦点管理
 *
 * 简化实现：
 * - 不涉及真实的 View 渲染
 * - 只管理窗口的抽象状态
 * - 实际的 View 容器由 Shell Activity 管理
 */
class NanoWindowManagerService(
    private val context: Context,
    private val activityManager: NanoActivityManagerService
) : NanoBinder(), INanoWindowManager {

    companion object {
        private const val TAG = "NanoWindowManagerService"
    }

    /** 所有窗口 */
    private val windows = ConcurrentHashMap<String, NanoWindowState>()

    /** 当前焦点窗口 Token */
    private var focusedWindowToken: String? = null

    /** 锁对象 */
    private val lock = Any()

    init {
        attachInterface(this, INanoWindowManager.DESCRIPTOR)
        NanoLog.d(TAG, "NanoWindowManagerService created")
    }

    // ==================== Window Management ====================

    override fun addWindow(
        token: String,
        type: NanoWindowState.WindowType,
        frame: Rect
    ): Boolean {
        synchronized(lock) {
            if (windows.containsKey(token)) {
                NanoLog.w(TAG, "Window already exists: $token")
                return false
            }

            // 创建窗口
            val window = NanoWindowState(
                token = token,
                type = type,
                frame = frame,
                layer = calculateLayer(type)
            )

            windows[token] = window
            NanoLog.i(TAG, "Window added: ${window.type} at layer ${window.layer}")

            // 如果是第一个窗口或应用窗口，设置焦点
            if (focusedWindowToken == null ||
                type == NanoWindowState.WindowType.TYPE_APPLICATION) {
                setFocusedWindow(token)
            }

            return true
        }
    }

    /**
     * 计算窗口层级
     */
    private fun calculateLayer(type: NanoWindowState.WindowType): Int {
        return when (type) {
            NanoWindowState.WindowType.TYPE_BASE_APPLICATION -> type.baseLayer
            NanoWindowState.WindowType.TYPE_APPLICATION -> {
                // 应用窗口，层级为现有应用窗口数量 + 2
                val appWindows = windows.values.count {
                    it.type == NanoWindowState.WindowType.TYPE_APPLICATION
                }
                type.baseLayer + appWindows
            }
            else -> type.baseLayer
        }
    }

    override fun removeWindow(token: String): Boolean {
        synchronized(lock) {
            val window = windows.remove(token)
            if (window == null) {
                NanoLog.w(TAG, "Window not found: $token")
                return false
            }

            NanoLog.i(TAG, "Window removed: ${window.type}")

            // 如果是焦点窗口，切换焦点
            if (focusedWindowToken == token) {
                focusedWindowToken = null
                // 将焦点设置到最顶层的可见窗口
                getTopVisibleWindow()?.let { setFocusedWindow(it.token) }
            }

            return true
        }
    }

    override fun updateWindowLayout(token: String, frame: Rect): Boolean {
        synchronized(lock) {
            val window = windows[token]
            if (window == null) {
                NanoLog.w(TAG, "Window not found: $token")
                return false
            }

            window.frame.set(frame)
            NanoLog.d(TAG, "Window layout updated: $token")
            return true
        }
    }

    override fun setWindowVisibility(token: String, visible: Boolean): Boolean {
        synchronized(lock) {
            val window = windows[token]
            if (window == null) {
                NanoLog.w(TAG, "Window not found: $token")
                return false
            }

            window.isVisible = visible
            NanoLog.d(TAG, "Window visibility changed: $token -> $visible")

            // 如果隐藏的是焦点窗口，切换焦点
            if (!visible && focusedWindowToken == token) {
                focusedWindowToken = null
                getTopVisibleWindow()?.let { setFocusedWindow(it.token) }
            }

            return true
        }
    }

    override fun setFocusedWindow(token: String): Boolean {
        synchronized(lock) {
            val window = windows[token]
            if (window == null) {
                NanoLog.w(TAG, "Window not found: $token")
                return false
            }

            if (!window.isVisible) {
                NanoLog.w(TAG, "Cannot focus invisible window: $token")
                return false
            }

            // 清除旧焦点
            focusedWindowToken?.let {
                windows[it]?.isFocused = false
            }

            // 设置新焦点
            window.isFocused = true
            focusedWindowToken = token

            NanoLog.i(TAG, "Focus changed to: ${window.type}")
            return true
        }
    }

    override fun getFocusedWindow(): NanoWindowState? {
        synchronized(lock) {
            return focusedWindowToken?.let { windows[it] }
        }
    }

    override fun getAllWindows(): List<NanoWindowState> {
        synchronized(lock) {
            // 按层级排序（从底到顶）
            return windows.values.sortedBy { it.layer }
        }
    }

    /**
     * 获取最顶层的可见窗口
     */
    private fun getTopVisibleWindow(): NanoWindowState? {
        return windows.values
            .filter { it.isVisible }
            .maxByOrNull { it.layer }
    }

    /**
     * 模拟触摸事件分发
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 处理事件的窗口，null 表示没有窗口处理
     */
    fun dispatchTouchEvent(x: Int, y: Int): NanoWindowState? {
        synchronized(lock) {
            // 从顶层窗口开始查找
            return windows.values
                .filter { it.isVisible }
                .sortedByDescending { it.layer }
                .firstOrNull { it.containsPoint(x, y) }
        }
    }

    // ==================== Binder Implementation ====================

    override fun onTransact(
        code: Int,
        data: NanoParcel,
        reply: NanoParcel?,
        flags: Int
    ): Boolean {
        when (code) {
            INanoWindowManager.TRANSACTION_ADD_WINDOW -> {
                data.enforceInterface(INanoWindowManager.DESCRIPTOR)
                val token = data.readString() ?: return false
                val typeOrdinal = data.readInt()
                val type = NanoWindowState.WindowType.values()[typeOrdinal]
                val left = data.readInt()
                val top = data.readInt()
                val right = data.readInt()
                val bottom = data.readInt()
                val frame = Rect(left, top, right, bottom)
                val result = addWindow(token, type, frame)
                reply?.writeBoolean(result)
                return true
            }

            INanoWindowManager.TRANSACTION_REMOVE_WINDOW -> {
                data.enforceInterface(INanoWindowManager.DESCRIPTOR)
                val token = data.readString() ?: return false
                val result = removeWindow(token)
                reply?.writeBoolean(result)
                return true
            }

            INanoWindowManager.TRANSACTION_SET_FOCUSED_WINDOW -> {
                data.enforceInterface(INanoWindowManager.DESCRIPTOR)
                val token = data.readString() ?: return false
                val result = setFocusedWindow(token)
                reply?.writeBoolean(result)
                return true
            }

            INanoWindowManager.TRANSACTION_GET_ALL_WINDOWS -> {
                data.enforceInterface(INanoWindowManager.DESCRIPTOR)
                val allWindows = getAllWindows()
                reply?.writeInt(allWindows.size)
                allWindows.forEach {
                    reply?.writeString(it.token)
                    reply?.writeInt(it.type.ordinal)
                    reply?.writeInt(it.layer)
                    reply?.writeBoolean(it.isVisible)
                    reply?.writeBoolean(it.isFocused)
                }
                return true
            }
        }

        return false
    }

    /**
     * 系统就绪回调
     */
    fun systemReady() {
        NanoLog.i(TAG, "WindowManagerService is ready")
    }
}
