package com.nano.kernel.handler

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * NanoHandler 单元测试
 */
class NanoHandlerTest {

    private lateinit var looperThread: Thread
    private lateinit var handler: NanoHandler
    private val looperReady = CountDownLatch(1)

    @Before
    fun setUp() {
        // 创建一个带有 Looper 的线程
        looperThread = Thread {
            NanoLooper.prepare()
            handler = NanoHandler()
            looperReady.countDown()
            NanoLooper.loop()
        }
        looperThread.start()

        // 等待 Looper 准备就绪
        assertTrue("Looper should be ready", looperReady.await(2, TimeUnit.SECONDS))
    }

    @After
    fun tearDown() {
        // 退出 Looper
        handler.looper.quit()
        looperThread.join(1000)
    }

    @Test
    fun `test post executes runnable`() {
        val executed = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        handler.post {
            executed.set(true)
            latch.countDown()
        }

        assertTrue("Runnable should execute", latch.await(1, TimeUnit.SECONDS))
        assertTrue(executed.get())
    }

    @Test
    fun `test postDelayed executes after delay`() {
        val executed = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        val startTime = System.currentTimeMillis()

        handler.postDelayed({
            executed.set(true)
            latch.countDown()
        }, 200)

        assertTrue("Runnable should execute", latch.await(1, TimeUnit.SECONDS))
        assertTrue(executed.get())

        val elapsed = System.currentTimeMillis() - startTime
        assertTrue("Should delay at least 200ms, actual: $elapsed", elapsed >= 180)
    }

    @Test
    fun `test sendMessage delivers to handleMessage`() {
        val receivedWhat = AtomicInteger(-1)
        val latch = CountDownLatch(1)

        // 使用回调接收消息
        val testHandler = object : NanoHandler(handler.looper) {
            override fun handleMessage(msg: NanoMessage) {
                receivedWhat.set(msg.what)
                latch.countDown()
            }
        }

        testHandler.sendEmptyMessage(42)

        assertTrue("Message should be delivered", latch.await(1, TimeUnit.SECONDS))
        assertEquals(42, receivedWhat.get())
    }

    @Test
    fun `test sendMessage with arguments`() {
        val receivedArg1 = AtomicInteger(-1)
        val receivedArg2 = AtomicInteger(-1)
        val receivedObj = AtomicReference<Any?>(null)
        val latch = CountDownLatch(1)

        val testHandler = object : NanoHandler(handler.looper) {
            override fun handleMessage(msg: NanoMessage) {
                receivedArg1.set(msg.arg1)
                receivedArg2.set(msg.arg2)
                receivedObj.set(msg.obj)
                latch.countDown()
            }
        }

        val msg = testHandler.obtainMessage(1, 10, 20, "test object")
        testHandler.sendMessage(msg)

        assertTrue("Message should be delivered", latch.await(1, TimeUnit.SECONDS))
        assertEquals(10, receivedArg1.get())
        assertEquals(20, receivedArg2.get())
        assertEquals("test object", receivedObj.get())
    }

    @Test
    fun `test callback takes priority over handleMessage`() {
        val callbackCalled = AtomicBoolean(false)
        val handleMessageCalled = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        val callback = NanoHandler.Callback { msg ->
            callbackCalled.set(true)
            latch.countDown()
            true // 返回 true 表示已处理，不调用 handleMessage
        }

        val testHandler = object : NanoHandler(handler.looper, callback) {
            override fun handleMessage(msg: NanoMessage) {
                handleMessageCalled.set(true)
            }
        }

        testHandler.sendEmptyMessage(1)

        assertTrue("Callback should be called", latch.await(1, TimeUnit.SECONDS))
        assertTrue(callbackCalled.get())
        assertFalse(handleMessageCalled.get())
    }

    @Test
    fun `test callback returns false allows handleMessage`() {
        val callbackCalled = AtomicBoolean(false)
        val handleMessageCalled = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        val callback = NanoHandler.Callback { msg ->
            callbackCalled.set(true)
            false // 返回 false 允许继续调用 handleMessage
        }

        val testHandler = object : NanoHandler(handler.looper, callback) {
            override fun handleMessage(msg: NanoMessage) {
                handleMessageCalled.set(true)
                latch.countDown()
            }
        }

        testHandler.sendEmptyMessage(1)

        assertTrue("handleMessage should be called", latch.await(1, TimeUnit.SECONDS))
        assertTrue(callbackCalled.get())
        assertTrue(handleMessageCalled.get())
    }

    @Test
    fun `test removeMessages`() {
        val messageCount = AtomicInteger(0)
        val latch = CountDownLatch(1)

        val testHandler = object : NanoHandler(handler.looper) {
            override fun handleMessage(msg: NanoMessage) {
                if (msg.what == 1) {
                    messageCount.incrementAndGet()
                } else if (msg.what == 2) {
                    latch.countDown()
                }
            }
        }

        // 发送多个延迟消息
        testHandler.sendEmptyMessageDelayed(1, 100)
        testHandler.sendEmptyMessageDelayed(1, 150)
        testHandler.sendEmptyMessageDelayed(1, 200)
        testHandler.sendEmptyMessageDelayed(2, 250) // 用于检测结束

        // 立即移除所有 what=1 的消息
        testHandler.removeMessages(1)

        // 等待
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        // what=1 的消息应该都被移除了
        assertEquals(0, messageCount.get())
    }

    @Test
    fun `test removeCallbacks`() {
        val executed = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        val runnable = Runnable {
            executed.set(true)
        }

        handler.postDelayed(runnable, 200)
        handler.removeCallbacks(runnable)

        // 发送一个标记来检测时间是否已过
        handler.postDelayed({
            latch.countDown()
        }, 300)

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertFalse("Runnable should have been removed", executed.get())
    }

    @Test
    fun `test hasMessages`() {
        val testHandler = object : NanoHandler(handler.looper) {
            override fun handleMessage(msg: NanoMessage) {}
        }

        assertFalse(testHandler.hasMessages(42))

        testHandler.sendEmptyMessageDelayed(42, 1000)
        assertTrue(testHandler.hasMessages(42))

        testHandler.removeMessages(42)
        assertFalse(testHandler.hasMessages(42))
    }

    @Test
    fun `test obtainMessage`() {
        val msg1 = handler.obtainMessage()
        assertEquals(0, msg1.what)

        val msg2 = handler.obtainMessage(5)
        assertEquals(5, msg2.what)

        val msg3 = handler.obtainMessage(6, 10, 20)
        assertEquals(6, msg3.what)
        assertEquals(10, msg3.arg1)
        assertEquals(20, msg3.arg2)

        val msg4 = handler.obtainMessage(7, 11, 21, "obj")
        assertEquals(7, msg4.what)
        assertEquals(11, msg4.arg1)
        assertEquals(21, msg4.arg2)
        assertEquals("obj", msg4.obj)
    }

    @Test
    fun `test message sendToTarget`() {
        val receivedWhat = AtomicInteger(-1)
        val latch = CountDownLatch(1)

        val testHandler = object : NanoHandler(handler.looper) {
            override fun handleMessage(msg: NanoMessage) {
                receivedWhat.set(msg.what)
                latch.countDown()
            }
        }

        val msg = testHandler.obtainMessage(99)
        msg.sendToTarget()

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(99, receivedWhat.get())
    }

    @Test
    fun `test multiple messages in order`() {
        val results = mutableListOf<Int>()
        val latch = CountDownLatch(3)

        val testHandler = object : NanoHandler(handler.looper) {
            override fun handleMessage(msg: NanoMessage) {
                synchronized(results) {
                    results.add(msg.what)
                }
                latch.countDown()
            }
        }

        testHandler.sendEmptyMessage(1)
        testHandler.sendEmptyMessage(2)
        testHandler.sendEmptyMessage(3)

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        synchronized(results) {
            assertEquals(listOf(1, 2, 3), results)
        }
    }
}
