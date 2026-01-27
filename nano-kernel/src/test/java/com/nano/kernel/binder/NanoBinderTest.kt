package com.nano.kernel.binder

import org.junit.Assert.*
import org.junit.Test

/**
 * NanoBinder 单元测试
 */
class NanoBinderTest {

    // 测试用的 Binder 实现
    class TestServiceBinder : NanoBinder() {

        companion object {
            const val DESCRIPTOR = "com.nano.test.ITestService"
            const val TRANSACTION_ADD = FIRST_CALL_TRANSACTION + 1
            const val TRANSACTION_ECHO = FIRST_CALL_TRANSACTION + 2
        }

        var lastTransactionCode: Int = -1
        var addResult: Int = 0

        init {
            attachInterface(this, DESCRIPTOR)
        }

        override fun onTransact(
            code: Int,
            data: NanoParcel,
            reply: NanoParcel?,
            flags: Int
        ): Boolean {
            lastTransactionCode = code

            when (code) {
                TRANSACTION_ADD -> {
                    data.enforceInterface(DESCRIPTOR)
                    val a = data.readInt()
                    val b = data.readInt()
                    addResult = a + b
                    reply?.writeInt(addResult)
                    return true
                }
                TRANSACTION_ECHO -> {
                    data.enforceInterface(DESCRIPTOR)
                    val message = data.readString()
                    reply?.writeString("Echo: $message")
                    return true
                }
            }
            return false
        }
    }

    @Test
    fun `test attachInterface and queryLocalInterface`() {
        val binder = TestServiceBinder()

        // 查询正确的描述符
        val result = binder.queryLocalInterface(TestServiceBinder.DESCRIPTOR)
        assertNotNull(result)
        assertEquals(binder, result)

        // 查询错误的描述符
        val wrongResult = binder.queryLocalInterface("wrong.descriptor")
        assertNull(wrongResult)
    }

    @Test
    fun `test getInterfaceDescriptor`() {
        val binder = TestServiceBinder()
        assertEquals(TestServiceBinder.DESCRIPTOR, binder.getInterfaceDescriptor())
    }

    @Test
    fun `test asBinder returns self`() {
        val binder = TestServiceBinder()
        assertEquals(binder, binder.asBinder())
    }

    @Test
    fun `test isBinderAlive`() {
        val binder = TestServiceBinder()
        assertTrue(binder.isBinderAlive())
    }

    @Test
    fun `test pingBinder`() {
        val binder = TestServiceBinder()
        assertTrue(binder.pingBinder())
    }

    @Test
    fun `test transact ADD operation`() {
        val binder = TestServiceBinder()

        val data = NanoParcel.obtain()
        val reply = NanoParcel.obtain()

        data.writeInterfaceToken(TestServiceBinder.DESCRIPTOR)
        data.writeInt(10)
        data.writeInt(20)

        val result = binder.transact(TestServiceBinder.TRANSACTION_ADD, data, reply, 0)

        assertTrue(result)
        assertEquals(TestServiceBinder.TRANSACTION_ADD, binder.lastTransactionCode)
        assertEquals(30, binder.addResult)

        reply.setDataPosition(0)
        assertEquals(30, reply.readInt())

        data.recycle()
        reply.recycle()
    }

    @Test
    fun `test transact ECHO operation`() {
        val binder = TestServiceBinder()

        val data = NanoParcel.obtain()
        val reply = NanoParcel.obtain()

        data.writeInterfaceToken(TestServiceBinder.DESCRIPTOR)
        data.writeString("Hello World")

        val result = binder.transact(TestServiceBinder.TRANSACTION_ECHO, data, reply, 0)

        assertTrue(result)

        reply.setDataPosition(0)
        assertEquals("Echo: Hello World", reply.readString())

        data.recycle()
        reply.recycle()
    }

    @Test
    fun `test transact returns false for unknown code`() {
        val binder = TestServiceBinder()

        val data = NanoParcel.obtain()
        val reply = NanoParcel.obtain()

        val result = binder.transact(9999, data, reply, 0)

        assertFalse(result)

        data.recycle()
        reply.recycle()
    }

    @Test
    fun `test FLAG_ONEWAY constant`() {
        assertEquals(1, NanoBinder.FLAG_ONEWAY)
    }

    @Test
    fun `test FIRST_CALL_TRANSACTION constant`() {
        assertEquals(1, NanoBinder.FIRST_CALL_TRANSACTION)
    }
}
