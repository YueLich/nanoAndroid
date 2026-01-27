package com.nano.kernel.handler

import com.nano.kernel.NanoLog

/**
 * NanoHandler - 消息处理器
 *
 * 模拟 Android Handler，用于在特定线程上执行任务。
 *
 * 主要功能：
 * 1. 发送消息到 MessageQueue
 * 2. 处理从 MessageQueue 取出的消息
 * 3. 支持延迟执行和定时执行
 */
open class NanoHandler {

    companion object {
        private const val TAG = "NanoHandler"
    }

    /** 关联的 Looper */
    val looper: NanoLooper

    /** 消息回调接口 */
    private val callback: Callback?

    /**
     * 创建与当前线程 Looper 关联的 Handler
     */
    constructor() : this(
        NanoLooper.myLooper() ?: throw RuntimeException(
            "Can't create handler inside thread ${Thread.currentThread()} that has not called Looper.prepare()"
        )
    )

    /**
     * 创建与指定 Looper 关联的 Handler
     */
    constructor(looper: NanoLooper, callback: Callback? = null) {
        this.looper = looper
        this.callback = callback
    }

    /**
     * 消息回调接口
     */
    fun interface Callback {
        /**
         * 处理消息
         * @return true 表示消息已处理，不再调用 handleMessage
         */
        fun handleMessage(msg: NanoMessage): Boolean
    }

    /**
     * 处理消息
     *
     * 子类可以重写此方法来处理消息
     */
    open fun handleMessage(msg: NanoMessage) {
        // 默认实现为空
    }

    /**
     * 分发消息
     *
     * 由 Looper 调用，按优先级分发：
     * 1. 消息自带的回调
     * 2. Handler 的回调接口
     * 3. handleMessage 方法
     */
    fun dispatchMessage(msg: NanoMessage) {
        // 优先执行消息自带的回调（用于 post(Runnable)）
        msg.callback?.let {
            it.run()
            return
        }

        // 其次执行 Handler 的回调接口
        callback?.let {
            if (it.handleMessage(msg)) {
                return
            }
        }

        // 最后调用 handleMessage
        handleMessage(msg)
    }

    // ==================== 发送消息 ====================

    /**
     * 发送消息
     */
    fun sendMessage(msg: NanoMessage): Boolean {
        return sendMessageDelayed(msg, 0)
    }

    /**
     * 发送空消息
     */
    fun sendEmptyMessage(what: Int): Boolean {
        return sendEmptyMessageDelayed(what, 0)
    }

    /**
     * 发送延迟消息
     */
    fun sendMessageDelayed(msg: NanoMessage, delayMillis: Long): Boolean {
        if (delayMillis < 0) {
            throw IllegalArgumentException("delayMillis must be >= 0")
        }
        return sendMessageAtTime(msg, System.currentTimeMillis() + delayMillis)
    }

    /**
     * 发送延迟空消息
     */
    fun sendEmptyMessageDelayed(what: Int, delayMillis: Long): Boolean {
        val msg = NanoMessage.obtain(this, what)
        return sendMessageDelayed(msg, delayMillis)
    }

    /**
     * 在指定时间发送消息
     */
    fun sendMessageAtTime(msg: NanoMessage, uptimeMillis: Long): Boolean {
        msg.target = this
        return looper.queue.enqueueMessage(msg, uptimeMillis)
    }

    /**
     * 发送到队列头部（立即执行）
     */
    fun sendMessageAtFrontOfQueue(msg: NanoMessage): Boolean {
        msg.target = this
        return looper.queue.enqueueMessage(msg, 0)
    }

    // ==================== Post Runnable ====================

    /**
     * 投递 Runnable 到队列
     */
    fun post(r: Runnable): Boolean {
        return sendMessageDelayed(getPostMessage(r), 0)
    }

    /**
     * 延迟投递 Runnable
     */
    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        return sendMessageDelayed(getPostMessage(r), delayMillis)
    }

    /**
     * 在指定时间投递 Runnable
     */
    fun postAtTime(r: Runnable, uptimeMillis: Long): Boolean {
        return sendMessageAtTime(getPostMessage(r), uptimeMillis)
    }

    /**
     * 投递到队列头部
     */
    fun postAtFrontOfQueue(r: Runnable): Boolean {
        return sendMessageAtFrontOfQueue(getPostMessage(r))
    }

    /**
     * 将 Runnable 包装为 Message
     */
    private fun getPostMessage(r: Runnable): NanoMessage {
        return NanoMessage.obtain(this, r)
    }

    // ==================== 移除消息 ====================

    /**
     * 移除指定 what 的消息
     */
    fun removeMessages(what: Int) {
        looper.queue.removeMessages(this, what)
    }

    /**
     * 移除指定 Runnable
     */
    fun removeCallbacks(r: Runnable) {
        looper.queue.removeCallbacks(this, r)
    }

    /**
     * 检查是否有指定 what 的消息
     */
    fun hasMessages(what: Int): Boolean {
        return looper.queue.hasMessages(this, what)
    }

    // ==================== 工具方法 ====================

    /**
     * 获取新消息
     */
    fun obtainMessage(): NanoMessage {
        return NanoMessage.obtain(this, 0)
    }

    /**
     * 获取新消息并设置 what
     */
    fun obtainMessage(what: Int): NanoMessage {
        return NanoMessage.obtain(this, what)
    }

    /**
     * 获取新消息并设置 what 和参数
     */
    fun obtainMessage(what: Int, arg1: Int, arg2: Int): NanoMessage {
        return NanoMessage.obtain(this, what).apply {
            this.arg1 = arg1
            this.arg2 = arg2
        }
    }

    /**
     * 获取新消息并设置 what、参数和对象
     */
    fun obtainMessage(what: Int, arg1: Int, arg2: Int, obj: Any?): NanoMessage {
        return NanoMessage.obtain(this, what).apply {
            this.arg1 = arg1
            this.arg2 = arg2
            this.obj = obj
        }
    }

    override fun toString(): String {
        return "NanoHandler(${looper.thread.name})"
    }
}

/**
 * 在主线程上创建 Handler
 */
fun mainHandler(callback: NanoHandler.Callback? = null): NanoHandler {
    return NanoHandler(NanoLooper.getMainLooper(), callback)
}
