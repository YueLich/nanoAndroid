package com.nano.view

import com.nano.kernel.NanoLog

/**
 * NanoView - 基础视图类
 *
 * 模拟 Android View 的核心功能
 */
abstract class NanoView {

    companion object {
        private const val TAG = "NanoView"

        // 可见性常量
        const val VISIBLE = 0
        const val INVISIBLE = 4
        const val GONE = 8
    }

    /** 视图 ID */
    var id: String? = null

    /** 父视图 */
    internal var parent: NanoViewGroup? = null

    /** 可见性 */
    var visibility: Int = VISIBLE
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** 测量后的宽度 */
    var measuredWidth: Int = 0
        private set

    /** 测量后的高度 */
    var measuredHeight: Int = 0
        private set

    /** 布局位置 - left */
    var left: Int = 0
        private set

    /** 布局位置 - top */
    var top: Int = 0
        private set

    /** 布局位置 - right */
    var right: Int = 0
        private set

    /** 布局位置 - bottom */
    var bottom: Int = 0
        private set

    /** 是否可点击 */
    var isClickable: Boolean = false

    /** 点击监听器 */
    private var onClickListener: (() -> Unit)? = null

    /** 是否需要布局 */
    private var needsLayout: Boolean = false

    // ==================== 样式属性 ====================

    /** 内边距 - left */
    var paddingLeft: Int = 0
        private set

    /** 内边距 - top */
    var paddingTop: Int = 0
        private set

    /** 内边距 - right */
    var paddingRight: Int = 0
        private set

    /** 内边距 - bottom */
    var paddingBottom: Int = 0
        private set

    /** 背景颜色 (ARGB) */
    var backgroundColor: Int = 0x00FFFFFF
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /** 圆角半径 */
    var borderRadius: Int = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    /**
     * 附加数据 - 用于存储额外信息
     *
     * 通过 tag 机制存储额外数据，避免模块间的依赖
     */
    var tag: Any? = null

    // ==================== 测量 ====================

    /**
     * 测量视图尺寸
     *
     * @param widthMeasureSpec 宽度测量规格
     * @param heightMeasureSpec 高度测量规格
     */
    fun measure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * 子类实现测量逻辑
     */
    protected open fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 默认实现：从测量规格中提取尺寸
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    /**
     * 设置测量后的尺寸
     */
    protected fun setMeasuredDimension(width: Int, height: Int) {
        measuredWidth = width
        measuredHeight = height
    }

    // ==================== 布局 ====================

    /**
     * 布局视图位置
     *
     * @param left 左边界
     * @param top 上边界
     * @param right 右边界
     * @param bottom 下边界
     */
    fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        val changed = this.left != left || this.top != top ||
                this.right != right || this.bottom != bottom

        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom

        needsLayout = false

        if (changed) {
            onLayout(changed, left, top, right, bottom)
        }
    }

    /**
     * 子类实现布局逻辑
     */
    protected open fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // 子类可以重写
    }

    /**
     * 请求重新布局
     */
    fun requestLayout() {
        needsLayout = true
        parent?.requestLayout()
    }

    /**
     * 检查是否需要布局
     */
    fun needsLayout(): Boolean = needsLayout

    // ==================== 绘制 ====================

    /**
     * 绘制视图
     */
    fun draw() {
        if (visibility != VISIBLE) {
            return
        }
        onDraw()
    }

    /**
     * 子类实现绘制逻辑
     */
    protected open fun onDraw() {
        // 子类可以重写
    }

    // ==================== 样式方法 ====================

    /**
     * 设置内边距
     *
     * @param left 左边距
     * @param top 上边距
     * @param right 右边距
     * @param bottom 下边距
     */
    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if (paddingLeft != left || paddingTop != top ||
            paddingRight != right || paddingBottom != bottom) {
            paddingLeft = left
            paddingTop = top
            paddingRight = right
            paddingBottom = bottom
            requestLayout()
        }
    }

    /**
     * 设置统一内边距
     */
    fun setPadding(padding: Int) {
        setPadding(padding, padding, padding, padding)
    }

    /**
     * 获取总内边距宽度（左 + 右）
     */
    fun getPaddingWidth(): Int = paddingLeft + paddingRight

    /**
     * 获取总内边距高度（上 + 下）
     */
    fun getPaddingHeight(): Int = paddingTop + paddingBottom

    // ==================== 事件处理 ====================

    /**
     * 设置点击监听器
     */
    fun setOnClickListener(listener: (() -> Unit)?) {
        isClickable = true
        onClickListener = listener
    }

    /**
     * 处理触摸事件
     *
     * @param x 触摸点 X 坐标（相对于视图自身，0 表示左边界）
     * @param y 触摸点 Y 坐标（相对于视图自身，0 表示上边界）
     * @return 是否消费了事件
     */
    open fun onTouchEvent(x: Int, y: Int): Boolean {
        if (!isClickable) {
            return false
        }

        // 检查触摸点是否在视图范围内（相对坐标）
        val width = getWidth()
        val height = getHeight()
        if (x >= 0 && x <= width && y >= 0 && y <= height) {
            performClick()
            return true
        }

        return false
    }

    /**
     * 执行点击
     */
    open fun performClick() {
        onClickListener?.invoke()
    }

    // ==================== 尺寸和位置 ====================

    /**
     * 获取视图宽度
     */
    fun getWidth(): Int = right - left

    /**
     * 获取视图高度
     */
    fun getHeight(): Int = bottom - top

    /**
     * 检查视图是否可见
     */
    fun isVisible(): Boolean = visibility == VISIBLE

    /**
     * 检查视图是否消失（GONE）
     */
    fun isGone(): Boolean = visibility == GONE

    // ==================== 父子关系 ====================

    /**
     * 获取父视图
     */
    fun getParent(): NanoViewGroup? = parent

    /**
     * 移除自己
     */
    fun removeFromParent() {
        parent?.removeView(this)
    }

    // ==================== 工具方法 ====================

    /**
     * 查找视图
     */
    open fun findViewById(id: String): NanoView? {
        return if (this.id == id) this else null
    }

    override fun toString(): String {
        return "${javaClass.simpleName}@${hashCode().toString(16)}(id=$id)"
    }

    // ==================== LayoutParams ====================

    /**
     * 布局参数
     *
     * 类似 Android 的 ViewGroup.LayoutParams，用于描述视图在父容器中的布局行为
     */
    open class LayoutParams(
        var width: Int = WRAP_CONTENT,
        var height: Int = WRAP_CONTENT
    ) {
        companion object {
            const val MATCH_PARENT = -1
            const val WRAP_CONTENT = -2
        }

        /** 外边距 */
        var leftMargin: Int = 0
        var topMargin: Int = 0
        var rightMargin: Int = 0
        var bottomMargin: Int = 0

        /**
         * 设置外边距
         */
        fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
            leftMargin = left
            topMargin = top
            rightMargin = right
            bottomMargin = bottom
        }

        /**
         * 获取总外边距宽度
         */
        fun getMarginWidth(): Int = leftMargin + rightMargin

        /**
         * 获取总外边距高度
         */
        fun getMarginHeight(): Int = topMargin + bottomMargin
    }

    /** 视图的布局参数 */
    var layoutParams: LayoutParams? = null

    /**
     * MeasureSpec - 测量规格
     *
     * 类似 Android 的 MeasureSpec，用于描述视图的测量要求
     */
    object MeasureSpec {
        private const val MODE_SHIFT = 30
        private const val MODE_MASK = 0x3 shl MODE_SHIFT

        const val UNSPECIFIED = 0 shl MODE_SHIFT
        const val EXACTLY = 1 shl MODE_SHIFT
        const val AT_MOST = 2 shl MODE_SHIFT

        /**
         * 创建测量规格
         */
        fun makeMeasureSpec(size: Int, mode: Int): Int {
            return (size and MODE_MASK.inv()) or (mode and MODE_MASK)
        }

        /**
         * 获取测量模式
         */
        fun getMode(measureSpec: Int): Int {
            return measureSpec and MODE_MASK
        }

        /**
         * 获取测量尺寸
         */
        fun getSize(measureSpec: Int): Int {
            return measureSpec and MODE_MASK.inv()
        }
    }
}
