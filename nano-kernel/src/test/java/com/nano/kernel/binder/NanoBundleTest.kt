package com.nano.kernel.binder

import org.junit.Assert.*
import org.junit.Test

/**
 * NanoBundle 单元测试
 */
class NanoBundleTest {

    @Test
    fun `test put and get int`() {
        val bundle = NanoBundle()

        bundle.putInt("key", 42)
        assertEquals(42, bundle.getInt("key"))
    }

    @Test
    fun `test getInt with default value`() {
        val bundle = NanoBundle()

        assertEquals(0, bundle.getInt("missing"))
        assertEquals(99, bundle.getInt("missing", 99))
    }

    @Test
    fun `test put and get long`() {
        val bundle = NanoBundle()

        bundle.putLong("key", 123456789L)
        assertEquals(123456789L, bundle.getLong("key"))
    }

    @Test
    fun `test put and get float`() {
        val bundle = NanoBundle()

        bundle.putFloat("key", 3.14f)
        assertEquals(3.14f, bundle.getFloat("key"), 0.001f)
    }

    @Test
    fun `test put and get double`() {
        val bundle = NanoBundle()

        bundle.putDouble("key", 3.14159265359)
        assertEquals(3.14159265359, bundle.getDouble("key"), 0.0000001)
    }

    @Test
    fun `test put and get boolean`() {
        val bundle = NanoBundle()

        bundle.putBoolean("true_key", true)
        bundle.putBoolean("false_key", false)

        assertTrue(bundle.getBoolean("true_key"))
        assertFalse(bundle.getBoolean("false_key"))
    }

    @Test
    fun `test put and get string`() {
        val bundle = NanoBundle()

        bundle.putString("key", "Hello World")
        assertEquals("Hello World", bundle.getString("key"))
    }

    @Test
    fun `test getString with null value`() {
        val bundle = NanoBundle()

        bundle.putString("key", null)
        assertNull(bundle.getString("key"))
    }

    @Test
    fun `test getString with default value`() {
        val bundle = NanoBundle()

        assertNull(bundle.getString("missing"))
        assertEquals("default", bundle.getString("missing", "default"))
    }

    @Test
    fun `test containsKey`() {
        val bundle = NanoBundle()

        assertFalse(bundle.containsKey("key"))

        bundle.putInt("key", 1)
        assertTrue(bundle.containsKey("key"))
    }

    @Test
    fun `test remove`() {
        val bundle = NanoBundle()

        bundle.putInt("key", 42)
        assertTrue(bundle.containsKey("key"))

        bundle.remove("key")
        assertFalse(bundle.containsKey("key"))
    }

    @Test
    fun `test clear`() {
        val bundle = NanoBundle()

        bundle.putInt("key1", 1)
        bundle.putString("key2", "value")
        bundle.putBoolean("key3", true)

        assertFalse(bundle.isEmpty())

        bundle.clear()

        assertTrue(bundle.isEmpty())
        assertFalse(bundle.containsKey("key1"))
        assertFalse(bundle.containsKey("key2"))
        assertFalse(bundle.containsKey("key3"))
    }

    @Test
    fun `test isEmpty`() {
        val bundle = NanoBundle()

        assertTrue(bundle.isEmpty())

        bundle.putInt("key", 1)
        assertFalse(bundle.isEmpty())

        bundle.remove("key")
        assertTrue(bundle.isEmpty())
    }

    @Test
    fun `test keySet`() {
        val bundle = NanoBundle()

        bundle.putInt("key1", 1)
        bundle.putString("key2", "value")
        bundle.putBoolean("key3", true)

        val keys = bundle.keySet()
        assertEquals(3, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
        assertTrue(keys.contains("key3"))
    }

    @Test
    fun `test copy creates independent copy`() {
        val original = NanoBundle()
        original.putInt("key", 42)
        original.putString("name", "test")

        val copy = original.copy()

        // 验证复制的内容相同
        assertEquals(42, copy.getInt("key"))
        assertEquals("test", copy.getString("name"))

        // 修改原始不影响复制
        original.putInt("key", 100)
        assertEquals(42, copy.getInt("key"))

        // 修改复制不影响原始
        copy.putString("name", "modified")
        assertEquals("test", original.getString("name"))
    }

    @Test
    fun `test toString`() {
        val bundle = NanoBundle()
        bundle.putInt("key", 42)

        val str = bundle.toString()
        assertTrue(str.contains("NanoBundle"))
        assertTrue(str.contains("key"))
        assertTrue(str.contains("42"))
    }

    @Test
    fun `test multiple types in same bundle`() {
        val bundle = NanoBundle()

        bundle.putInt("int", 1)
        bundle.putLong("long", 2L)
        bundle.putFloat("float", 3.0f)
        bundle.putDouble("double", 4.0)
        bundle.putBoolean("bool", true)
        bundle.putString("string", "five")

        assertEquals(1, bundle.getInt("int"))
        assertEquals(2L, bundle.getLong("long"))
        assertEquals(3.0f, bundle.getFloat("float"), 0.001f)
        assertEquals(4.0, bundle.getDouble("double"), 0.001)
        assertTrue(bundle.getBoolean("bool"))
        assertEquals("five", bundle.getString("string"))
    }

    @Test
    fun `test overwrite value`() {
        val bundle = NanoBundle()

        bundle.putInt("key", 1)
        assertEquals(1, bundle.getInt("key"))

        bundle.putInt("key", 2)
        assertEquals(2, bundle.getInt("key"))
    }
}
