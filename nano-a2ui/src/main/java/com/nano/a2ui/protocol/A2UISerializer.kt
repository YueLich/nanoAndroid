package com.nano.a2ui.protocol

import com.nano.llm.a2ui.*
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

/**
 * A2UI 序列化器 - 将 A2UISpec 序列化为 JSON 并反序列化
 *
 * 由于 A2UIComponent 是 sealed class，采用类型判别字段 "type" 的策略：
 * 序列化时写入 type 字段，反序列化时根据 type 分发到对应子类构造。
 */
object A2UISerializer {

    private val json = Json { prettyPrint = true }

    /** 序列化 A2UISpec 为 JSON 字符串 */
    fun serialize(spec: A2UISpec): String {
        val obj = buildJsonObject {
            put("version", spec.version)
            put("root", serializeComponent(spec.root))
        }
        return json.encodeToString(JsonElement.serializer(), obj)
    }

    /** 从 JSON 字符串反序列化为 A2UISpec */
    fun deserialize(jsonStr: String): A2UISpec {
        val obj = json.parseToJsonElement(jsonStr) as JsonObject
        return A2UISpec(
            version = obj["version"]?.jsonPrimitive?.content ?: "1.0",
            root = deserializeComponent(obj["root"] as JsonObject)
        )
    }

    // ==================== 组件序列化 ====================

    internal fun serializeComponent(component: A2UIComponent): JsonObject {
        return buildJsonObject {
            put("type", getComponentType(component))
            component.id?.let { put("id", it) }
            component.style?.let { put("style", serializeStyle(it)) }

            when (component) {
                is A2UIContainer -> {
                    put("direction", component.direction.name)
                    put("children", buildJsonArray {
                        component.children.forEach { add(serializeComponent(it)) }
                    })
                }
                is A2UIText -> {
                    put("text", component.text)
                    component.textStyle?.let { put("textStyle", serializeTextStyle(it)) }
                }
                is A2UIButton -> {
                    put("text", component.text)
                    put("action", serializeAction(component.action))
                }
                is A2UIList -> {
                    put("items", buildJsonArray {
                        component.items.forEach { add(serializeListItem(it)) }
                    })
                    component.onItemClick?.let { put("onItemClick", serializeAction(it)) }
                }
                is A2UICard -> {
                    component.header?.let { put("header", serializeComponent(it)) }
                    put("content", serializeComponent(component.content))
                    component.footer?.let { put("footer", serializeComponent(it)) }
                    component.action?.let { put("action", serializeAction(it)) }
                }
                is A2UIInput -> {
                    component.placeholder?.let { put("placeholder", it) }
                    component.value?.let { put("value", it) }
                    put("inputType", component.inputType.name)
                    component.onSubmit?.let { put("onSubmit", serializeAction(it)) }
                }
                is A2UITabBar -> {
                    put("tabs", buildJsonArray {
                        component.tabs.forEach { add(serializeTab(it)) }
                    })
                }
                is A2UITabContent -> {
                    put("tabs", buildJsonArray {
                        component.tabs.forEach { add(serializeTab(it)) }
                    })
                }
            }
        }
    }

    // ==================== 组件反序列化 ====================

    internal fun deserializeComponent(obj: JsonObject): A2UIComponent {
        val type = obj["type"]?.jsonPrimitive?.content ?: ""
        val id = obj["id"]?.jsonPrimitive?.content
        val style = (obj["style"] as? JsonObject)?.let { deserializeStyle(it) }

        return when (type) {
            "container" -> A2UIContainer(
                id = id, style = style,
                direction = Direction.valueOf(obj["direction"]?.jsonPrimitive?.content ?: "VERTICAL"),
                children = (obj["children"] as? JsonArray)?.map {
                    deserializeComponent(it as JsonObject)
                } ?: emptyList()
            )
            "text" -> A2UIText(
                id = id, style = style,
                text = obj["text"]?.jsonPrimitive?.content ?: "",
                textStyle = (obj["textStyle"] as? JsonObject)?.let { deserializeTextStyle(it) }
            )
            "button" -> A2UIButton(
                id = id, style = style,
                text = obj["text"]?.jsonPrimitive?.content ?: "",
                action = deserializeAction(obj["action"] as JsonObject)
            )
            "list" -> A2UIList(
                id = id, style = style,
                items = (obj["items"] as? JsonArray)?.map {
                    deserializeListItem(it as JsonObject)
                } ?: emptyList(),
                onItemClick = (obj["onItemClick"] as? JsonObject)?.let { deserializeAction(it) }
            )
            "card" -> A2UICard(
                id = id, style = style,
                header = (obj["header"] as? JsonObject)?.let { deserializeComponent(it) },
                content = deserializeComponent(obj["content"] as JsonObject),
                footer = (obj["footer"] as? JsonObject)?.let { deserializeComponent(it) },
                action = (obj["action"] as? JsonObject)?.let { deserializeAction(it) }
            )
            "input" -> A2UIInput(
                id = id, style = style,
                placeholder = obj["placeholder"]?.jsonPrimitive?.content,
                value = obj["value"]?.jsonPrimitive?.content,
                inputType = InputType.valueOf(obj["inputType"]?.jsonPrimitive?.content ?: "TEXT"),
                onSubmit = (obj["onSubmit"] as? JsonObject)?.let { deserializeAction(it) }
            )
            "tabbar" -> A2UITabBar(
                id = id, style = style,
                tabs = (obj["tabs"] as? JsonArray)?.map {
                    deserializeTab(it as JsonObject)
                } ?: emptyList()
            )
            "tabcontent" -> A2UITabContent(
                id = id, style = style,
                tabs = (obj["tabs"] as? JsonArray)?.map {
                    deserializeTab(it as JsonObject)
                } ?: emptyList()
            )
            else -> A2UIText(id = id, style = style, text = "Unknown component: $type")
        }
    }

    // ==================== 辅助类型序列化 ====================

    private fun getComponentType(component: A2UIComponent): String = when (component) {
        is A2UIContainer -> "container"
        is A2UIText -> "text"
        is A2UIButton -> "button"
        is A2UIList -> "list"
        is A2UICard -> "card"
        is A2UIInput -> "input"
        is A2UITabBar -> "tabbar"
        is A2UITabContent -> "tabcontent"
    }

    private fun serializeStyle(style: A2UIStyle): JsonObject = buildJsonObject {
        style.width?.let { put("width", it) }
        style.height?.let { put("height", it) }
        style.padding?.let { put("padding", serializeSpacing(it)) }
        style.margin?.let { put("margin", serializeSpacing(it)) }
        style.backgroundColor?.let { put("backgroundColor", it) }
        style.borderRadius?.let { put("borderRadius", it) }
    }

    private fun deserializeStyle(obj: JsonObject): A2UIStyle = A2UIStyle(
        width = obj["width"]?.jsonPrimitive?.intOrNull,
        height = obj["height"]?.jsonPrimitive?.intOrNull,
        padding = (obj["padding"] as? JsonObject)?.let { deserializeSpacing(it) },
        margin = (obj["margin"] as? JsonObject)?.let { deserializeSpacing(it) },
        backgroundColor = obj["backgroundColor"]?.jsonPrimitive?.content,
        borderRadius = obj["borderRadius"]?.jsonPrimitive?.intOrNull
    )

    private fun serializeSpacing(spacing: Spacing): JsonObject = buildJsonObject {
        put("top", spacing.top)
        put("right", spacing.right)
        put("bottom", spacing.bottom)
        put("left", spacing.left)
    }

    private fun deserializeSpacing(obj: JsonObject): Spacing = Spacing(
        top = obj["top"]?.jsonPrimitive?.intOrNull ?: 0,
        right = obj["right"]?.jsonPrimitive?.intOrNull ?: 0,
        bottom = obj["bottom"]?.jsonPrimitive?.intOrNull ?: 0,
        left = obj["left"]?.jsonPrimitive?.intOrNull ?: 0
    )

    private fun serializeTextStyle(style: TextStyle): JsonObject = buildJsonObject {
        style.fontSize?.let { put("fontSize", it) }
        style.fontWeight?.let { put("fontWeight", it.name) }
        style.color?.let { put("color", it) }
        style.align?.let { put("align", it.name) }
    }

    private fun deserializeTextStyle(obj: JsonObject): TextStyle = TextStyle(
        fontSize = obj["fontSize"]?.jsonPrimitive?.intOrNull,
        fontWeight = obj["fontWeight"]?.jsonPrimitive?.content?.let { FontWeight.valueOf(it) },
        color = obj["color"]?.jsonPrimitive?.content,
        align = obj["align"]?.jsonPrimitive?.content?.let { TextAlign.valueOf(it) }
    )

    private fun serializeAction(action: A2UIAction): JsonObject = buildJsonObject {
        put("type", action.type.name)
        put("target", action.target)
        put("method", action.method)
        action.params?.let { params ->
            put("params", buildJsonObject {
                params.forEach { (k, v) -> put(k, v) }
            })
        }
    }

    private fun deserializeAction(obj: JsonObject): A2UIAction = A2UIAction(
        type = ActionType.valueOf(obj["type"]?.jsonPrimitive?.content ?: "CUSTOM"),
        target = obj["target"]?.jsonPrimitive?.content ?: "",
        method = obj["method"]?.jsonPrimitive?.content ?: "",
        params = (obj["params"] as? JsonObject)?.let { paramsObj ->
            paramsObj.entries.associate { (k, v) -> k to (v as JsonPrimitive).content }
        }
    )

    private fun serializeListItem(item: A2UIListItem): JsonObject = buildJsonObject {
        put("id", item.id)
        put("title", item.title)
        item.subtitle?.let { put("subtitle", it) }
        item.image?.let { put("image", it) }
        item.trailing?.let { put("trailing", it) }
        if (item.data.isNotEmpty()) {
            put("data", buildJsonObject {
                item.data.forEach { (k, v) -> put(k, v) }
            })
        }
    }

    private fun deserializeListItem(obj: JsonObject): A2UIListItem = A2UIListItem(
        id = obj["id"]?.jsonPrimitive?.content ?: "",
        title = obj["title"]?.jsonPrimitive?.content ?: "",
        subtitle = obj["subtitle"]?.jsonPrimitive?.content,
        image = obj["image"]?.jsonPrimitive?.content,
        trailing = obj["trailing"]?.jsonPrimitive?.content,
        data = (obj["data"] as? JsonObject)?.let { dataObj ->
            dataObj.entries.associate { (k, v) -> k to (v as JsonPrimitive).content }
        } ?: emptyMap()
    )

    private fun serializeTab(tab: A2UITab): JsonObject = buildJsonObject {
        put("id", tab.id)
        put("label", tab.label)
        tab.content?.let { put("content", serializeComponent(it)) }
    }

    private fun deserializeTab(obj: JsonObject): A2UITab = A2UITab(
        id = obj["id"]?.jsonPrimitive?.content ?: "",
        label = obj["label"]?.jsonPrimitive?.content ?: "",
        content = (obj["content"] as? JsonObject)?.let { deserializeComponent(it) }
    )
}
