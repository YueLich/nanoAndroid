package com.nano.kernel.binder

import org.junit.Assert.*
import org.junit.Test

/**
 * NanoParcel 单元测试
 */
class NanoParcelTest {

    @Test
    fun `test obtain returns new instance`() {
        val parcel = NanoParcel.obtain()
        assertNotNull(parcel)
        parcel.recycle()
    }

    @Test
    fun `test write and read int`() {
        val parcel = NanoParcel.obtain()

        parcel.writeInt(42)
        parcel.writeInt(-100)
        parcel.writeInt(Int.MAX_VALUE)

        parcel.setDataPosition(0)

        assertEquals(42, parcel.readInt())
        assertEquals(-100, parcel.readInt())
        assertEquals(Int.MAX_VALUE, parcel.readInt())

        parcel.recycle()
    }

    @Test
    fun `test write and read long`() {
        val parcel = NanoParcel.obtain()

        parcel.writeLong(123456789L)
        parcel.writeLong(Long.MAX_VALUE)

        parcel.setDataPosition(0)

        assertEquals(123456789L, parcel.readLong())
        assertEquals(Long.MAX_VALUE, parcel.readLong())

        parcel.recycle()
    }

    @Test
    fun `test write and read string`() {
        val parcel = NanoParcel.obtain()

        parcel.writeString("Hello")
        parcel.writeString("World")
        parcel.writeString(null)
        parcel.writeString("")

        parcel.setDataPosition(0)

        assertEquals("Hello", parcel.readString())
        assertEquals("World", parcel.readString())
        assertNull(parcel.readString())
        assertEquals("", parcel.readString())

        parcel.recycle()
    }

    @Test
    fun `test write and read boolean`() {
        val parcel = NanoParcel.obtain()

        parcel.writeBoolean(true)
        parcel.writeBoolean(false)

        parcel.setDataPosition(0)

        assertTrue(parcel.readBoolean())
        assertFalse(parcel.readBoolean())

        parcel.recycle()
    }

    @Test
    fun `test write and read float`() {
        val parcel = NanoParcel.obtain()

        parcel.writeFloat(3.14f)
        parcel.writeFloat(-2.5f)

        parcel.setDataPosition(0)

        assertEquals(3.14f, parcel.readFloat(), 0.001f)
        assertEquals(-2.5f, parcel.readFloat(), 0.001f)

        parcel.recycle()
    }

    @Test
    fun `test write and read double`() {
        val parcel = NanoParcel.obtain()

        parcel.writeDouble(3.14159265359)
        parcel.writeDouble(-2.71828)

        parcel.setDataPosition(0)

        assertEquals(3.14159265359, parcel.readDouble(), 0.0000001)
        assertEquals(-2.71828, parcel.readDouble(), 0.0001)

        parcel.recycle()
    }

    @Test
    fun `test write and read mixed types`() {
        val parcel = NanoParcel.obtain()

        parcel.writeInt(1)
        parcel.writeString("test")
        parcel.writeBoolean(true)
        parcel.writeLong(999L)

        parcel.setDataPosition(0)

        assertEquals(1, parcel.readInt())
        assertEquals("test", parcel.readString())
        assertTrue(parcel.readBoolean())
        assertEquals(999L, parcel.readLong())

        parcel.recycle()
    }

    @Test
    fun `test interface token enforcement`() {
        val parcel = NanoParcel.obtain()
        val descriptor = "com.nano.test.ITestService"

        parcel.writeInterfaceToken(descriptor)
        parcel.setDataPosition(0)

        // Should not throw
        parcel.enforceInterface(descriptor)

        parcel.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test interface token mismatch throws`() {
        val parcel = NanoParcel.obtain()

        parcel.writeInterfaceToken("com.nano.test.IServiceA")
        parcel.setDataPosition(0)

        // Should throw
        parcel.enforceInterface("com.nano.test.IServiceB")
    }

    @Test
    fun `test data size`() {
        val parcel = NanoParcel.obtain()

        assertEquals(0, parcel.dataSize())

        parcel.writeInt(1)
        parcel.writeString("test")

        assertEquals(2, parcel.dataSize())

        parcel.recycle()
    }

    @Test
    fun `test data position`() {
        val parcel = NanoParcel.obtain()

        parcel.writeInt(1)
        parcel.writeInt(2)
        parcel.writeInt(3)

        assertEquals(0, parcel.dataPosition())

        parcel.readInt()
        assertEquals(1, parcel.dataPosition())

        parcel.readInt()
        assertEquals(2, parcel.dataPosition())

        parcel.setDataPosition(0)
        assertEquals(0, parcel.dataPosition())

        parcel.recycle()
    }

    @Test
    fun `test parcel reuse after recycle`() {
        val parcel1 = NanoParcel.obtain()
        parcel1.writeInt(42)
        parcel1.recycle()

        val parcel2 = NanoParcel.obtain()
        // After recycle, data should be cleared
        assertEquals(0, parcel2.dataSize())

        parcel2.recycle()
    }
}
