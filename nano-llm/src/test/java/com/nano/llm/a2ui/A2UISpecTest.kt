package com.nano.llm.a2ui

import org.junit.Test
import org.junit.Assert.*

class A2UISpecTest {

    @Test
    fun testCreateA2UISpecDefaultVersion() {
        val text = A2UIText(text = "Hello")
        val spec = A2UISpec(root = text)
        assertEquals("1.0", spec.version)
        assertNotNull(spec.root)
    }

    @Test
    fun testCustomVersion() {
        val spec = A2UISpec(version = "2.0", root = A2UIText(text = "test"))
        assertEquals("2.0", spec.version)
    }

    @Test
    fun testContainerWithChildren() {
        val child1 = A2UIText(text = "First")
        val child2 = A2UIText(text = "Second")
        val container = A2UIContainer(
            id = "main",
            direction = Direction.VERTICAL,
            children = listOf(child1, child2)
        )
        assertEquals("main", container.id)
        assertEquals(Direction.VERTICAL, container.direction)
        assertEquals(2, container.children.size)
    }

    @Test
    fun testContainerHorizontal() {
        val container = A2UIContainer(
            direction = Direction.HORIZONTAL,
            children = emptyList()
        )
        assertEquals(Direction.HORIZONTAL, container.direction)
    }

    @Test
    fun testTextComponent() {
        val text = A2UIText(
            id = "title",
            text = "标题文本",
            textStyle = TextStyle(fontSize = 18, fontWeight = FontWeight.BOLD)
        )
        val ts = text.textStyle!!
        assertEquals("title", text.id)
        assertEquals("标题文本", text.text)
        assertEquals(18, ts.fontSize)
        assertEquals(FontWeight.BOLD, ts.fontWeight)
    }

    @Test
    fun testTextComponentMinimal() {
        val text = A2UIText(text = "simple")
        assertNull(text.id)
        assertNull(text.style)
        assertNull(text.textStyle)
        assertEquals("simple", text.text)
    }

    @Test
    fun testButtonComponent() {
        val action = A2UIAction(
            type = ActionType.AGENT_CALL,
            target = "meituan",
            method = "search"
        )
        val button = A2UIButton(
            id = "btn1",
            text = "搜索",
            action = action
        )
        assertEquals("搜索", button.text)
        assertEquals(ActionType.AGENT_CALL, button.action.type)
        assertEquals("meituan", button.action.target)
        assertEquals("search", button.action.method)
    }

    @Test
    fun testButtonActionWithParams() {
        val action = A2UIAction(
            type = ActionType.SUBMIT,
            target = "form",
            method = "submit",
            params = mapOf("key" to "value")
        )
        assertEquals("value", action.params!!["key"])
    }

    @Test
    fun testListComponent() {
        val items = listOf(
            A2UIListItem(id = "item1", title = "Shop A", subtitle = "Near", trailing = "¥10"),
            A2UIListItem(id = "item2", title = "Shop B", trailing = "¥15")
        )
        val list = A2UIList(id = "shop_list", items = items)

        assertEquals(2, list.items.size)
        assertEquals("Shop A", list.items[0].title)
        assertEquals("¥15", list.items[1].trailing)
        assertNull(list.onItemClick)
    }

    @Test
    fun testListWithOnItemClick() {
        val action = A2UIAction(ActionType.AGENT_CALL, "agent", "open")
        val list = A2UIList(
            items = listOf(A2UIListItem(id = "i1", title = "Item")),
            onItemClick = action
        )
        assertNotNull(list.onItemClick)
        assertEquals("open", list.onItemClick!!.method)
    }

    @Test
    fun testListItemWithData() {
        val item = A2UIListItem(
            id = "item1",
            title = "Test Shop",
            data = mapOf("shopId" to "001", "rating" to "4.8")
        )
        assertEquals("001", item.data["shopId"])
        assertEquals("4.8", item.data["rating"])
    }

    @Test
    fun testListItemDefaults() {
        val item = A2UIListItem(id = "i1", title = "Title")
        assertNull(item.subtitle)
        assertNull(item.image)
        assertNull(item.trailing)
        assertTrue(item.data.isEmpty())
    }

    @Test
    fun testCardComponent() {
        val header = A2UIText(text = "美团外卖")
        val content = A2UIText(text = "黄焖鸡 15家")
        val card = A2UICard(
            id = "card1",
            header = header,
            content = content
        )
        assertEquals("card1", card.id)
        assertNotNull(card.header)
        assertNull(card.footer)
        assertNull(card.action)
    }

    @Test
    fun testCardWithFooterAndAction() {
        val card = A2UICard(
            content = A2UIText(text = "main"),
            footer = A2UIText(text = "footer"),
            action = A2UIAction(ActionType.DISMISS, "card", "close")
        )
        assertNotNull(card.footer)
        assertNotNull(card.action)
    }

    @Test
    fun testInputComponent() {
        val input = A2UIInput(
            id = "search_input",
            placeholder = "搜索...",
            inputType = InputType.TEXT,
            onSubmit = A2UIAction(ActionType.AGENT_CALL, "system", "search")
        )
        assertEquals("搜索...", input.placeholder)
        assertEquals(InputType.TEXT, input.inputType)
        assertNotNull(input.onSubmit)
    }

    @Test
    fun testInputDefaults() {
        val input = A2UIInput()
        assertNull(input.placeholder)
        assertNull(input.value)
        assertEquals(InputType.TEXT, input.inputType)
        assertNull(input.onSubmit)
    }

    @Test
    fun testTabBarComponent() {
        val tabs = listOf(
            A2UITab(id = "meituan", label = "美团"),
            A2UITab(id = "eleme", label = "饿了么")
        )
        val tabBar = A2UITabBar(id = "tabs", tabs = tabs)
        assertEquals(2, tabBar.tabs.size)
        assertEquals("美团", tabBar.tabs[0].label)
        assertEquals("饿了么", tabBar.tabs[1].label)
    }

    @Test
    fun testTabContentComponent() {
        val tabs = listOf(
            A2UITab(id = "tab1", label = "Tab 1", content = A2UIText(text = "Content 1")),
            A2UITab(id = "tab2", label = "Tab 2", content = A2UIText(text = "Content 2"))
        )
        val tabContent = A2UITabContent(tabs = tabs)
        assertEquals(2, tabContent.tabs.size)
        assertNotNull(tabContent.tabs[0].content)
    }

    @Test
    fun testA2UIStyle() {
        val style = A2UIStyle(
            width = 200,
            height = 100,
            padding = Spacing(16, 16, 16, 16),
            margin = Spacing(bottom = 8),
            backgroundColor = "#FF5733",
            borderRadius = 12
        )
        assertEquals(200, style.width)
        assertEquals(16, style.padding!!.top)
        assertEquals(8, style.margin!!.bottom)
        assertEquals("#FF5733", style.backgroundColor)
        assertEquals(12, style.borderRadius)
    }

    @Test
    fun testSpacingDefaults() {
        val spacing = Spacing()
        assertEquals(0, spacing.top)
        assertEquals(0, spacing.right)
        assertEquals(0, spacing.bottom)
        assertEquals(0, spacing.left)
    }

    @Test
    fun testActionTypes() {
        assertEquals(5, ActionType.values().size)
        assertNotNull(ActionType.valueOf("AGENT_CALL"))
        assertNotNull(ActionType.valueOf("NAVIGATE"))
        assertNotNull(ActionType.valueOf("SUBMIT"))
        assertNotNull(ActionType.valueOf("DISMISS"))
        assertNotNull(ActionType.valueOf("CUSTOM"))
    }

    @Test
    fun testInputTypes() {
        assertEquals(4, InputType.values().size)
        assertNotNull(InputType.valueOf("TEXT"))
        assertNotNull(InputType.valueOf("NUMBER"))
        assertNotNull(InputType.valueOf("MULTILINE"))
    }

    @Test
    fun testNestedContainers() {
        val inner = A2UIContainer(
            id = "inner",
            direction = Direction.HORIZONTAL,
            children = listOf(
                A2UIText(text = "Left"),
                A2UIText(text = "Right")
            )
        )
        val outer = A2UIContainer(
            id = "outer",
            direction = Direction.VERTICAL,
            children = listOf(
                A2UIText(text = "Title"),
                inner
            )
        )
        val spec = A2UISpec(root = outer)

        val outerContainer = spec.root as A2UIContainer
        assertEquals("outer", outerContainer.id)
        assertEquals(2, outerContainer.children.size)

        val innerContainer = outerContainer.children[1] as A2UIContainer
        assertEquals("inner", innerContainer.id)
        assertEquals(Direction.HORIZONTAL, innerContainer.direction)
    }

    @Test
    fun testComponentStyle() {
        val style = A2UIStyle(padding = Spacing(8, 8, 8, 8))
        val text = A2UIText(text = "Styled", style = style)
        assertEquals(8, text.style!!.padding!!.top)
    }

    @Test
    fun testTextStyleFields() {
        val style = TextStyle(
            fontSize = 14,
            fontWeight = FontWeight.NORMAL,
            color = "#333333",
            align = TextAlign.CENTER
        )
        assertEquals(14, style.fontSize)
        assertEquals(FontWeight.NORMAL, style.fontWeight)
        assertEquals("#333333", style.color)
        assertEquals(TextAlign.CENTER, style.align)
    }
}
