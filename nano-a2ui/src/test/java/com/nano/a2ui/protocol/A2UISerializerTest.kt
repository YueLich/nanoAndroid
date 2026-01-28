package com.nano.a2ui.protocol

import com.nano.llm.a2ui.*
import org.junit.Assert.*
import org.junit.Test

class A2UISerializerTest {

    @Test
    fun testSerializeText() {
        val spec = A2UISpec(root = A2UIText(id = "txt1", text = "Hello"))
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"type\": \"text\""))
        assertTrue(json.contains("\"text\": \"Hello\""))
        assertTrue(json.contains("\"id\": \"txt1\""))
    }

    @Test
    fun testSerializeButton() {
        val action = A2UIAction(ActionType.AGENT_CALL, "calc", "calculate")
        val spec = A2UISpec(root = A2UIButton(id = "btn1", text = "计算", action = action))
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"type\": \"button\""))
        assertTrue(json.contains("\"text\": \"计算\""))
        assertTrue(json.contains("\"target\": \"calc\""))
    }

    @Test
    fun testSerializeContainer() {
        val spec = A2UISpec(
            root = A2UIContainer(
                id = "container1",
                direction = Direction.HORIZONTAL,
                children = listOf(
                    A2UIText(text = "A"),
                    A2UIText(text = "B")
                )
            )
        )
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"type\": \"container\""))
        assertTrue(json.contains("\"direction\": \"HORIZONTAL\""))
        assertTrue(json.contains("\"children\""))
    }

    @Test
    fun testSerializeList() {
        val spec = A2UISpec(
            root = A2UIList(
                id = "list1",
                items = listOf(
                    A2UIListItem(id = "i1", title = "Item 1", subtitle = "Sub 1"),
                    A2UIListItem(id = "i2", title = "Item 2")
                )
            )
        )
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"type\": \"list\""))
        assertTrue(json.contains("\"title\": \"Item 1\""))
        assertTrue(json.contains("\"subtitle\": \"Sub 1\""))
    }

    @Test
    fun testSerializeCard() {
        val spec = A2UISpec(
            root = A2UICard(
                id = "card1",
                header = A2UIText(text = "Header"),
                content = A2UIText(text = "Content"),
                footer = A2UIText(text = "Footer")
            )
        )
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"type\": \"card\""))
        assertTrue(json.contains("\"header\""))
        assertTrue(json.contains("\"footer\""))
    }

    @Test
    fun testSerializeInput() {
        val spec = A2UISpec(
            root = A2UIInput(
                id = "input1",
                placeholder = "Enter text",
                inputType = InputType.MULTILINE
            )
        )
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"type\": \"input\""))
        assertTrue(json.contains("\"placeholder\": \"Enter text\""))
        assertTrue(json.contains("\"inputType\": \"MULTILINE\""))
    }

    @Test
    fun testSerializeTabBar() {
        val spec = A2UISpec(
            root = A2UITabBar(
                id = "tabs1",
                tabs = listOf(
                    A2UITab(id = "t1", label = "Tab A"),
                    A2UITab(id = "t2", label = "Tab B")
                )
            )
        )
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"type\": \"tabbar\""))
        assertTrue(json.contains("\"label\": \"Tab A\""))
    }

    @Test
    fun testSerializeWithStyle() {
        val spec = A2UISpec(
            root = A2UIText(
                text = "Styled",
                style = A2UIStyle(
                    width = 200,
                    height = 100,
                    backgroundColor = "#FF0000",
                    borderRadius = 8,
                    padding = Spacing(top = 10, right = 5, bottom = 10, left = 5)
                )
            )
        )
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"width\": 200"))
        assertTrue(json.contains("\"backgroundColor\": \"#FF0000\""))
        assertTrue(json.contains("\"borderRadius\": 8"))
    }

    @Test
    fun testSerializeVersion() {
        val spec = A2UISpec(version = "2.0", root = A2UIText(text = "v2"))
        val json = A2UISerializer.serialize(spec)

        assertTrue(json.contains("\"version\": \"2.0\""))
    }

    // ==================== 往返测试 ====================

    @Test
    fun testRoundTripText() {
        val original = A2UISpec(root = A2UIText(id = "t1", text = "Round trip"))
        val json = A2UISerializer.serialize(original)
        val restored = A2UISerializer.deserialize(json)

        val root = restored.root as A2UIText
        assertEquals("t1", root.id)
        assertEquals("Round trip", root.text)
    }

    @Test
    fun testRoundTripButton() {
        val action = A2UIAction(ActionType.SUBMIT, "agent1", "submit", mapOf("key" to "val"))
        val original = A2UISpec(root = A2UIButton(id = "b1", text = "Go", action = action))

        val restored = A2UISerializer.deserialize(A2UISerializer.serialize(original))
        val root = restored.root as A2UIButton

        assertEquals("Go", root.text)
        assertEquals("agent1", root.action.target)
        assertEquals("val", root.action.params?.get("key"))
    }

    @Test
    fun testRoundTripContainer() {
        val original = A2UISpec(
            root = A2UIContainer(
                id = "c1",
                direction = Direction.HORIZONTAL,
                children = listOf(
                    A2UIText(text = "Child 1"),
                    A2UIText(text = "Child 2")
                )
            )
        )

        val restored = A2UISerializer.deserialize(A2UISerializer.serialize(original))
        val root = restored.root as A2UIContainer

        assertEquals(Direction.HORIZONTAL, root.direction)
        assertEquals(2, root.children.size)
        assertEquals("Child 1", (root.children[0] as A2UIText).text)
    }

    @Test
    fun testRoundTripList() {
        val original = A2UISpec(
            root = A2UIList(
                id = "l1",
                items = listOf(
                    A2UIListItem(id = "i1", title = "Title", subtitle = "Sub", data = mapOf("k" to "v")),
                    A2UIListItem(id = "i2", title = "Title 2")
                )
            )
        )

        val restored = A2UISerializer.deserialize(A2UISerializer.serialize(original))
        val root = restored.root as A2UIList

        assertEquals(2, root.items.size)
        assertEquals("Title", root.items[0].title)
        assertEquals("Sub", root.items[0].subtitle)
        assertEquals("v", root.items[0].data["k"])
    }

    @Test
    fun testRoundTripCard() {
        val original = A2UISpec(
            root = A2UICard(
                id = "card1",
                header = A2UIText(text = "H"),
                content = A2UIText(text = "C"),
                footer = A2UIText(text = "F")
            )
        )

        val restored = A2UISerializer.deserialize(A2UISerializer.serialize(original))
        val root = restored.root as A2UICard

        assertEquals("H", (root.header as A2UIText).text)
        assertEquals("C", (root.content as A2UIText).text)
        assertEquals("F", (root.footer as A2UIText).text)
    }

    @Test
    fun testRoundTripNestedContainer() {
        val original = A2UISpec(
            root = A2UIContainer(
                id = "outer",
                children = listOf(
                    A2UIContainer(
                        id = "inner",
                        direction = Direction.HORIZONTAL,
                        children = listOf(A2UIText(text = "Nested"))
                    )
                )
            )
        )

        val restored = A2UISerializer.deserialize(A2UISerializer.serialize(original))
        val outer = restored.root as A2UIContainer
        val inner = outer.children[0] as A2UIContainer
        val text = inner.children[0] as A2UIText

        assertEquals("inner", inner.id)
        assertEquals(Direction.HORIZONTAL, inner.direction)
        assertEquals("Nested", text.text)
    }

    @Test
    fun testRoundTripStyle() {
        val original = A2UISpec(
            root = A2UIText(
                text = "styled",
                style = A2UIStyle(
                    width = 300,
                    padding = Spacing(top = 5, right = 10, bottom = 5, left = 10),
                    backgroundColor = "#00FF00"
                )
            )
        )

        val restored = A2UISerializer.deserialize(A2UISerializer.serialize(original))
        val root = restored.root as A2UIText

        assertEquals(300, root.style!!.width)
        assertEquals(5, root.style!!.padding!!.top)
        assertEquals("#00FF00", root.style!!.backgroundColor)
    }

    @Test
    fun testRoundTripTextStyle() {
        val original = A2UISpec(
            root = A2UIText(
                text = "bold",
                textStyle = TextStyle(fontSize = 18, fontWeight = FontWeight.BOLD, color = "#333333", align = TextAlign.CENTER)
            )
        )

        val restored = A2UISerializer.deserialize(A2UISerializer.serialize(original))
        val root = restored.root as A2UIText

        assertEquals(18, root.textStyle!!.fontSize)
        assertEquals(FontWeight.BOLD, root.textStyle!!.fontWeight)
        assertEquals("#333333", root.textStyle!!.color)
        assertEquals(TextAlign.CENTER, root.textStyle!!.align)
    }

    @Test
    fun testDeserializeUnknownType() {
        val json = """{"version":"1.0","root":{"type":"unknown_widget","text":"fallback"}}"""
        val spec = A2UISerializer.deserialize(json)

        // 未知类型应回退为 A2UIText
        val root = spec.root as A2UIText
        assertTrue(root.text.contains("Unknown"))
    }
}
