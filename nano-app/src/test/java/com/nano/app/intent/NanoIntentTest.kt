package com.nano.app.intent

import com.nano.kernel.binder.NanoBundle
import org.junit.Assert.*
import org.junit.Test

/**
 * NanoIntent 单元测试
 */
class NanoIntentTest {

    @Test
    fun `test create empty intent`() {
        val intent = NanoIntent()
        assertNull(intent.action)
        assertNull(intent.packageName)
        assertNull(intent.className)
        assertEquals(0, intent.flags)
    }

    @Test
    fun `test create intent with action`() {
        val intent = NanoIntent(NanoIntent.ACTION_MAIN)
        assertEquals(NanoIntent.ACTION_MAIN, intent.action)
    }

    @Test
    fun `test create intent with component`() {
        val intent = NanoIntent("com.test.app", "com.test.app.MainActivity")
        assertEquals("com.test.app", intent.packageName)
        assertEquals("com.test.app.MainActivity", intent.className)
    }

    @Test
    fun `test set component`() {
        val intent = NanoIntent()
        intent.setComponent("com.test.app", "com.test.app.MainActivity")
        assertEquals("com.test.app", intent.packageName)
        assertEquals("com.test.app.MainActivity", intent.className)
    }

    @Test
    fun `test get component`() {
        val intent = NanoIntent("com.test.app", "com.test.app.MainActivity")
        assertEquals("com.test.app/com.test.app.MainActivity", intent.getComponent())
    }

    @Test
    fun `test get component returns null when incomplete`() {
        val intent = NanoIntent()
        assertNull(intent.getComponent())

        intent.packageName = "com.test.app"
        assertNull(intent.getComponent())
    }

    @Test
    fun `test put and get int extra`() {
        val intent = NanoIntent()
        intent.putExtra("key1", 123)
        assertEquals(123, intent.getIntExtra("key1"))
        assertEquals(0, intent.getIntExtra("non_existent"))
        assertEquals(99, intent.getIntExtra("non_existent", 99))
    }

    @Test
    fun `test put and get long extra`() {
        val intent = NanoIntent()
        intent.putExtra("key1", 123456789L)
        assertEquals(123456789L, intent.getLongExtra("key1"))
    }

    @Test
    fun `test put and get string extra`() {
        val intent = NanoIntent()
        intent.putExtra("key1", "value1")
        assertEquals("value1", intent.getStringExtra("key1"))
        assertNull(intent.getStringExtra("non_existent"))
    }

    @Test
    fun `test put and get boolean extra`() {
        val intent = NanoIntent()
        intent.putExtra("key1", true)
        assertTrue(intent.getBooleanExtra("key1"))
        assertFalse(intent.getBooleanExtra("non_existent"))
    }

    @Test
    fun `test put extras with bundle`() {
        val bundle = NanoBundle()
        bundle.putInt("int_key", 42)
        bundle.putString("string_key", "hello")

        val intent = NanoIntent()
        intent.putExtras(bundle)

        assertEquals(42, intent.getIntExtra("int_key"))
        assertEquals("hello", intent.getStringExtra("string_key"))
    }

    @Test
    fun `test has extra`() {
        val intent = NanoIntent()
        assertFalse(intent.hasExtra("key1"))

        intent.putExtra("key1", 123)
        assertTrue(intent.hasExtra("key1"))
    }

    @Test
    fun `test remove extra`() {
        val intent = NanoIntent()
        intent.putExtra("key1", 123)
        assertTrue(intent.hasExtra("key1"))

        intent.removeExtra("key1")
        assertFalse(intent.hasExtra("key1"))
    }

    @Test
    fun `test get extras`() {
        val intent = NanoIntent()
        intent.putExtra("key1", 123)
        intent.putExtra("key2", "value")

        val extras = intent.getExtras()
        assertEquals(123, extras.getInt("key1"))
        assertEquals("value", extras.getString("key2"))
    }

    @Test
    fun `test add flags`() {
        val intent = NanoIntent()
        assertEquals(0, intent.flags)

        intent.addFlags(NanoIntent.FLAG_ACTIVITY_NEW_TASK)
        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_NEW_TASK))

        intent.addFlags(NanoIntent.FLAG_ACTIVITY_CLEAR_TOP)
        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    @Test
    fun `test set flags`() {
        val intent = NanoIntent()
        intent.setFlags(NanoIntent.FLAG_ACTIVITY_NEW_TASK)
        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_NEW_TASK))

        intent.setFlags(NanoIntent.FLAG_ACTIVITY_CLEAR_TOP)
        assertFalse(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    @Test
    fun `test has flag`() {
        val intent = NanoIntent()
        intent.addFlags(NanoIntent.FLAG_ACTIVITY_NEW_TASK or NanoIntent.FLAG_ACTIVITY_CLEAR_TOP)

        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_CLEAR_TOP))
        assertFalse(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    @Test
    fun `test clone intent`() {
        val original = NanoIntent("com.test.app", "com.test.app.MainActivity")
        original.action = NanoIntent.ACTION_MAIN
        original.putExtra("key1", 123)
        original.putExtra("key2", "value")
        original.setFlags(NanoIntent.FLAG_ACTIVITY_NEW_TASK)

        val cloned = original.clone()

        assertEquals(original.action, cloned.action)
        assertEquals(original.packageName, cloned.packageName)
        assertEquals(original.className, cloned.className)
        assertEquals(original.flags, cloned.flags)
        assertEquals(123, cloned.getIntExtra("key1"))
        assertEquals("value", cloned.getStringExtra("key2"))

        // 修改 cloned 不影响 original
        cloned.putExtra("key1", 456)
        assertEquals(123, original.getIntExtra("key1"))
        assertEquals(456, cloned.getIntExtra("key1"))
    }

    @Test
    fun `test method chaining`() {
        val intent = NanoIntent()
            .setComponent("com.test.app", "com.test.app.MainActivity")
            .putExtra("key1", 123)
            .putExtra("key2", "value")
            .addFlags(NanoIntent.FLAG_ACTIVITY_NEW_TASK)

        assertEquals("com.test.app", intent.packageName)
        assertEquals(123, intent.getIntExtra("key1"))
        assertTrue(intent.hasFlag(NanoIntent.FLAG_ACTIVITY_NEW_TASK))
    }

    @Test
    fun `test toString`() {
        val intent = NanoIntent("com.test.app", "com.test.app.MainActivity")
        intent.action = NanoIntent.ACTION_MAIN
        intent.setFlags(0x12345678)

        val str = intent.toString()
        assertTrue(str.contains("NanoIntent"))
        assertTrue(str.contains("action=${NanoIntent.ACTION_MAIN}"))
        assertTrue(str.contains("component=com.test.app/com.test.app.MainActivity"))
        assertTrue(str.contains("flags=0x12345678"))
    }

    @Test
    fun `test common actions`() {
        assertEquals("android.intent.action.MAIN", NanoIntent.ACTION_MAIN)
        assertEquals("android.intent.action.VIEW", NanoIntent.ACTION_VIEW)
        assertEquals("android.intent.action.SEND", NanoIntent.ACTION_SEND)
    }

    @Test
    fun `test common flags`() {
        assertEquals(0x10000000, NanoIntent.FLAG_ACTIVITY_NEW_TASK)
        assertEquals(0x04000000, NanoIntent.FLAG_ACTIVITY_CLEAR_TOP)
        assertEquals(0x20000000, NanoIntent.FLAG_ACTIVITY_SINGLE_TOP)
        assertEquals(0x00008000, NanoIntent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
