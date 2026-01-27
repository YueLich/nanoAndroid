package com.nano.app.intent

import com.nano.kernel.binder.NanoBundle

/**
 * NanoIntent - 意图对象
 *
 * 模拟 Android Intent，用于启动组件和传递数据
 */
class NanoIntent {

    /** 动作 */
    var action: String? = null

    /** 目标组件 - 包名 */
    var packageName: String? = null

    /** 目标组件 - 类名 */
    var className: String? = null

    /** 携带的数据 */
    private val extras: NanoBundle = NanoBundle()

    /** 标志位 */
    var flags: Int = 0

    /**
     * 创建空 Intent
     */
    constructor()

    /**
     * 创建带 action 的 Intent
     */
    constructor(action: String) {
        this.action = action
    }

    /**
     * 创建显式 Intent（指定目标组件）
     */
    constructor(packageName: String, className: String) {
        this.packageName = packageName
        this.className = className
    }

    // ==================== 组件设置 ====================

    /**
     * 设置组件
     */
    fun setComponent(packageName: String, className: String): NanoIntent {
        this.packageName = packageName
        this.className = className
        return this
    }

    /**
     * 获取完整的组件名
     */
    fun getComponent(): String? {
        return if (packageName != null && className != null) {
            "$packageName/$className"
        } else {
            null
        }
    }

    // ==================== 数据操作 ====================

    /**
     * 放入 Int 数据
     */
    fun putExtra(key: String, value: Int): NanoIntent {
        extras.putInt(key, value)
        return this
    }

    /**
     * 放入 Long 数据
     */
    fun putExtra(key: String, value: Long): NanoIntent {
        extras.putLong(key, value)
        return this
    }

    /**
     * 放入 String 数据
     */
    fun putExtra(key: String, value: String): NanoIntent {
        extras.putString(key, value)
        return this
    }

    /**
     * 放入 Boolean 数据
     */
    fun putExtra(key: String, value: Boolean): NanoIntent {
        extras.putBoolean(key, value)
        return this
    }

    /**
     * 放入 Bundle 数据
     */
    fun putExtras(bundle: NanoBundle): NanoIntent {
        // 将 bundle 的所有数据复制到 extras
        extras.putAll(bundle)
        return this
    }

    /**
     * 获取 Int 数据
     */
    fun getIntExtra(key: String, defaultValue: Int = 0): Int {
        return extras.getInt(key, defaultValue)
    }

    /**
     * 获取 Long 数据
     */
    fun getLongExtra(key: String, defaultValue: Long = 0L): Long {
        return extras.getLong(key, defaultValue)
    }

    /**
     * 获取 String 数据
     */
    fun getStringExtra(key: String): String? {
        return extras.getString(key)
    }

    /**
     * 获取 Boolean 数据
     */
    fun getBooleanExtra(key: String, defaultValue: Boolean = false): Boolean {
        return extras.getBoolean(key, defaultValue)
    }

    /**
     * 获取所有 extras
     */
    fun getExtras(): NanoBundle {
        return extras
    }

    /**
     * 检查是否包含某个 key
     */
    fun hasExtra(key: String): Boolean {
        return extras.containsKey(key)
    }

    /**
     * 移除某个 key
     */
    fun removeExtra(key: String) {
        extras.remove(key)
    }

    // ==================== 标志位操作 ====================

    /**
     * 添加标志
     */
    fun addFlags(flags: Int): NanoIntent {
        this.flags = this.flags or flags
        return this
    }

    /**
     * 设置标志
     */
    fun setFlags(flags: Int): NanoIntent {
        this.flags = flags
        return this
    }

    /**
     * 检查是否有指定标志
     */
    fun hasFlag(flag: Int): Boolean {
        return (flags and flag) == flag
    }

    // ==================== 工具方法 ====================

    /**
     * 克隆 Intent
     */
    fun clone(): NanoIntent {
        val cloned = NanoIntent()
        cloned.action = this.action
        cloned.packageName = this.packageName
        cloned.className = this.className
        cloned.flags = this.flags
        cloned.putExtras(this.extras)
        return cloned
    }

    override fun toString(): String {
        val component = getComponent()
        return "NanoIntent(action=$action, component=$component, flags=0x${flags.toString(16)})"
    }

    companion object {
        // 常用 Action
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"

        // 常用 Flag
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        const val FLAG_ACTIVITY_SINGLE_TOP = 0x20000000
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000
    }
}
