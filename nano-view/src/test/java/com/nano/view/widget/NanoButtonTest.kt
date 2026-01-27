package com.nano.view.widget

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * NanoButton 单元测试
 */
class NanoButtonTest {

    private lateinit var button: NanoButton

    @Before
    fun setUp() {
        button = NanoButton()
    }

    @Test
    fun `test initial state`() {
        assertTrue(button.isEnabled)
        assertTrue(button.isClickable)
        assertEquals(0xFFCCCCCC.toInt(), button.backgroundColor)
    }

    @Test
    fun `test set enabled`() {
        button.isEnabled = true
        assertTrue(button.isEnabled)
        assertEquals(0xFF000000.toInt(), button.textColor)

        button.isEnabled = false
        assertFalse(button.isEnabled)
        assertEquals(0xFF888888.toInt(), button.textColor)
    }

    @Test
    fun `test enabled button responds to touch`() {
        button.isEnabled = true
        var clicked = false
        button.setOnClickListener { clicked = true }

        button.layout(0, 0, 100, 50)
        val handled = button.onTouchEvent(50, 25)

        assertTrue(handled)
        assertTrue(clicked)
    }

    @Test
    fun `test disabled button does not respond to touch`() {
        button.isEnabled = false
        var clicked = false
        button.setOnClickListener { clicked = true }

        button.layout(0, 0, 100, 50)
        val handled = button.onTouchEvent(50, 25)

        assertFalse(handled)
        assertFalse(clicked)
    }

    @Test
    fun `test enabled button can perform click`() {
        button.isEnabled = true
        var clicked = false
        button.setOnClickListener { clicked = true }

        button.performClick()
        assertTrue(clicked)
    }

    @Test
    fun `test disabled button cannot perform click`() {
        button.isEnabled = false
        var clicked = false
        button.setOnClickListener { clicked = true }

        button.performClick()
        assertFalse(clicked)
    }

    @Test
    fun `test button inherits text view properties`() {
        button.text = "Click Me"
        button.textSize = 16f

        assertEquals("Click Me", button.text)
        assertEquals(16f, button.textSize, 0.01f)
    }

    @Test
    fun `test toString`() {
        button.id = "my_button"
        button.text = "Submit"
        button.isEnabled = true

        val str = button.toString()
        assertTrue(str.contains("NanoButton"))
        assertTrue(str.contains("my_button"))
        assertTrue(str.contains("Submit"))
        assertTrue(str.contains("enabled=true"))
    }

    @Test
    fun `test button background color`() {
        val blue = 0xFF0000FF.toInt()
        button.backgroundColor = blue
        assertEquals(blue, button.backgroundColor)
    }
}
