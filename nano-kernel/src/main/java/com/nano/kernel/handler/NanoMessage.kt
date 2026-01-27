package com.nano.kernel.handler

import com.nano.kernel.binder.NanoBundle
import java.util.ArrayDeque

/**
 * NanoMessage - 消息对象
 *
 * 模拟 Android Message，用于在 Handler 和 Looper 之间传递消息。
 *
 * 消息包含：
 * - what: 消息类型标识
 * - arg1, arg2: 简单的整数参数
 * - obj: 任意对象参数
 * - data: Bundle 类型的数据
 * - callback: 可运行的回调（用于 post(Runnable)）
 */
class NanoMessage private constructor() {

    companion object {
        private const val MAX_POOL_SIZE = 50
        private val pool = ArrayDeque<NanoMessage>()

        /** 全局序列号（用于保证FIFO顺序） */
        @Volatile
        private var nextSeq = 0L

        /**
         * 从对象池获取消息实例
         */
        fun obtain(): NanoMessage {
            synchronized(pool) {
                val msg = pool.pollFirst()?.apply { reset() } ?: NanoMessage()
                msg.seq = nextSeq++
                return msg
            }
        }

        /**
         * 获取消息实例并设置 what
         */
        fun obtain(what: Int): NanoMessage {
            return obtain().apply { this.what = what }
        }

        /**
         * 获取消息实例并设置 Handler 和 what
         */
        fun obtain(handler: NanoHandler, what: Int): NanoMessage {
            return obtain().apply {
                this.target = handler
                this.what = what
            }
        }

        /**
         * 获取消息实例并设置回调
         */
        fun obtain(handler: NanoHandler, callback: Runnable): NanoMessage {
            return obtain().apply {
                this.target = handler
                this.callback = callback
            }
        }
    }

    /** 消息类型标识 */
    var what: Int = 0

    /** 整数参数1 */
    var arg1: Int = 0

    /** 整数参数2 */
    var arg2: Int = 0

    /** 任意对象参数 */
    var obj: Any? = null

    /** Bundle 数据（私有字段） */
    private var _data: NanoBundle? = null

    /**
     * Bundle 数据属性
     * 访问时自动创建
     */
    var data: NanoBundle
        get() {
            if (_data == null) {
                _data = NanoBundle()
            }
            return _data!!
        }
        set(value) {
            _data = value
        }

    /**
     * 设置可空的 Bundle 数据
     */
    fun setDataOrNull(data: NanoBundle?) {
        _data = data
    }

    /**
     * 获取可空的 Bundle 数据（不自动创建）
     */
    fun getDataOrNull(): NanoBundle? = _data

    /** 目标 Handler */
    internal var target: NanoHandler? = null

    /** 回调（用于 post(Runnable)） */
    internal var callback: Runnable? = null

    /** 执行时间（用于延迟消息） */
    internal var `when`: Long = 0

    /** 序列号（用于保证FIFO顺序） */
    internal var seq: Long = 0

    /** 是否正在使用中 */
    internal var isInUse: Boolean = false

    /**
     * 重置消息状态
     */
    private fun reset() {
        what = 0
        arg1 = 0
        arg2 = 0
        obj = null
        _data = null
        target = null
        callback = null
        `when` = 0
        isInUse = false
    }

    /**
     * 回收消息到对象池
     */
    fun recycle() {
        if (isInUse) {
            throw IllegalStateException("Cannot recycle a message that is still in use")
        }

        synchronized(pool) {
            if (pool.size < MAX_POOL_SIZE) {
                reset()
                pool.addLast(this)
            }
        }
    }

    /**
     * 发送此消息到目标 Handler
     */
    fun sendToTarget() {
        target?.sendMessage(this)
            ?: throw IllegalStateException("Message has no target Handler")
    }

    override fun toString(): String {
        return "NanoMessage(what=$what, arg1=$arg1, arg2=$arg2, obj=$obj, when=$`when`)"
    }
}
