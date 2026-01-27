package com.nano.view.widget

import com.nano.view.NanoView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * NanoLinearLayout 单元测试
 */
class NanoLinearLayoutTest {

    private lateinit var layout: NanoLinearLayout
    private lateinit var child1: NanoView
    private lateinit var child2: NanoView
    private lateinit var child3: NanoView

    class SimpleView : NanoView() {
        var fixedWidth: Int = 0
        var fixedHeight: Int = 0

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = if (fixedWidth > 0) fixedWidth else MeasureSpec.getSize(widthMeasureSpec)
            val height = if (fixedHeight > 0) fixedHeight else MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(width, height)
        }
    }

    @Before
    fun setUp() {
        layout = NanoLinearLayout()
        child1 = SimpleView().apply {
            id = "child1"
            (this as SimpleView).fixedWidth = 100
            (this as SimpleView).fixedHeight = 50
        }
        child2 = SimpleView().apply {
            id = "child2"
            (this as SimpleView).fixedWidth = 100
            (this as SimpleView).fixedHeight = 50
        }
        child3 = SimpleView().apply {
            id = "child3"
            (this as SimpleView).fixedWidth = 100
            (this as SimpleView).fixedHeight = 50
        }
    }

    @Test
    fun `test initial state`() {
        assertEquals(NanoLinearLayout.VERTICAL, layout.orientation)
        assertEquals(0, layout.getChildCount())
    }

    @Test
    fun `test set orientation`() {
        layout.orientation = NanoLinearLayout.HORIZONTAL
        assertEquals(NanoLinearLayout.HORIZONTAL, layout.orientation)

        layout.orientation = NanoLinearLayout.VERTICAL
        assertEquals(NanoLinearLayout.VERTICAL, layout.orientation)
    }

    @Test
    fun `test vertical layout measure`() {
        layout.orientation = NanoLinearLayout.VERTICAL
        layout.addView(child1)
        layout.addView(child2)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(200, NanoView.MeasureSpec.AT_MOST)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(300, NanoView.MeasureSpec.AT_MOST)

        layout.measure(widthSpec, heightSpec)

        // 垂直布局：高度累加，宽度取最大
        assertEquals(100, layout.measuredWidth) // max(100, 100)
        assertEquals(100, layout.measuredHeight) // 50 + 50
    }

    @Test
    fun `test horizontal layout measure`() {
        layout.orientation = NanoLinearLayout.HORIZONTAL
        layout.addView(child1)
        layout.addView(child2)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(300, NanoView.MeasureSpec.AT_MOST)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(200, NanoView.MeasureSpec.AT_MOST)

        layout.measure(widthSpec, heightSpec)

        // 水平布局：宽度累加，高度取最大
        assertEquals(200, layout.measuredWidth) // 100 + 100
        assertEquals(50, layout.measuredHeight) // max(50, 50)
    }

    @Test
    fun `test vertical layout with exactly mode`() {
        layout.orientation = NanoLinearLayout.VERTICAL
        layout.addView(child1)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(300, NanoView.MeasureSpec.EXACTLY)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(400, NanoView.MeasureSpec.EXACTLY)

        layout.measure(widthSpec, heightSpec)

        assertEquals(300, layout.measuredWidth)
        assertEquals(400, layout.measuredHeight)
    }

    @Test
    fun `test vertical layout positions children`() {
        layout.orientation = NanoLinearLayout.VERTICAL
        layout.addView(child1)
        layout.addView(child2)
        layout.addView(child3)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(200, NanoView.MeasureSpec.EXACTLY)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(300, NanoView.MeasureSpec.EXACTLY)

        layout.measure(widthSpec, heightSpec)
        layout.layout(0, 0, layout.measuredWidth, layout.measuredHeight)

        // 检查子视图的垂直位置
        assertEquals(0, child1.top)
        assertEquals(50, child1.bottom)

        assertEquals(50, child2.top)
        assertEquals(100, child2.bottom)

        assertEquals(100, child3.top)
        assertEquals(150, child3.bottom)
    }

    @Test
    fun `test horizontal layout positions children`() {
        layout.orientation = NanoLinearLayout.HORIZONTAL
        layout.addView(child1)
        layout.addView(child2)
        layout.addView(child3)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(400, NanoView.MeasureSpec.EXACTLY)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(100, NanoView.MeasureSpec.EXACTLY)

        layout.measure(widthSpec, heightSpec)
        layout.layout(0, 0, layout.measuredWidth, layout.measuredHeight)

        // 检查子视图的水平位置
        assertEquals(0, child1.left)
        assertEquals(100, child1.right)

        assertEquals(100, child2.left)
        assertEquals(200, child2.right)

        assertEquals(200, child3.left)
        assertEquals(300, child3.right)
    }

    @Test
    fun `test layout skips gone children`() {
        layout.orientation = NanoLinearLayout.VERTICAL
        layout.addView(child1)
        layout.addView(child2)
        child2.visibility = NanoView.GONE
        layout.addView(child3)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(200, NanoView.MeasureSpec.AT_MOST)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(300, NanoView.MeasureSpec.AT_MOST)

        layout.measure(widthSpec, heightSpec)

        // child2 是 GONE，不计入高度
        assertEquals(100, layout.measuredHeight) // 50 + 50 (child2 被跳过)
    }

    @Test
    fun `test toString`() {
        layout.id = "my_layout"
        layout.orientation = NanoLinearLayout.VERTICAL
        layout.addView(child1)
        layout.addView(child2)

        val str = layout.toString()
        assertTrue(str.contains("NanoLinearLayout"))
        assertTrue(str.contains("my_layout"))
        assertTrue(str.contains("VERTICAL"))
        assertTrue(str.contains("children=2"))
    }

    @Test
    fun `test change orientation triggers relayout`() {
        layout.addView(child1)
        layout.addView(child2)

        val widthSpec = NanoView.MeasureSpec.makeMeasureSpec(400, NanoView.MeasureSpec.AT_MOST)
        val heightSpec = NanoView.MeasureSpec.makeMeasureSpec(300, NanoView.MeasureSpec.AT_MOST)

        // 垂直布局
        layout.orientation = NanoLinearLayout.VERTICAL
        layout.measure(widthSpec, heightSpec)
        val verticalHeight = layout.measuredHeight
        val verticalWidth = layout.measuredWidth

        // 水平布局
        layout.orientation = NanoLinearLayout.HORIZONTAL
        layout.measure(widthSpec, heightSpec)
        val horizontalHeight = layout.measuredHeight
        val horizontalWidth = layout.measuredWidth

        // 尺寸应该不同
        assertTrue(verticalHeight > horizontalHeight)
        assertTrue(horizontalWidth > verticalWidth)
    }
}
