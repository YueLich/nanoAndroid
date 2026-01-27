package com.nano.view.widget

/**
 * NanoButton - 按钮视图
 *
 * 模拟 Android Button，继承自 NanoTextView
 */
class NanoButton : NanoTextView() {

    /** 是否启用 */
    var isEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateTextColor()
            }
        }

    /**
     * 更新文本颜色
     */
    private fun updateTextColor() {
        // 禁用时改变文本颜色
        super.textColor = if (isEnabled) {
            0xFF000000.toInt() // 黑色
        } else {
            0xFF888888.toInt() // 灰色
        }
    }

    /** 背景颜色 */
    var backgroundColor: Int = 0xFFCCCCCC.toInt() // 浅灰色

    init {
        // 按钮默认可点击
        isClickable = true
    }

    override fun onTouchEvent(x: Int, y: Int): Boolean {
        // 禁用时不响应触摸事件
        if (!isEnabled) {
            return false
        }
        return super.onTouchEvent(x, y)
    }

    override fun performClick() {
        // 禁用时不执行点击
        if (!isEnabled) {
            return
        }
        super.performClick()
    }

    override fun toString(): String {
        return "NanoButton(id=$id, text='$text', enabled=$isEnabled)"
    }
}
