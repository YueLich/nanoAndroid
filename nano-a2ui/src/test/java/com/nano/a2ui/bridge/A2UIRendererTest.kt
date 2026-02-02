package com.nano.a2ui.bridge

import com.nano.llm.a2ui.*
import com.nano.view.widget.NanoButton
import com.nano.view.widget.NanoLinearLayout
import com.nano.view.widget.NanoTextView
import org.junit.Assert.*
import org.junit.Test

class A2UIRendererTest {

    private val renderer = A2UIRenderer()

    @Test
    fun testRenderText() {
        val spec = A2UISpec(root = A2UIText(id = "t1", text = "Hello"))
        val view = renderer.render(spec)

        assertNotNull(view)
        assertTrue(view is NanoTextView)
        assertEquals("t1", view.id)
        assertEquals("Hello", (view as NanoTextView).text)
    }

    @Test
    fun testRenderTextWithStyle() {
        val spec = A2UISpec(
            root = A2UIText(
                text = "Styled",
                textStyle = TextStyle(fontSize = 24, color = "#FF0000")
            )
        )
        val view = renderer.render(spec) as NanoTextView

        assertEquals(24f, view.textSize)
        assertEquals(0xFFFF0000.toInt(), view.textColor)
    }

    @Test
    fun testRenderButton() {
        val action = A2UIAction(ActionType.AGENT_CALL, "calc", "run")
        val spec = A2UISpec(root = A2UIButton(id = "btn1", text = "Run", action = action))
        val view = renderer.render(spec)

        assertTrue(view is NanoButton)
        assertEquals("btn1", view.id)
        assertEquals("Run", (view as NanoButton).text)
    }

    @Test
    fun testRenderButtonAutoId() {
        val action = A2UIAction(ActionType.AGENT_CALL, "agent1", "method1")
        val spec = A2UISpec(root = A2UIButton(text = "Click", action = action))
        val view = renderer.render(spec)

        // 无 ID 时自动生成
        assertTrue(view.id!!.startsWith("btn_agent1_method1"))
    }

    @Test
    fun testRenderVerticalContainer() {
        val spec = A2UISpec(
            root = A2UIContainer(
                id = "vc",
                direction = Direction.VERTICAL,
                children = listOf(
                    A2UIText(text = "A"),
                    A2UIText(text = "B"),
                    A2UIText(text = "C")
                )
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        assertEquals("vc", view.id)
        assertEquals(NanoLinearLayout.VERTICAL, view.orientation)
        assertEquals(3, view.getChildCount())
    }

    @Test
    fun testRenderHorizontalContainer() {
        val spec = A2UISpec(
            root = A2UIContainer(
                direction = Direction.HORIZONTAL,
                children = listOf(A2UIText(text = "X"))
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        assertEquals(NanoLinearLayout.HORIZONTAL, view.orientation)
    }

    @Test
    fun testRenderEmptyContainer() {
        val spec = A2UISpec(
            root = A2UIContainer(id = "empty", children = emptyList())
        )
        val view = renderer.render(spec) as NanoLinearLayout

        assertEquals(0, view.getChildCount())
    }

    @Test
    fun testRenderList() {
        val spec = A2UISpec(
            root = A2UIList(
                id = "list1",
                items = listOf(
                    A2UIListItem(id = "i1", title = "Item 1", subtitle = "Sub"),
                    A2UIListItem(id = "i2", title = "Item 2", trailing = "→")
                )
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        assertEquals("list1", view.id)
        assertEquals(2, view.getChildCount())

        val item1 = view.getChildAt(0) as NanoTextView
        assertEquals("list_item_i1", item1.id)
        assertTrue(item1.text.contains("Item 1"))
        assertTrue(item1.text.contains("Sub"))

        val item2 = view.getChildAt(1) as NanoTextView
        assertTrue(item2.text.contains("[→]"))
    }

    @Test
    fun testRenderEmptyList() {
        val spec = A2UISpec(root = A2UIList(id = "emptyList", items = emptyList()))
        val view = renderer.render(spec) as NanoLinearLayout

        assertEquals(0, view.getChildCount())
    }

    @Test
    fun testRenderCard() {
        val spec = A2UISpec(
            root = A2UICard(
                id = "card1",
                header = A2UIText(text = "Header"),
                content = A2UIText(text = "Body"),
                footer = A2UIText(text = "Footer")
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        assertEquals("card1", view.id)
        assertEquals(3, view.getChildCount())
        assertEquals("Header", (view.getChildAt(0) as NanoTextView).text)
        assertEquals("Body", (view.getChildAt(1) as NanoTextView).text)
        assertEquals("Footer", (view.getChildAt(2) as NanoTextView).text)
    }

    @Test
    fun testRenderCardWithoutFooter() {
        val spec = A2UISpec(
            root = A2UICard(
                content = A2UIText(text = "Only content")
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        // 仅 content，无 header/footer
        assertEquals(1, view.getChildCount())
    }

    @Test
    fun testRenderInput() {
        val spec = A2UISpec(
            root = A2UIInput(id = "inp1", placeholder = "Enter name")
        )
        val view = renderer.render(spec) as NanoTextView

        assertEquals("inp1", view.id)
        assertEquals("Enter name", view.text)
    }

    @Test
    fun testRenderInputWithValue() {
        val spec = A2UISpec(
            root = A2UIInput(placeholder = "ph", value = "existing value")
        )
        val view = renderer.render(spec) as NanoTextView

        // value 优先于 placeholder
        assertEquals("existing value", view.text)
    }

    @Test
    fun testRenderTabBar() {
        val spec = A2UISpec(
            root = A2UITabBar(
                id = "tabbar1",
                tabs = listOf(
                    A2UITab(id = "t1", label = "美团"),
                    A2UITab(id = "t2", label = "饿了么")
                )
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        assertEquals("tabbar1", view.id)
        assertEquals(NanoLinearLayout.HORIZONTAL, view.orientation)
        assertEquals(2, view.getChildCount())
        assertEquals("美团", (view.getChildAt(0) as NanoTextView).text)
    }

    @Test
    fun testRenderTabContent() {
        val spec = A2UISpec(
            root = A2UITabContent(
                id = "tabcontent1",
                tabs = listOf(
                    A2UITab(id = "t1", label = "First", content = A2UIText(text = "Tab 1 Content")),
                    A2UITab(id = "t2", label = "Second", content = A2UIText(text = "Tab 2 Content"))
                )
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        // 只渲染第一个 tab 的内容
        assertEquals(1, view.getChildCount())
        assertEquals("Tab 1 Content", (view.getChildAt(0) as NanoTextView).text)
    }

    @Test
    fun testRenderNestedContainer() {
        val spec = A2UISpec(
            root = A2UIContainer(
                id = "outer",
                children = listOf(
                    A2UIContainer(
                        id = "inner",
                        direction = Direction.HORIZONTAL,
                        children = listOf(A2UIText(text = "Nested"))
                    ),
                    A2UIText(text = "Sibling")
                )
            )
        )
        val outer = renderer.render(spec) as NanoLinearLayout

        assertEquals(2, outer.getChildCount())
        val inner = outer.getChildAt(0) as NanoLinearLayout
        assertEquals("inner", inner.id)
        assertEquals(1, inner.getChildCount())
    }

    @Test
    fun testRenderComplexSpec() {
        val spec = A2UISpec(
            root = A2UIContainer(
                id = "root",
                children = listOf(
                    A2UIText(text = "Title", textStyle = TextStyle(fontWeight = FontWeight.BOLD)),
                    A2UIList(items = listOf(
                        A2UIListItem(id = "1", title = "Item"),
                        A2UIListItem(id = "2", title = "Item 2")
                    )),
                    A2UIButton(
                        text = "Action",
                        action = A2UIAction(ActionType.AGENT_CALL, "app", "go")
                    )
                )
            )
        )

        val root = renderer.render(spec) as NanoLinearLayout
        assertEquals(3, root.getChildCount())
        assertTrue(root.getChildAt(0) is NanoTextView)
        assertTrue(root.getChildAt(1) is NanoLinearLayout)
        assertTrue(root.getChildAt(2) is NanoButton)
    }

    @Test
    fun testRenderTextColorHex6() {
        val spec = A2UISpec(
            root = A2UIText(text = "red", textStyle = TextStyle(color = "#FF0000"))
        )
        val view = renderer.render(spec) as NanoTextView

        assertEquals(0xFFFF0000.toInt(), view.textColor)
    }

    @Test
    fun testRenderTextColorHex8() {
        val spec = A2UISpec(
            root = A2UIText(text = "semi", textStyle = TextStyle(color = "#80FF0000"))
        )
        val view = renderer.render(spec) as NanoTextView

        assertEquals(0x80FF0000.toInt(), view.textColor)
    }

    @Test
    fun testApplyStyleWithPadding() {
        val spec = A2UISpec(
            root = A2UIText(
                text = "Padded",
                style = A2UIStyle(
                    padding = Spacing(top = 10, right = 20, bottom = 30, left = 40)
                )
            )
        )
        val view = renderer.render(spec)

        assertEquals(40, view.paddingLeft)
        assertEquals(10, view.paddingTop)
        assertEquals(20, view.paddingRight)
        assertEquals(30, view.paddingBottom)
    }

    @Test
    fun testApplyStyleWithMargin() {
        val spec = A2UISpec(
            root = A2UIButton(
                text = "Margined",
                action = A2UIAction(ActionType.AGENT_CALL, "test", "test"),
                style = A2UIStyle(
                    margin = Spacing(top = 5, right = 10, bottom = 15, left = 20)
                )
            )
        )
        val view = renderer.render(spec)

        assertNotNull(view.layoutParams)
        assertEquals(20, view.layoutParams!!.leftMargin)
        assertEquals(5, view.layoutParams!!.topMargin)
        assertEquals(10, view.layoutParams!!.rightMargin)
        assertEquals(15, view.layoutParams!!.bottomMargin)
    }

    @Test
    fun testApplyStyleWithBackgroundColor() {
        val spec = A2UISpec(
            root = A2UIContainer(
                children = emptyList(),
                style = A2UIStyle(backgroundColor = "#FFAA00")
            )
        )
        val view = renderer.render(spec)

        assertEquals(0xFFFFAA00.toInt(), view.backgroundColor)
    }

    @Test
    fun testApplyStyleWithDimensions() {
        val spec = A2UISpec(
            root = A2UIText(
                text = "Sized",
                style = A2UIStyle(width = 200, height = 100)
            )
        )
        val view = renderer.render(spec)

        assertNotNull(view.layoutParams)
        assertEquals(200, view.layoutParams!!.width)
        assertEquals(100, view.layoutParams!!.height)
    }

    @Test
    fun testApplyStyleWithBorderRadius() {
        val spec = A2UISpec(
            root = A2UIButton(
                text = "Rounded",
                action = A2UIAction(ActionType.AGENT_CALL, "test", "test"),
                style = A2UIStyle(borderRadius = 8)
            )
        )
        val view = renderer.render(spec)

        assertEquals(8, view.borderRadius)
    }

    @Test
    fun testRenderButtonWithAction() {
        val action = A2UIAction(
            type = ActionType.AGENT_CALL,
            target = "notepad",
            method = "add_note",
            params = mapOf("title" to "Test")
        )
        val spec = A2UISpec(
            root = A2UIButton(text = "Add", action = action)
        )
        val view = renderer.render(spec) as NanoButton

        // 验证 action 存储在 tag 中
        assertNotNull(view.tag)
        assertTrue(view.tag is A2UIAction)
        val storedAction = view.tag as A2UIAction
        assertEquals("notepad", storedAction.target)
        assertEquals("add_note", storedAction.method)
    }

    @Test
    fun testRenderListWithItemClick() {
        val onItemClick = A2UIAction(
            type = ActionType.AGENT_CALL,
            target = "notepad",
            method = "view_note"
        )
        val spec = A2UISpec(
            root = A2UIList(
                items = listOf(
                    A2UIListItem(id = "note1", title = "Note 1", data = mapOf("note_id" to "1"))
                ),
                onItemClick = onItemClick
            )
        )
        val view = renderer.render(spec) as NanoLinearLayout

        val itemView = view.getChildAt(0)
        assertTrue(itemView.isClickable)
        assertNotNull(itemView.tag)
    }
}
