package com.nano.view.widget

import com.nano.view.NanoView
import com.nano.view.NanoViewGroup

/**
 * NanoLinearLayout - 线性布局容器
 *
 * 模拟 Android LinearLayout，支持水平或垂直排列子视图
 */
class NanoLinearLayout : NanoViewGroup() {

    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }

    /** 方向 */
    var orientation: Int = VERTICAL
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 测量所有子视图
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        var totalWidth = 0
        var totalHeight = 0
        var maxWidth = 0
        var maxHeight = 0

        // 根据方向计算尺寸
        for (i in 0 until getChildCount()) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }

            if (orientation == VERTICAL) {
                // 垂直方向：累加高度，取最大宽度
                totalHeight += child.measuredHeight
                maxWidth = maxWidth.coerceAtLeast(child.measuredWidth)
            } else {
                // 水平方向：累加宽度，取最大高度
                totalWidth += child.measuredWidth
                maxHeight = maxHeight.coerceAtLeast(child.measuredHeight)
            }
        }

        // 根据测量规格确定最终尺寸
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> if (orientation == VERTICAL) {
                maxWidth.coerceAtMost(widthSize)
            } else {
                totalWidth.coerceAtMost(widthSize)
            }
            else -> if (orientation == VERTICAL) maxWidth else totalWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> if (orientation == VERTICAL) {
                totalHeight.coerceAtMost(heightSize)
            } else {
                maxHeight.coerceAtMost(heightSize)
            }
            else -> if (orientation == VERTICAL) totalHeight else maxHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var currentLeft = 0
        var currentTop = 0

        for (i in 0 until getChildCount()) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }

            if (orientation == VERTICAL) {
                // 垂直布局
                val childLeft = 0
                val childTop = currentTop
                val childRight = childLeft + child.measuredWidth
                val childBottom = childTop + child.measuredHeight

                layoutChild(child, childLeft, childTop, childRight, childBottom)
                currentTop = childBottom
            } else {
                // 水平布局
                val childLeft = currentLeft
                val childTop = 0
                val childRight = childLeft + child.measuredWidth
                val childBottom = childTop + child.measuredHeight

                layoutChild(child, childLeft, childTop, childRight, childBottom)
                currentLeft = childRight
            }
        }
    }

    override fun toString(): String {
        return "NanoLinearLayout(id=$id, orientation=${if (orientation == VERTICAL) "VERTICAL" else "HORIZONTAL"}, children=${getChildCount()})"
    }
}
