package com.nano.view

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * NanoViewGroup 单元测试
 */
class NanoViewGroupTest {

    private lateinit var viewGroup: TestViewGroup
    private lateinit var child1: TestView
    private lateinit var child2: TestView
    private lateinit var child3: TestView

    class TestViewGroup : NanoViewGroup() {
        var onLayoutCalled = false
        var onDrawCalled = false

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            onLayoutCalled = true
        }

        override fun onDraw() {
            onDrawCalled = true
            super.onDraw()
        }
    }

    class TestView : NanoView() {
        var drawCalled = false

        override fun onDraw() {
            drawCalled = true
        }
    }

    @Before
    fun setUp() {
        viewGroup = TestViewGroup()
        child1 = TestView().apply { id = "child1" }
        child2 = TestView().apply { id = "child2" }
        child3 = TestView().apply { id = "child3" }
    }

    @Test
    fun `test initial state`() {
        assertEquals(0, viewGroup.getChildCount())
    }

    @Test
    fun `test add view`() {
        viewGroup.addView(child1)

        assertEquals(1, viewGroup.getChildCount())
        assertSame(child1, viewGroup.getChildAt(0))
        assertSame(viewGroup, child1.parent)
    }

    @Test
    fun `test add multiple views`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)
        viewGroup.addView(child3)

        assertEquals(3, viewGroup.getChildCount())
        assertSame(child1, viewGroup.getChildAt(0))
        assertSame(child2, viewGroup.getChildAt(1))
        assertSame(child3, viewGroup.getChildAt(2))
    }

    @Test
    fun `test add view with index`() {
        viewGroup.addView(child1)
        viewGroup.addView(child3)
        viewGroup.addView(child2, 1)

        assertEquals(3, viewGroup.getChildCount())
        assertSame(child1, viewGroup.getChildAt(0))
        assertSame(child2, viewGroup.getChildAt(1))
        assertSame(child3, viewGroup.getChildAt(2))
    }

    @Test
    fun `test add view that already has parent throws exception`() {
        viewGroup.addView(child1)

        try {
            val anotherGroup = TestViewGroup()
            anotherGroup.addView(child1)
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("already has a parent") == true)
        }
    }

    @Test
    fun `test remove view`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)

        viewGroup.removeView(child1)

        assertEquals(1, viewGroup.getChildCount())
        assertSame(child2, viewGroup.getChildAt(0))
        assertNull(child1.parent)
    }

    @Test
    fun `test remove view at index`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)
        viewGroup.addView(child3)

        viewGroup.removeViewAt(1)

        assertEquals(2, viewGroup.getChildCount())
        assertSame(child1, viewGroup.getChildAt(0))
        assertSame(child3, viewGroup.getChildAt(1))
        assertNull(child2.parent)
    }

    @Test
    fun `test remove all views`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)
        viewGroup.addView(child3)

        viewGroup.removeAllViews()

        assertEquals(0, viewGroup.getChildCount())
        assertNull(child1.parent)
        assertNull(child2.parent)
        assertNull(child3.parent)
    }

    @Test
    fun `test get child at invalid index throws exception`() {
        viewGroup.addView(child1)

        try {
            viewGroup.getChildAt(-1)
            fail("Should throw IndexOutOfBoundsException")
        } catch (e: IndexOutOfBoundsException) {
            // Expected
        }

        try {
            viewGroup.getChildAt(1)
            fail("Should throw IndexOutOfBoundsException")
        } catch (e: IndexOutOfBoundsException) {
            // Expected
        }
    }

    @Test
    fun `test index of child`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)
        viewGroup.addView(child3)

        assertEquals(0, viewGroup.indexOfChild(child1))
        assertEquals(1, viewGroup.indexOfChild(child2))
        assertEquals(2, viewGroup.indexOfChild(child3))

        val notChild = TestView()
        assertEquals(-1, viewGroup.indexOfChild(notChild))
    }

    @Test
    fun `test measure children`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(300, NanoView.MeasureSpec.EXACTLY)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(400, NanoView.MeasureSpec.EXACTLY)

        viewGroup.measure(widthSpec, heightSpec)

        assertEquals(300, viewGroup.measuredWidth)
        assertEquals(400, viewGroup.measuredHeight)
        assertTrue(child1.measuredWidth > 0 || child1.measuredHeight > 0)
        assertTrue(child2.measuredWidth > 0 || child2.measuredHeight > 0)
    }

    @Test
    fun `test draw children`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)

        child1.visibility = NanoView.VISIBLE
        child2.visibility = NanoView.VISIBLE

        viewGroup.draw()

        assertTrue(viewGroup.onDrawCalled)
        assertTrue(child1.drawCalled)
        assertTrue(child2.drawCalled)
    }

    @Test
    fun `test draw skips invisible children`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)

        child1.visibility = NanoView.VISIBLE
        child2.visibility = NanoView.INVISIBLE

        viewGroup.draw()

        assertTrue(child1.drawCalled)
        assertFalse(child2.drawCalled)
    }

    @Test
    fun `test touch event dispatch to children`() {
        viewGroup.addView(child1)
        viewGroup.addView(child2)

        child1.layout(0, 0, 100, 100)
        child2.layout(100, 0, 200, 100)

        var child1Clicked = false
        var child2Clicked = false

        child1.setOnClickListener { child1Clicked = true }
        child2.setOnClickListener { child2Clicked = true }

        viewGroup.layout(0, 0, 200, 100)

        // 点击 child1
        viewGroup.onTouchEvent(50, 50)
        assertTrue(child1Clicked)
        assertFalse(child2Clicked)

        child1Clicked = false

        // 点击 child2
        viewGroup.onTouchEvent(150, 50)
        assertFalse(child1Clicked)
        assertTrue(child2Clicked)
    }

    @Test
    fun `test find view by id recursively`() {
        viewGroup.id = "root"
        child1.id = "child1"
        child2.id = "child2"

        viewGroup.addView(child1)
        viewGroup.addView(child2)

        assertSame(viewGroup, viewGroup.findViewById("root"))
        assertSame(child1, viewGroup.findViewById("child1"))
        assertSame(child2, viewGroup.findViewById("child2"))
        assertNull(viewGroup.findViewById("not_found"))
    }

    @Test
    fun `test find view by id in nested hierarchy`() {
        val nested = TestViewGroup().apply { id = "nested" }
        val deepChild = TestView().apply { id = "deep" }

        viewGroup.addView(child1)
        viewGroup.addView(nested)
        nested.addView(deepChild)

        assertSame(deepChild, viewGroup.findViewById("deep"))
        assertSame(nested, viewGroup.findViewById("nested"))
    }

    @Test
    fun `test child from parent utility`() {
        viewGroup.addView(child1)
        child1.removeFromParent()

        assertEquals(0, viewGroup.getChildCount())
        assertNull(child1.parent)
    }
}
