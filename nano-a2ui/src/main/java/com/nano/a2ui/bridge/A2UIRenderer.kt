package com.nano.a2ui.bridge

import com.nano.llm.a2ui.*
import com.nano.view.NanoView
import com.nano.view.widget.NanoButton
import com.nano.view.widget.NanoLinearLayout
import com.nano.view.widget.NanoTextView

/**
 * A2UI 渲染器 - 将 A2UISpec 转换为 NanoView 视图树
 *
 * 映射规则:
 * - A2UIContainer → NanoLinearLayout (VERTICAL/HORIZONTAL)
 * - A2UIText      → NanoTextView
 * - A2UIButton    → NanoButton
 * - A2UIList      → NanoLinearLayout (垂直) + 多个 NanoTextView
 * - A2UICard      → NanoLinearLayout + header/content/footer
 * - A2UIInput     → NanoTextView (简化表示，后续扩展为输入框)
 * - A2UITabBar    → NanoLinearLayout (水平) + Tab 标签
 * - A2UITabContent → NanoLinearLayout + 默认激活 Tab 的内容
 */
class A2UIRenderer {

    /**
     * 渲染 A2UISpec 为 NanoView 树
     */
    fun render(spec: A2UISpec): NanoView {
        return renderComponent(spec.root)
    }

    /**
     * 渲染单个组件为 NanoView
     */
    fun renderComponent(component: A2UIComponent): NanoView {
        val view = when (component) {
            is A2UIContainer -> renderContainer(component)
            is A2UIText -> renderText(component)
            is A2UIButton -> renderButton(component)
            is A2UIList -> renderList(component)
            is A2UICard -> renderCard(component)
            is A2UIInput -> renderInput(component)
            is A2UITabBar -> renderTabBar(component)
            is A2UITabContent -> renderTabContent(component)
        }

        // 应用通用 ID（组件级别 ID 优先）
        component.id?.let { view.id = it }

        // 应用通用样式
        component.style?.let { applyStyle(view, it) }

        return view
    }

    /**
     * 应用 A2UIStyle 到 NanoView
     */
    private fun applyStyle(view: NanoView, style: A2UIStyle) {
        // 应用尺寸
        val layoutParams = view.layoutParams ?: NanoView.LayoutParams(
            width = NanoView.LayoutParams.WRAP_CONTENT,
            height = NanoView.LayoutParams.WRAP_CONTENT
        )

        style.width?.let { layoutParams.width = it }
        style.height?.let { layoutParams.height = it }

        // 应用外边距
        style.margin?.let { margin ->
            layoutParams.setMargins(
                left = margin.left,
                top = margin.top,
                right = margin.right,
                bottom = margin.bottom
            )
        }

        view.layoutParams = layoutParams

        // 应用内边距
        style.padding?.let { padding ->
            view.setPadding(
                left = padding.left,
                top = padding.top,
                right = padding.right,
                bottom = padding.bottom
            )
        }

        // 应用背景颜色
        style.backgroundColor?.let { colorStr ->
            view.backgroundColor = parseColor(colorStr)
        }

        // 应用圆角
        style.borderRadius?.let { radius ->
            view.borderRadius = radius
        }
    }

    // ==================== 具体组件渲染 ====================

    private fun renderContainer(container: A2UIContainer): NanoView {
        val layout = NanoLinearLayout()
        layout.orientation = when (container.direction) {
            Direction.VERTICAL -> NanoLinearLayout.VERTICAL
            Direction.HORIZONTAL -> NanoLinearLayout.HORIZONTAL
        }

        container.children.forEach { child ->
            layout.addView(renderComponent(child))
        }

        return layout
    }

    private fun renderText(text: A2UIText): NanoView {
        val textView = NanoTextView()
        textView.text = text.text

        text.textStyle?.let { style ->
            style.fontSize?.let { textView.textSize = it.toFloat() }
            style.color?.let { textView.textColor = parseColor(it) }
        }

        return textView
    }

    private fun renderButton(button: A2UIButton): NanoView {
        val nanoButton = NanoButton()
        nanoButton.text = button.text
        // 按钮 ID 用于事件路由，如果组件没 ID 则自动生成
        nanoButton.id = button.id ?: "btn_${button.action.target}_${button.action.method}"
        // 将 A2UIAction 存储到 tag 中，供事件处理使用
        nanoButton.tag = button.action
        return nanoButton
    }

    private fun renderList(list: A2UIList): NanoView {
        val layout = NanoLinearLayout()
        layout.orientation = NanoLinearLayout.VERTICAL

        list.items.forEach { item ->
            val itemView = NanoTextView()
            itemView.id = "list_item_${item.id}"
            itemView.text = buildString {
                append(item.title)
                item.subtitle?.let { append(" - $it") }
                item.trailing?.let { append(" [$it]") }
            }

            // 如果列表有 onItemClick 动作，则将其与 item.data 一起存储到 tag
            list.onItemClick?.let { action ->
                itemView.isClickable = true
                // 存储 action 和 item 数据，供点击时使用
                itemView.tag = ListItemData(action, item.id, item.data)
            }

            layout.addView(itemView)
        }

        return layout
    }

    /**
     * 列表项数据 - 存储在 NanoTextView.tag 中
     */
    data class ListItemData(
        val action: A2UIAction,
        val itemId: String,
        val itemData: Map<String, String>
    )

    private fun renderCard(card: A2UICard): NanoView {
        val layout = NanoLinearLayout()
        layout.orientation = NanoLinearLayout.VERTICAL

        card.header?.let { layout.addView(renderComponent(it)) }
        layout.addView(renderComponent(card.content))
        card.footer?.let { layout.addView(renderComponent(it)) }

        return layout
    }

    private fun renderInput(input: A2UIInput): NanoView {
        val textView = NanoTextView()
        textView.id = input.id ?: "input_${System.currentTimeMillis()}"
        textView.text = input.value ?: input.placeholder ?: ""
        return textView
    }

    private fun renderTabBar(tabBar: A2UITabBar): NanoView {
        val layout = NanoLinearLayout()
        layout.orientation = NanoLinearLayout.HORIZONTAL

        tabBar.tabs.forEach { tab ->
            val tabView = NanoTextView()
            tabView.id = "tab_${tab.id}"
            tabView.text = tab.label
            layout.addView(tabView)
        }

        return layout
    }

    private fun renderTabContent(tabContent: A2UITabContent): NanoView {
        val layout = NanoLinearLayout()
        layout.orientation = NanoLinearLayout.VERTICAL

        // 默认渲染第一个 Tab 的内容
        tabContent.tabs.firstOrNull()?.content?.let {
            layout.addView(renderComponent(it))
        }

        return layout
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析颜色字符串为整数 (支持 #RRGGBB 和 #AARRGGBB)
     */
    private fun parseColor(colorStr: String): Int {
        val hex = colorStr.removePrefix("#")
        return when (hex.length) {
            6 -> (0xFF000000.toInt() or hex.toLong(16).toInt())
            8 -> hex.toLong(16).toInt()
            else -> 0xFF000000.toInt()
        }
    }
}
