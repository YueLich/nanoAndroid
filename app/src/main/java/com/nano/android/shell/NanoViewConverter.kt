package com.nano.android.shell

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.nano.a2ui.bridge.A2UIRenderer
import com.nano.llm.a2ui.A2UIAction
import com.nano.view.NanoView
import com.nano.view.NanoViewGroup
import com.nano.view.widget.NanoButton
import com.nano.view.widget.NanoLinearLayout
import com.nano.view.widget.NanoTextView
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * NanoViewConverter - NanoView 到 Android View 的转换器
 *
 * 将 nano-view 框架的 NanoView 转换为标准 Android View
 * 支持递归处理 NanoViewGroup 的子视图
 *
 * @param context Android Context
 * @param onA2UIActionListener A2UI 动作监听器（可选），用于处理 A2UI 按钮点击事件，接收JSON格式的A2UIAction
 */
class NanoViewConverter(
    private val context: Context,
    private val onA2UIActionListener: ((String) -> Unit)? = null
) {

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

            // 应用样式
            applyNanoStyle(this, nano)

            // 如果是可点击的列表项，设置点击监听器
            if (nano.isClickable) {
                setOnClickListener {
                    // 触发 NanoView 的点击事件
                    nano.performClick()

                    // 检查是否是列表项数据
                    val listItemData = nano.tag as? A2UIRenderer.ListItemData
                    if (listItemData != null && onA2UIActionListener != null) {
                        // 构建包含 itemId 和 itemData 的完整 action
                        val actionWithData = listItemData.action.copy(
                            params = (listItemData.action.params ?: emptyMap()) +
                                    mapOf("itemId" to listItemData.itemId) +
                                    listItemData.itemData
                        )
                        try {
                            val actionJson = Json.encodeToString(actionWithData)
                            onA2UIActionListener.invoke(actionJson)
                        } catch (e: Exception) {
                            onA2UIActionListener.invoke(nano.text)
                        }
                    }
                }
            }
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

            // 应用样式（padding、background、borderRadius）
            applyNanoStyle(this, nano)

            // 设置点击监听器
            setOnClickListener {
                // 触发 NanoView 的点击事件
                nano.performClick()

                // 如果有 A2UI 动作监听器，则触发回调
                // 检查 tag 中是否有 A2UIAction
                val action = nano.tag as? A2UIAction
                if (action != null && onA2UIActionListener != null) {
                    // 将 A2UIAction 序列化为 JSON 传递
                    try {
                        val actionJson = Json.encodeToString(action)
                        onA2UIActionListener.invoke(actionJson)
                    } catch (e: Exception) {
                        // 降级：使用按钮文本
                        onA2UIActionListener.invoke(nano.text)
                    }
                } else {
                    // 没有 action，使用按钮文本作为降级
                    onA2UIActionListener?.invoke(nano.text)
                }
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

            // 应用样式
            applyNanoStyle(this, nano)

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

            // 应用样式
            applyNanoStyle(this, nano)

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

                    // 如果有 A2UI 动作监听器，则触发回调
                    onA2UIActionListener?.invoke("click")
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

    /**
     * 应用 NanoView 样式到 Android View
     *
     * 包括：padding, backgroundColor, borderRadius
     */
    private fun applyNanoStyle(androidView: View, nanoView: NanoView) {
        // 应用内边距
        val hasPadding = nanoView.paddingLeft > 0 || nanoView.paddingTop > 0 ||
                nanoView.paddingRight > 0 || nanoView.paddingBottom > 0
        if (hasPadding) {
            androidView.setPadding(
                nanoView.paddingLeft,
                nanoView.paddingTop,
                nanoView.paddingRight,
                nanoView.paddingBottom
            )
        } else {
            // 默认内边距
            val defaultPadding = dpToPx(8)
            androidView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)
        }

        // 应用背景颜色和圆角
        if (nanoView.backgroundColor != 0x00FFFFFF || nanoView.borderRadius > 0) {
            val drawable = GradientDrawable().apply {
                // 设置背景颜色
                setColor(nanoView.backgroundColor)

                // 设置圆角
                if (nanoView.borderRadius > 0) {
                    cornerRadius = nanoView.borderRadius.toFloat()
                }
            }
            androidView.background = drawable
        }
    }
}
