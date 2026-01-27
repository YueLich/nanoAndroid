package com.nano.view.widget

import com.nano.view.NanoView

/**
 * NanoTextView - 文本视图
 *
 * 模拟 Android TextView
 */
open class NanoTextView : NanoView() {

    /** 文本内容 */
    var text: String = ""
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** 文本大小 */
    var textSize: Float = 14f
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** 文本颜色（ARGB 格式） */
    var textColor: Int = 0xFF000000.toInt() // 黑色
        set(value) {
            if (field != value) {
                field = value
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 简化实现：使用文本长度估算宽度
        val textWidth = (text.length * textSize).toInt()
        val textHeight = (textSize * 1.5f).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> textWidth.coerceAtMost(widthSize)
            else -> textWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> textHeight.coerceAtMost(heightSize)
            else -> textHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw() {
        // 实际绘制由真实 Android View 系统处理
        // 这里只是模拟
    }

    override fun toString(): String {
        return "NanoTextView(id=$id, text='$text', size=${textSize}sp)"
    }
}
