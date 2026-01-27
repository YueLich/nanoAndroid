package com.nano.app.context

import android.content.Context
import com.nano.app.intent.NanoIntent
import com.nano.kernel.handler.NanoLooper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * NanoContext 单元测试
 */
class NanoContextTest {

    private lateinit var androidContext: Context
    private lateinit var nanoContext: TestContext

    /**
     * 测试用的 Context 实现
     */
    class TestContext(androidContext: Context) : NanoContext(androidContext) {
        var startActivityFailedCalled = false
        var lastFailedIntent: NanoIntent? = null

        override fun onStartActivityFailed(intent: NanoIntent) {
            startActivityFailedCalled = true
            lastFailedIntent = intent
        }

        fun reset() {
            startActivityFailedCalled = false
            lastFailedIntent = null
        }
    }

    @Before
    fun setUp() {
        // 准备主 Looper
        try {
            NanoLooper.prepareMainLooper()
        } catch (e: Exception) {
            // 已经准备好了，忽略
        }

        androidContext = mock(Context::class.java)
        `when`(androidContext.packageName).thenReturn("com.test.app")
        nanoContext = TestContext(androidContext)
    }

    @Test
    fun `test get android context`() {
        assertEquals(androidContext, nanoContext.androidContext)
    }

    @Test
    fun `test get package name`() {
        assertEquals("com.test.app", nanoContext.getPackageName())
    }

    @Test
    fun `test get main handler`() {
        val handler = nanoContext.mainHandler
        assertNotNull(handler)
    }

    @Test
    fun `test system service names`() {
        assertEquals("activity", NanoContext.ACTIVITY_SERVICE)
        assertEquals("window", NanoContext.WINDOW_SERVICE)
        assertEquals("package", NanoContext.PACKAGE_SERVICE)
        assertEquals("llm", NanoContext.LLM_SERVICE)
    }

    @Test
    fun `test get system service returns null when not available`() {
        // 在单元测试环境中，系统服务不可用
        val activityManager = nanoContext.getActivityManager()
        assertNull(activityManager)

        val windowManager = nanoContext.getWindowManager()
        assertNull(windowManager)

        val packageManager = nanoContext.getPackageManager()
        assertNull(packageManager)
    }

    @Test
    fun `test start activity with incomplete intent throws exception`() {
        val intent = NanoIntent()
        // 没有设置 packageName 和 className

        try {
            nanoContext.startActivity(intent)
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("must specify component") == true)
        }
    }

    @Test
    fun `test start activity with only package name throws exception`() {
        val intent = NanoIntent()
        intent.packageName = "com.test.app"
        // className 为 null

        try {
            nanoContext.startActivity(intent)
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("must specify component") == true)
        }
    }

    @Test
    fun `test start activity with only class name throws exception`() {
        val intent = NanoIntent()
        intent.className = "MainActivity"
        // packageName 为 null

        try {
            nanoContext.startActivity(intent)
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("must specify component") == true)
        }
    }

    @Test
    fun `test run on main thread`() {
        var executed = false
        nanoContext.runOnMainThread {
            executed = true
        }
        // 注意：在单元测试中，Handler 可能不会立即执行
        // 这里只验证方法调用不抛异常
    }

    @Test
    fun `test post delayed`() {
        var executed = false
        nanoContext.postDelayed({
            executed = true
        }, 100)
        // 注意：在单元测试中，Handler 可能不会立即执行
        // 这里只验证方法调用不抛异常
    }
}
