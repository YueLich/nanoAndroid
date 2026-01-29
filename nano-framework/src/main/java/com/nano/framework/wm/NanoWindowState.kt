package com.nano.framework.wm

import com.nano.framework.common.NanoRect
import com.nano.framework.am.NanoActivityRecord

/**
 * NanoWindowState - 窗口状态
 *
 * 描述一个窗口的属性和状态
 */
data class NanoWindowState(
    /** 窗口 Token */
    val token: String,

    /** 窗口类型 */
    val type: WindowType,

    /** 关联的 Activity（如果是应用窗口） */
    val activityRecord: NanoActivityRecord? = null,

    /** Z-order 层级 */
    var layer: Int = 0,

    /** 是否有焦点 */
    var isFocused: Boolean = false,

    /** 是否可见 */
    var isVisible: Boolean = true,

    /** 窗口位置和大小 */
    val frame: NanoRect = NanoRect(),

    /** 创建时间 */
    val createTime: Long = System.currentTimeMillis()
) {

    /**
     * 窗口类型
     */
    enum class WindowType(val baseLayer: Int) {
        /** 应用窗口基础层 */
        TYPE_BASE_APPLICATION(1),

        /** 普通应用窗口 */
        TYPE_APPLICATION(2),

        /** Toast 提示 */
        TYPE_TOAST(1050),

        /** 系统警告窗口 */
        TYPE_SYSTEM_ALERT(1100),

        /** 输入法窗口 */
        TYPE_INPUT_METHOD(1200)
    }

    /**
     * 检查点是否在窗口内
     */
    fun containsPoint(x: Int, y: Int): Boolean {
        return isVisible && frame.contains(x, y)
    }

    override fun toString(): String {
        return "WindowState(token=${token.substring(0, 8)}, " +
                "type=$type, layer=$layer, visible=$isVisible, focused=$isFocused)"
    }
}
