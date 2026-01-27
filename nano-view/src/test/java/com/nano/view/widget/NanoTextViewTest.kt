package com.nano.view.widget

import com.nano.view.NanoView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * NanoTextView 单元测试
 */
class NanoTextViewTest {

    private lateinit var textView: NanoTextView

    @Before
    fun setUp() {
        textView = NanoTextView()
    }

    @Test
    fun `test initial state`() {
        assertEquals("", textView.text)
        assertEquals(14f, textView.textSize, 0.01f)
        assertEquals(0xFF000000.toInt(), textView.textColor)
    }

    @Test
    fun `test set text`() {
        textView.text = "Hello"
        assertEquals("Hello", textView.text)
    }

    @Test
    fun `test set text size`() {
        textView.textSize = 20f
        assertEquals(20f, textView.textSize, 0.01f)
    }

    @Test
    fun `test set text color`() {
        val red = 0xFFFF0000.toInt()
        textView.textColor = red
        assertEquals(red, textView.textColor)
    }

    @Test
    fun `test measure with exactly mode`() {
        textView.text = "Hello World"

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(200, NanoView.MeasureSpec.EXACTLY)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(50, NanoView.MeasureSpec.EXACTLY)

        textView.measure(widthSpec, heightSpec)

        assertEquals(200, textView.measuredWidth)
        assertEquals(50, textView.measuredHeight)
    }

    @Test
    fun `test measure with at most mode`() {
        textView.text = "Hi"
        textView.textSize = 14f

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(500, NanoView.MeasureSpec.AT_MOST)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.AT_MOST)

        textView.measure(widthSpec, heightSpec)

        // 文本较短，测量宽度应小于最大值
        assertTrue(textView.measuredWidth <= 500)
        assertTrue(textView.measuredHeight <= 100)
        assertTrue(textView.measuredWidth > 0)
        assertTrue(textView.measuredHeight > 0)
    }

    @Test
    fun `test measure with unspecified mode`() {
        textView.text = "Test"

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(0, NanoView.MeasureSpec.UNSPECIFIED)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(0, NanoView.MeasureSpec.UNSPECIFIED)

        textView.measure(widthSpec, heightSpec)

        assertTrue(textView.measuredWidth > 0)
        assertTrue(textView.measuredHeight > 0)
    }

    @Test
    fun `test measured size changes with text`() {
        textView.text = "Short"
        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(500, NanoView.MeasureSpec.AT_MOST)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.AT_MOST)
        textView.measure(widthSpec, heightSpec)
        val shortWidth = textView.measuredWidth

        textView.text = "This is a much longer text"
        textView.measure(widthSpec, heightSpec)
        val longWidth = textView.measuredWidth

        assertTrue(longWidth > shortWidth)
    }

    @Test
    fun `test measured size changes with text size`() {
        textView.text = "Test"
        textView.textSize = 10f
        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(500, NanoView.MeasureSpec.AT_MOST)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.AT_MOST)
        textView.measure(widthSpec, heightSpec)
        val smallHeight = textView.measuredHeight

        textView.textSize = 20f
        textView.measure(widthSpec, heightSpec)
        val largeHeight = textView.measuredHeight

        assertTrue(largeHeight > smallHeight)
    }

    @Test
    fun `test toString`() {
        textView.id = "my_text"
        textView.text = "Hello"
        textView.textSize = 16f

        val str = textView.toString()
        assertTrue(str.contains("NanoTextView"))
        assertTrue(str.contains("my_text"))
        assertTrue(str.contains("Hello"))
        assertTrue(str.contains("16"))
    }
}
