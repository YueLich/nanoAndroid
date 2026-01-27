package com.nano.kernel.binder

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * NanoServiceManager 单元测试
 */
class NanoServiceManagerTest {

    // 测试用的简单 Binder 实现
    class TestBinder(private val name: String) : NanoBinder() {
        override fun onTransact(
            code: Int,
            data: NanoParcel,
            reply: NanoParcel?,
            flags: Int
        ): Boolean {
            return true
        }

        override fun toString(): String = "TestBinder($name)"
    }

    @Before
    fun setUp() {
        // 清理之前的状态
        NanoServiceManager.clearAllServices()
    }

    @After
    fun tearDown() {
        NanoServiceManager.clearAllServices()
    }

    @Test
    fun `test addService and getService`() {
        val testBinder = TestBinder("test")

        NanoServiceManager.addService("test_service", testBinder)

        val retrieved = NanoServiceManager.getService("test_service")
        assertNotNull(retrieved)
        assertEquals(testBinder, retrieved)
    }

    @Test
    fun `test getService returns null for unknown service`() {
        val result = NanoServiceManager.getService("unknown_service")
        assertNull(result)
    }

    @Test
    fun `test checkService`() {
        val testBinder = TestBinder("check")

        assertNull(NanoServiceManager.checkService("check_service"))

        NanoServiceManager.addService("check_service", testBinder)

        assertNotNull(NanoServiceManager.checkService("check_service"))
    }

    @Test
    fun `test listServices`() {
        assertTrue(NanoServiceManager.listServices().isEmpty())

        NanoServiceManager.addService("service1", TestBinder("s1"))
        NanoServiceManager.addService("service2", TestBinder("s2"))
        NanoServiceManager.addService("service3", TestBinder("s3"))

        val services = NanoServiceManager.listServices()
        assertEquals(3, services.size)
        assertTrue(services.contains("service1"))
        assertTrue(services.contains("service2"))
        assertTrue(services.contains("service3"))
    }

    @Test
    fun `test waitForService callback when service already exists`() {
        val testBinder = TestBinder("existing")
        NanoServiceManager.addService("existing_service", testBinder)

        val callbackInvoked = AtomicBoolean(false)
        val receivedBinder = AtomicReference<NanoBinder?>(null)

        NanoServiceManager.waitForService("existing_service") { binder ->
            callbackInvoked.set(true)
            receivedBinder.set(binder)
        }

        assertTrue(callbackInvoked.get())
        assertEquals(testBinder, receivedBinder.get())
    }

    @Test
    fun `test waitForService callback when service added later`() {
        val testBinder = TestBinder("later")
        val latch = CountDownLatch(1)
        val receivedBinder = AtomicReference<NanoBinder?>(null)

        // 先注册等待
        NanoServiceManager.waitForService("later_service") { binder ->
            receivedBinder.set(binder)
            latch.countDown()
        }

        // 服务还未添加，回调不应被调用
        assertNull(receivedBinder.get())

        // 添加服务
        NanoServiceManager.addService("later_service", testBinder)

        // 等待回调
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(testBinder, receivedBinder.get())
    }

    @Test
    fun `test waitForServiceSync returns immediately for existing service`() {
        val testBinder = TestBinder("sync_existing")
        NanoServiceManager.addService("sync_service", testBinder)

        val startTime = System.currentTimeMillis()
        val result = NanoServiceManager.waitForServiceSync("sync_service", 5000)
        val elapsed = System.currentTimeMillis() - startTime

        assertNotNull(result)
        assertEquals(testBinder, result)
        assertTrue("Should return immediately", elapsed < 100)
    }

    @Test
    fun `test waitForServiceSync times out for non-existent service`() {
        val startTime = System.currentTimeMillis()
        val result = NanoServiceManager.waitForServiceSync("non_existent", 500)
        val elapsed = System.currentTimeMillis() - startTime

        assertNull(result)
        assertTrue("Should wait at least 500ms", elapsed >= 450) // 允许一点误差
    }

    @Test
    fun `test waitForServiceSync returns when service added`() {
        val testBinder = TestBinder("sync_later")
        val latch = CountDownLatch(1)
        val resultHolder = AtomicReference<NanoBinder?>(null)

        // 在另一个线程中等待服务
        Thread {
            val result = NanoServiceManager.waitForServiceSync("sync_later_service", 5000)
            resultHolder.set(result)
            latch.countDown()
        }.start()

        // 等待一小段时间确保等待线程已启动
        Thread.sleep(100)

        // 添加服务
        NanoServiceManager.addService("sync_later_service", testBinder)

        // 等待结果
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(testBinder, resultHolder.get())
    }

    @Test
    fun `test multiple services can be registered`() {
        val binder1 = TestBinder("b1")
        val binder2 = TestBinder("b2")
        val binder3 = TestBinder("b3")

        NanoServiceManager.addService("activity", binder1)
        NanoServiceManager.addService("window", binder2)
        NanoServiceManager.addService("package", binder3)

        assertEquals(binder1, NanoServiceManager.getService("activity"))
        assertEquals(binder2, NanoServiceManager.getService("window"))
        assertEquals(binder3, NanoServiceManager.getService("package"))
    }

    @Test
    fun `test service can be overwritten`() {
        val binder1 = TestBinder("v1")
        val binder2 = TestBinder("v2")

        NanoServiceManager.addService("overwrite_test", binder1)
        assertEquals(binder1, NanoServiceManager.getService("overwrite_test"))

        NanoServiceManager.addService("overwrite_test", binder2)
        assertEquals(binder2, NanoServiceManager.getService("overwrite_test"))
    }
}
