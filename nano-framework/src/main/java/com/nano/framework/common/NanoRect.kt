package com.nano.framework.common

/**
 * NanoRect - 纯 Kotlin 的矩形类
 *
 * 不依赖 Android Rect，提供基本的矩形操作
 */
data class NanoRect(
    var left: Int = 0,
    var top: Int = 0,
    var right: Int = 0,
    var bottom: Int = 0
) {
    /**
     * 获取宽度
     */
    fun width(): Int = right - left

    /**
     * 获取高度
     */
    fun height(): Int = bottom - top

    /**
     * 设置矩形边界
     */
    fun set(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    /**
     * 检查是否为空（宽度或高度为 0）
     */
    fun isEmpty(): Boolean = width() <= 0 || height() <= 0

    /**
     * 检查是否包含指定点
     */
    fun contains(x: Int, y: Int): Boolean {
        return x >= left && x < right && y >= top && y < bottom
    }

    /**
     * 检查是否与另一个矩形相交
     */
    fun intersects(other: NanoRect): Boolean {
        return left < other.right && right > other.left &&
                top < other.bottom && bottom > other.top
    }

    /**
     * 偏移矩形
     */
    fun offset(dx: Int, dy: Int) {
        left += dx
        top += dy
        right += dx
        bottom += dy
    }

    /**
     * 复制矩形
     */
    fun copy(): NanoRect = NanoRect(left, top, right, bottom)

    override fun toString(): String {
        return "NanoRect($left, $top, $right, $bottom) [${width()}x${height()}]"
    }
}
