package com.nano.llm.a2ui

import kotlinx.serialization.Serializable

/**
 * A2UI 协议 - AI 到 UI 渲染规格
 */
@Serializable
data class A2UISpec(
    val version: String = "1.0",
    val root: A2UIComponent
)

/**
 * A2UI 组件基类
 */
@Serializable
sealed class A2UIComponent {
    abstract val id: String?
    abstract val style: A2UIStyle?
}

/** 容器组件 */
@Serializable
data class A2UIContainer(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val direction: Direction = Direction.VERTICAL,
    val children: List<A2UIComponent> = emptyList()
) : A2UIComponent()

/** 文本组件 */
@Serializable
data class A2UIText(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val text: String,
    val textStyle: TextStyle? = null
) : A2UIComponent()

/** 按钮组件 */
@Serializable
data class A2UIButton(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val text: String,
    val action: A2UIAction
) : A2UIComponent()

/** 列表组件 */
@Serializable
data class A2UIList(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val items: List<A2UIListItem> = emptyList(),
    val onItemClick: A2UIAction? = null
) : A2UIComponent()

/** 卡片组件 */
@Serializable
data class A2UICard(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val header: A2UIComponent? = null,
    val content: A2UIComponent,
    val footer: A2UIComponent? = null,
    val action: A2UIAction? = null
) : A2UIComponent()

/** 输入组件 */
@Serializable
data class A2UIInput(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val placeholder: String? = null,
    val value: String? = null,
    val inputType: InputType = InputType.TEXT,
    val onSubmit: A2UIAction? = null
) : A2UIComponent()

/** Tab 栏组件 */
@Serializable
data class A2UITabBar(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val tabs: List<A2UITab> = emptyList()
) : A2UIComponent()

/** Tab 内容组件 */
@Serializable
data class A2UITabContent(
    override val id: String? = null,
    override val style: A2UIStyle? = null,
    val tabs: List<A2UITab> = emptyList()
) : A2UIComponent()

// ---- 支持类型 ----

@Serializable
data class A2UIListItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val image: String? = null,
    val trailing: String? = null,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class A2UITab(
    val id: String,
    val label: String,
    val content: A2UIComponent? = null
)

@Serializable
data class A2UIAction(
    val type: ActionType,
    val target: String,
    val method: String,
    val params: Map<String, String>? = null
)

@Serializable
data class A2UIStyle(
    val width: Int? = null,
    val height: Int? = null,
    val padding: Spacing? = null,
    val margin: Spacing? = null,
    val backgroundColor: String? = null,
    val borderRadius: Int? = null
)

@Serializable
data class Spacing(
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0
)

@Serializable
data class TextStyle(
    val fontSize: Int? = null,
    val fontWeight: FontWeight? = null,
    val color: String? = null,
    val align: TextAlign? = null
)

@Serializable
enum class Direction { VERTICAL, HORIZONTAL }

@Serializable
enum class ActionType { AGENT_CALL, NAVIGATE, SUBMIT, DISMISS, CUSTOM }

@Serializable
enum class InputType { TEXT, NUMBER, PASSWORD, MULTILINE }

@Serializable
enum class FontWeight { NORMAL, BOLD }

@Serializable
enum class TextAlign { LEFT, CENTER, RIGHT }
