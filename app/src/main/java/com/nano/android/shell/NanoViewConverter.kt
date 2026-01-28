package com.nano.android.shell

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.nano.view.NanoView
import com.nano.view.NanoViewGroup
import com.nano.view.widget.NanoButton
import com.nano.view.widget.NanoLinearLayout
import com.nano.view.widget.NanoTextView

/**
 * NanoViewConverter - NanoView 到 Android View 的转换器
 *
 * 将 nano-view 框架的 NanoView 转换为标准 Android View
 * 支持递归处理 NanoViewGroup 的子视图
 */
class NanoViewConverter(private val context: Context) {

    companion object {
        private const val TAG = "NanoViewConverter"
    }

    /**
     * 转换 NanoView 到 Android View
     *
     * @param nanoView 要转换的 NanoView
     * @return 转换后的 Android View
     */
    fun convert(nanoView: NanoView): View {
        return when (nanoView) {
            is NanoButton -> convertButton(nanoView)
            is NanoTextView -> convertTextView(nanoView)
            is NanoLinearLayout -> convertLinearLayout(nanoView)
            is NanoViewGroup -> convertViewGroup(nanoView)
            else -> convertGenericView(nanoView)
        }
    }

    /**
     * 转换 NanoTextView
     */
    private fun convertTextView(nano: NanoTextView): TextView {
        return TextView(context).apply {
            // 设置文本
            text = nano.text

            // 设置文本大小（sp）
            setTextSize(TypedValue.COMPLEX_UNIT_SP, nano.textSize)

            // 设置文本颜色
            setTextColor(nano.textColor)

            // 设置可见性
            visibility = convertVisibility(nano.visibility)

            // 设置 ID（如果有）
            nano.id?.let { id = it.hashCode() }

            // 设置布局参数
            layoutParams = createLayoutParams(nano)

            // 添加内边距
            val padding = dpToPx(8)
            setPadding(padding, padding, padding, padding)
        }
    }

    /**
     * 转换 NanoButton
     */
    private fun convertButton(nano: NanoButton): Button {
        return Button(context).apply {
            // 设置文本
            text = nano.text

            // 设置文本大小（sp）
            setTextSize(TypedValue.COMPLEX_UNIT_SP, nano.textSize)

            // 设置文本颜色
            setTextColor(nano.textColor)

            // 设置启用状态
            isEnabled = nano.isEnabled

            // 设置可见性
            visibility = convertVisibility(nano.visibility)

            // 设置 ID（如果有）
            nano.id?.let { id = it.hashCode() }

            // 设置布局参数
            layoutParams = createLayoutParams(nano)

            // 设置点击监听器
            // 注意：NanoButton 的点击监听器通过 setOnClickListener 设置
            // 我们需要在点击时触发 NanoView 的点击事件
            setOnClickListener {
                // 触发 NanoView 的点击事件
                nano.performClick()
            }

            // 设置背景颜色（如果有）
            try {
                setBackgroundColor(nano.backgroundColor)
            } catch (e: Exception) {
                // 忽略背景色设置错误
            }
        }
    }

    /**
     * 转换 NanoLinearLayout
     */
    private fun convertLinearLayout(nano: NanoLinearLayout): LinearLayout {
        return LinearLayout(context).apply {
            // 设置方向
            orientation = when (nano.orientation) {
                NanoLinearLayout.VERTICAL -> LinearLayout.VERTICAL
                NanoLinearLayout.HORIZONTAL -> LinearLayout.HORIZONTAL
                else -> LinearLayout.VERTICAL
            }

            // 设置可见性
            visibility = convertVisibility(nano.visibility)

            // 设置 ID（如果有）
            nano.id?.let { id = it.hashCode() }

            // 设置布局参数
            layoutParams = createLayoutParams(nano)

            // 添加内边距
            val padding = dpToPx(8)
            setPadding(padding, padding, padding, padding)

            // 递归转换并添加子视图
            for (i in 0 until nano.getChildCount()) {
                val child = nano.getChildAt(i)
                val androidChild = convert(child)
                addView(androidChild)
            }
        }
    }

    /**
     * 转换通用 NanoViewGroup
     */
    private fun convertViewGroup(nano: NanoViewGroup): ViewGroup {
        // 对于未特定处理的 ViewGroup，使用 LinearLayout 作为默认容器
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            // 设置可见性
            visibility = convertVisibility(nano.visibility)

            // 设置 ID（如果有）
            nano.id?.let { id = it.hashCode() }

            // 设置布局参数
            layoutParams = createLayoutParams(nano)

            // 添加内边距
            val padding = dpToPx(8)
            setPadding(padding, padding, padding, padding)

            // 递归转换并添加子视图
            for (i in 0 until nano.getChildCount()) {
                val child = nano.getChildAt(i)
                val androidChild = convert(child)
                addView(androidChild)
            }
        }
    }

    /**
     * 转换通用 NanoView
     */
    private fun convertGenericView(nano: NanoView): View {
        // 对于未特定处理的 View，创建一个基本的 View
        return View(context).apply {
            // 设置可见性
            visibility = convertVisibility(nano.visibility)

            // 设置 ID（如果有）
            nano.id?.let { id = it.hashCode() }

            // 设置布局参数
            layoutParams = createLayoutParams(nano)

            // 设置最小尺寸
            minimumWidth = dpToPx(48)
            minimumHeight = dpToPx(48)

            // 设置点击监听器（如果可点击）
            if (nano.isClickable) {
                setOnClickListener {
                    nano.performClick()
                }
            }
        }
    }

    /**
     * 转换可见性
     */
    private fun convertVisibility(nanoVisibility: Int): Int {
        return when (nanoVisibility) {
            NanoView.VISIBLE -> View.VISIBLE
            NanoView.INVISIBLE -> View.INVISIBLE
            NanoView.GONE -> View.GONE
            else -> View.VISIBLE
        }
    }

    /**
     * 创建布局参数
     */
    private fun createLayoutParams(nano: NanoView): ViewGroup.LayoutParams {
        // 使用 WRAP_CONTENT 作为默认值
        val width = if (nano.measuredWidth > 0) {
            nano.measuredWidth
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }

        val height = if (nano.measuredHeight > 0) {
            nano.measuredHeight
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }

        return ViewGroup.LayoutParams(width, height)
    }

    /**
     * dp 转 px
     */
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
