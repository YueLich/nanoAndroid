package com.nano.view

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * NanoView 单元测试
 */
class NanoViewTest {

    private lateinit var view: TestView

    /**
     * 测试用的 View 实现
     */
    class TestView : NanoView() {
        var onMeasureCalled = false
        var onLayoutCalled = false
        var onDrawCalled = false
        var performClickCalled = false

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            onMeasureCalled = true
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            onLayoutCalled = true
        }

        override fun onDraw() {
            onDrawCalled = true
        }

        override fun performClick() {
            performClickCalled = true
            super.performClick()
        }

        fun reset() {
            onMeasureCalled = false
            onLayoutCalled = false
            onDrawCalled = false
            performClickCalled = false
        }
    }

    @Before
    fun setUp() {
        view = TestView()
    }

    @Test
    fun `test initial state`() {
        assertNull(view.id)
        assertNull(view.parent)
        assertEquals(NanoView.VISIBLE, view.visibility)
        assertEquals(0, view.measuredWidth)
        assertEquals(0, view.measuredHeight)
        assertFalse(view.isClickable)
    }

    @Test
    fun `test set id`() {
        view.id = "test_view"
        assertEquals("test_view", view.id)
    }

    @Test
    fun `test visibility`() {
        assertEquals(NanoView.VISIBLE, view.visibility)

        view.visibility = NanoView.INVISIBLE
        assertEquals(NanoView.INVISIBLE, view.visibility)

        view.visibility = NanoView.GONE
        assertEquals(NanoView.GONE, view.visibility)
    }

    @Test
    fun `test measure`() {
        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.EXACTLY)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(200, NanoView.MeasureSpec.EXACTLY)

        view.measure(widthSpec, heightSpec)

        assertTrue(view.onMeasureCalled)
        assertEquals(100, view.measuredWidth)
        assertEquals(200, view.measuredHeight)
    }

    @Test
    fun `test layout`() {
        view.layout(10, 20, 110, 220)

        assertTrue(view.onLayoutCalled)
        assertEquals(10, view.left)
        assertEquals(20, view.top)
        assertEquals(110, view.right)
        assertEquals(220, view.bottom)
        assertEquals(100, view.getWidth())
        assertEquals(200, view.getHeight())
    }

    @Test
    fun `test layout does not trigger onLayout when unchanged`() {
        view.layout(10, 20, 110, 220)
        view.reset()

        view.layout(10, 20, 110, 220)
        assertFalse(view.onLayoutCalled)
    }

    @Test
    fun `test draw visible view`() {
        view.visibility = NanoView.VISIBLE
        view.draw()
        assertTrue(view.onDrawCalled)
    }

    @Test
    fun `test draw invisible view`() {
        view.visibility = NanoView.INVISIBLE
        view.draw()
        assertFalse(view.onDrawCalled)
    }

    @Test
    fun `test draw gone view`() {
        view.visibility = NanoView.GONE
        view.draw()
        assertFalse(view.onDrawCalled)
    }

    @Test
    fun `test click listener`() {
        var clicked = false
        view.setOnClickListener {
            clicked = true
        }

        assertTrue(view.isClickable)

        view.layout(0, 0, 100, 100)
        view.onTouchEvent(50, 50)

        assertTrue(view.performClickCalled)
        assertTrue(clicked)
    }

    @Test
    fun `test touch event outside bounds`() {
        view.setOnClickListener {
            fail("Should not be called")
        }

        view.layout(0, 0, 100, 100)
        val handled = view.onTouchEvent(150, 150)

        assertFalse(handled)
        assertFalse(view.performClickCalled)
    }

    @Test
    fun `test not clickable view`() {
        view.isClickable = false
        view.layout(0, 0, 100, 100)
        val handled = view.onTouchEvent(50, 50)

        assertFalse(handled)
    }

    @Test
    fun `test needs layout`() {
        assertFalse(view.needsLayout())

        view.measure(
            NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.EXACTLY),
            NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 100, 100)

        assertFalse(view.needsLayout())

        view.requestLayout()
        assertTrue(view.needsLayout())
    }

    @Test
    fun `test is visible`() {
        view.visibility = NanoView.VISIBLE
        assertTrue(view.isVisible())
        assertFalse(view.isGone())

        view.visibility = NanoView.INVISIBLE
        assertFalse(view.isVisible())
        assertFalse(view.isGone())

        view.visibility = NanoView.GONE
        assertFalse(view.isVisible())
        assertTrue(view.isGone())
    }

    @Test
    fun `test find view by id`() {
        view.id = "test_view"
        val found = view.findViewById("test_view")
        assertSame(view, found)

        val notFound = view.findViewById("other_id")
        assertNull(notFound)
    }

    @Test
    fun `test measure spec exactly`() {
        val spec = NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.EXACTLY)
        assertEquals(NanoView.MeasureSpec.EXACTLY, NanoView.MeasureSpec.getMode(spec))
        assertEquals(100, NanoView.MeasureSpec.getSize(spec))
    }

    @Test
    fun `test measure spec at most`() {
        val spec = NanoView.MeasureSpec.makeMeasureSpec(200, NanoView.MeasureSpec.AT_MOST)
        assertEquals(NanoView.MeasureSpec.AT_MOST, NanoView.MeasureSpec.getMode(spec))
        assertEquals(200, NanoView.MeasureSpec.getSize(spec))
    }

    @Test
    fun `test measure spec unspecified`() {
        val spec = NanoView.MeasureSpec.makeMeasureSpec(0, NanoView.MeasureSpec.UNSPECIFIED)
        assertEquals(NanoView.MeasureSpec.UNSPECIFIED, NanoView.MeasureSpec.getMode(spec))
    }

    @Test
    fun `test toString`() {
        view.id = "my_view"
        val str = view.toString()
        assertTrue(str.contains("TestView"))
        assertTrue(str.contains("my_view"))
    }
}
