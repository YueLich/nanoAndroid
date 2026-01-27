package com.nano.kernel.handler

import com.nano.kernel.binder.NanoBundle
import org.junit.Assert.*
import org.junit.Test

/**
 * NanoMessage 单元测试
 */
class NanoMessageTest {

    @Test
    fun `test obtain returns message`() {
        val msg = NanoMessage.obtain()
        assertNotNull(msg)
        msg.recycle()
    }

    @Test
    fun `test obtain with what`() {
        val msg = NanoMessage.obtain(42)
        assertEquals(42, msg.what)
        msg.recycle()
    }

    @Test
    fun `test message properties`() {
        val msg = NanoMessage.obtain()

        msg.what = 1
        msg.arg1 = 10
        msg.arg2 = 20
        msg.obj = "test object"

        assertEquals(1, msg.what)
        assertEquals(10, msg.arg1)
        assertEquals(20, msg.arg2)
        assertEquals("test object", msg.obj)

        msg.recycle()
    }

    @Test
    fun `test message data`() {
        val msg = NanoMessage.obtain()

        // data property creates bundle if null
        val data = msg.data
        assertNotNull(data)

        data.putInt("key", 42)
        assertEquals(42, msg.data.getInt("key"))

        msg.recycle()
    }

    @Test
    fun `test setData`() {
        val msg = NanoMessage.obtain()

        val bundle = NanoBundle()
        bundle.putString("name", "test")

        msg.setDataOrNull(bundle)

        assertEquals("test", msg.data.getString("name"))

        msg.recycle()
    }

    @Test
    fun `test message reuse after recycle`() {
        val msg1 = NanoMessage.obtain()
        msg1.what = 100
        msg1.arg1 = 200
        msg1.obj = "object"
        msg1.recycle()

        val msg2 = NanoMessage.obtain()
        // After recycle, values should be reset
        assertEquals(0, msg2.what)
        assertEquals(0, msg2.arg1)
        assertNull(msg2.obj)

        msg2.recycle()
    }

    @Test
    fun `test toString`() {
        val msg = NanoMessage.obtain()
        msg.what = 5
        msg.arg1 = 10
        msg.arg2 = 20

        val str = msg.toString()
        assertTrue(str.contains("what=5"))
        assertTrue(str.contains("arg1=10"))
        assertTrue(str.contains("arg2=20"))

        msg.recycle()
    }

    @Test(expected = IllegalStateException::class)
    fun `test cannot recycle message in use`() {
        val msg = NanoMessage.obtain()
        msg.isInUse = true
        msg.recycle() // Should throw
    }
}
