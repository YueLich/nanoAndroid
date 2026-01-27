package com.nano.app.activity

import android.content.Context
import com.nano.app.intent.NanoIntent
import com.nano.kernel.handler.NanoLooper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * NanoActivity 单元测试
 */
class NanoActivityTest {

    private lateinit var context: Context
    private lateinit var activity: TestActivity

    /**
     * 测试用的 Activity 实现
     */
    class TestActivity(context: Context) : NanoActivity(context) {
        var onCreateCalled = false
        var onStartCalled = false
        var onResumeCalled = false
        var onPauseCalled = false
        var onStopCalled = false
        var onDestroyCalled = false
        var onNewIntentCalled = false
        var lastNewIntent: NanoIntent? = null

        override fun onCreate() {
            super.onCreate()
            onCreateCalled = true
        }

        override fun onStart() {
            super.onStart()
            onStartCalled = true
        }

        override fun onResume() {
            super.onResume()
            onResumeCalled = true
        }

        override fun onPause() {
            super.onPause()
            onPauseCalled = true
        }

        override fun onStop() {
            super.onStop()
            onStopCalled = true
        }

        override fun onDestroy() {
            super.onDestroy()
            onDestroyCalled = true
        }

        override fun onNewIntent(intent: NanoIntent) {
            super.onNewIntent(intent)
            onNewIntentCalled = true
            lastNewIntent = intent
        }

        fun reset() {
            onCreateCalled = false
            onStartCalled = false
            onResumeCalled = false
            onPauseCalled = false
            onStopCalled = false
            onDestroyCalled = false
            onNewIntentCalled = false
            lastNewIntent = null
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

        context = mock(Context::class.java)
        activity = TestActivity(context)
    }

    @Test
    fun `test initial state`() {
        assertNull(activity.activityToken)
        assertNull(activity.intent)
        assertFalse(activity.isFinishing())
        assertFalse(activity.isDestroyed())
    }

    @Test
    fun `test perform create`() {
        val intent = NanoIntent("com.test.app", "MainActivity")
        activity.performCreate(intent)

        assertTrue(activity.onCreateCalled)
        assertEquals(intent, activity.intent)
        assertEquals(intent, activity.intent)
    }

    @Test
    fun `test perform start`() {
        activity.performStart()
        assertTrue(activity.onStartCalled)
    }

    @Test
    fun `test perform resume`() {
        activity.performResume()
        assertTrue(activity.onResumeCalled)
    }

    @Test
    fun `test perform pause`() {
        activity.performPause()
        assertTrue(activity.onPauseCalled)
    }

    @Test
    fun `test perform stop`() {
        activity.performStop()
        assertTrue(activity.onStopCalled)
    }

    @Test
    fun `test perform destroy`() {
        activity.performDestroy()
        assertTrue(activity.onDestroyCalled)
        assertTrue(activity.isDestroyed())
    }

    @Test
    fun `test perform new intent`() {
        val originalIntent = NanoIntent("com.test.app", "MainActivity")
        activity.performCreate(originalIntent)
        activity.reset()

        val newIntent = NanoIntent("com.test.app", "MainActivity")
        newIntent.putExtra("key", "value")
        activity.performNewIntent(newIntent)

        assertTrue(activity.onNewIntentCalled)
        assertEquals(newIntent, activity.lastNewIntent)
        assertEquals(newIntent, activity.intent)
    }

    @Test
    fun `test full lifecycle`() {
        val intent = NanoIntent("com.test.app", "MainActivity")
        activity.activityToken = "test_token"

        // onCreate -> onStart -> onResume
        activity.performCreate(intent)
        assertTrue(activity.onCreateCalled)

        activity.performStart()
        assertTrue(activity.onStartCalled)

        activity.performResume()
        assertTrue(activity.onResumeCalled)

        // onPause -> onStop -> onDestroy
        activity.performPause()
        assertTrue(activity.onPauseCalled)

        activity.performStop()
        assertTrue(activity.onStopCalled)

        activity.performDestroy()
        assertTrue(activity.onDestroyCalled)
        assertTrue(activity.isDestroyed())
    }

    @Test
    fun `test set intent`() {
        val intent1 = NanoIntent("com.test.app", "MainActivity")
        activity.setIntent(intent1)
        assertEquals(intent1, activity.intent)

        val intent2 = NanoIntent("com.test.app", "SecondActivity")
        activity.setIntent(intent2)
        assertEquals(intent2, activity.intent)
    }

    @Test
    fun `test activity token`() {
        assertNull(activity.activityToken)

        activity.activityToken = "token_123"
        assertEquals("token_123", activity.activityToken)
    }

    @Test
    fun `test finish sets is finishing`() {
        assertFalse(activity.isFinishing())

        activity.activityToken = "test_token"
        activity.finish()

        assertTrue(activity.isFinishing())
    }

    @Test
    fun `test finish without token does not crash`() {
        // Should not throw exception
        activity.finish()
        assertTrue(activity.isFinishing())
    }

    @Test
    fun `test finish when already finishing`() {
        activity.activityToken = "test_token"
        activity.finish()
        assertTrue(activity.isFinishing())

        // 再次 finish 不应该有副作用
        activity.finish()
        assertTrue(activity.isFinishing())
    }

    @Test
    fun `test finish when already destroyed`() {
        activity.performDestroy()
        assertTrue(activity.isDestroyed())

        // 销毁后 finish 不应该有副作用
        activity.finish()
        assertFalse(activity.isFinishing()) // 因为已经销毁，不会设置 finishing
    }

    @Test
    fun `test toString`() {
        val str = activity.toString()
        assertTrue(str.contains("TestActivity"))
        assertTrue(str.contains("@"))
    }

    @Test
    fun `test get android context`() {
        assertEquals(context, activity.androidContext)
    }

    @Test
    fun `test lifecycle callback order matters`() {
        val intent = NanoIntent("com.test.app", "MainActivity")

        // onCreate 必须在其他方法之前
        activity.performCreate(intent)
        assertTrue(activity.onCreateCalled)
        assertFalse(activity.onStartCalled)

        // onStart
        activity.performStart()
        assertTrue(activity.onStartCalled)
        assertFalse(activity.onResumeCalled)

        // onResume
        activity.performResume()
        assertTrue(activity.onResumeCalled)
        assertFalse(activity.onPauseCalled)
    }
}
