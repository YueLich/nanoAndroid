package com.nano.framework.wm

import com.nano.framework.common.NanoRect
import com.nano.kernel.binder.INanoInterface
import com.nano.kernel.binder.NanoBinder

/**
 * INanoWindowManager - WindowManager Binder 接口
 */
interface INanoWindowManager : INanoInterface {

    /**
     * 添加窗口
     *
     * @param token 窗口 Token
     * @param type 窗口类型
     * @param frame 窗口位置和大小
     * @return 是否成功
     */
    fun addWindow(
        token: String,
        type: NanoWindowState.WindowType,
        frame: NanoRect
    ): Boolean

    /**
     * 移除窗口
     */
    fun removeWindow(token: String): Boolean

    /**
     * 更新窗口布局
     */
    fun updateWindowLayout(token: String, frame: NanoRect): Boolean

    /**
     * 设置窗口可见性
     */
    fun setWindowVisibility(token: String, visible: Boolean): Boolean

    /**
     * 设置焦点窗口
     */
    fun setFocusedWindow(token: String): Boolean

    /**
     * 获取焦点窗口
     */
    fun getFocusedWindow(): NanoWindowState?

    /**
     * 获取所有窗口（按 Z-order 排序）
     */
    fun getAllWindows(): List<NanoWindowState>

    companion object {
        const val DESCRIPTOR = "com.nano.framework.wm.INanoWindowManager"

        // 事务代码
        const val TRANSACTION_ADD_WINDOW = NanoBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_REMOVE_WINDOW = NanoBinder.FIRST_CALL_TRANSACTION + 2
        const val TRANSACTION_UPDATE_WINDOW_LAYOUT = NanoBinder.FIRST_CALL_TRANSACTION + 3
        const val TRANSACTION_SET_WINDOW_VISIBILITY = NanoBinder.FIRST_CALL_TRANSACTION + 4
        const val TRANSACTION_SET_FOCUSED_WINDOW = NanoBinder.FIRST_CALL_TRANSACTION + 5
        const val TRANSACTION_GET_FOCUSED_WINDOW = NanoBinder.FIRST_CALL_TRANSACTION + 6
        const val TRANSACTION_GET_ALL_WINDOWS = NanoBinder.FIRST_CALL_TRANSACTION + 7
    }
}
