package com.nano.view

import com.nano.kernel.NanoLog

/**
 * NanoViewGroup - 视图容器类
 *
 * 模拟 Android ViewGroup，可以包含子视图
 */
abstract class NanoViewGroup : NanoView() {

    companion object {
        private const val TAG = "NanoViewGroup"
    }

    /** 子视图列表 */
    private val children: MutableList<NanoView> = mutableListOf()

    // ==================== 子视图管理 ====================

    /**
     * 添加子视图
     */
    fun addView(child: NanoView) {
        if (child.parent != null) {
            throw IllegalStateException("View already has a parent")
        }

        children.add(child)
        child.parent = this
        requestLayout()
    }

    /**
     * 在指定位置添加子视图
     */
    fun addView(child: NanoView, index: Int) {
        if (child.parent != null) {
            throw IllegalStateException("View already has a parent")
        }

        if (index < 0 || index > children.size) {
            throw IndexOutOfBoundsException("Index: $index, Size: ${children.size}")
        }

        children.add(index, child)
        child.parent = this
        requestLayout()
    }

    /**
     * 移除子视图
     */
    fun removeView(child: NanoView) {
        if (children.remove(child)) {
            child.parent = null
            requestLayout()
        }
    }

    /**
     * 移除指定位置的子视图
     */
    fun removeViewAt(index: Int) {
        if (index < 0 || index >= children.size) {
            throw IndexOutOfBoundsException("Index: $index, Size: ${children.size}")
        }

        val child = children.removeAt(index)
        child.parent = null
        requestLayout()
    }

    /**
     * 移除所有子视图
     */
    fun removeAllViews() {
        for (child in children) {
            child.parent = null
        }
        children.clear()
        requestLayout()
    }

    /**
     * 获取子视图数量
     */
    fun getChildCount(): Int = children.size

    /**
     * 获取指定位置的子视图
     */
    fun getChildAt(index: Int): NanoView {
        if (index < 0 || index >= children.size) {
            throw IndexOutOfBoundsException("Index: $index, Size: ${children.size}")
        }
        return children[index]
    }

    /**
     * 获取子视图的索引
     */
    fun indexOfChild(child: NanoView): Int {
        return children.indexOf(child)
    }

    // ==================== 测量 ====================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 默认实现：测量所有子视图
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        // 计算容器尺寸（默认使用测量规格的尺寸）
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    /**
     * 测量所有子视图
     */
    protected fun measureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        for (child in children) {
            if (child.visibility != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
            }
        }
    }

    /**
     * 测量单个子视图
     */
    protected fun measureChild(
        child: NanoView,
        parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
        // 为子视图创建测量规格
        val childWidthMeasureSpec = getChildMeasureSpec(
            parentWidthMeasureSpec,
            0,
            -1 // MATCH_PARENT
        )
        val childHeightMeasureSpec = getChildMeasureSpec(
            parentHeightMeasureSpec,
            0,
            -1 // MATCH_PARENT
        )

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    /**
     * 为子视图生成测量规格
     */
    protected fun getChildMeasureSpec(
        spec: Int,
        padding: Int,
        childDimension: Int
    ): Int {
        val specMode = MeasureSpec.getMode(spec)
        val specSize = MeasureSpec.getSize(spec)
        val size = (specSize - padding).coerceAtLeast(0)

        return when {
            childDimension >= 0 -> {
                // 具体尺寸
                MeasureSpec.makeMeasureSpec(childDimension, MeasureSpec.EXACTLY)
            }
            childDimension == -1 -> {
                // MATCH_PARENT
                MeasureSpec.makeMeasureSpec(size, specMode)
            }
            childDimension == -2 -> {
                // WRAP_CONTENT
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST)
            }
            else -> {
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.UNSPECIFIED)
            }
        }
    }

    // ==================== 布局 ====================

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // 子类实现具体的布局逻辑
    }

    /**
     * 布局子视图
     */
    protected fun layoutChild(child: NanoView, left: Int, top: Int, right: Int, bottom: Int) {
        child.layout(left, top, right, bottom)
    }

    // ==================== 绘制 ====================

    override fun onDraw() {
        // 绘制所有子视图
        drawChildren()
    }

    /**
     * 绘制所有子视图
     */
    protected fun drawChildren() {
        for (child in children) {
            if (child.visibility == VISIBLE) {
                child.draw()
            }
        }
    }

    // ==================== 事件分发 ====================

    override fun onTouchEvent(x: Int, y: Int): Boolean {
        // 反向遍历子视图（从上层到下层）
        for (i in children.size - 1 downTo 0) {
            val child = children[i]
            if (child.visibility == VISIBLE) {
                // 将坐标转换为子视图坐标系
                val handled = child.onTouchEvent(x - child.left, y - child.top)
                if (handled) {
                    return true
                }
            }
        }

        // 子视图未处理，自己处理
        return super.onTouchEvent(x, y)
    }

    // ==================== 查找视图 ====================

    override fun findViewById(id: String): NanoView? {
        // 先检查自己
        if (this.id == id) {
            return this
        }

        // 递归查找子视图
        for (child in children) {
            val found = child.findViewById(id)
            if (found != null) {
                return found
            }
        }

        return null
    }
}
