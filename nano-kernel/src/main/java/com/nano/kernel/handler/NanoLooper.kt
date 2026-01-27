package com.nano.kernel.handler

import com.nano.kernel.NanoLog
import java.util.PriorityQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * NanoLooper - 消息循环
 *
 * 模拟 Android Looper，负责消息的循环处理。
 *
 * 每个线程最多只能有一个 Looper，通过 ThreadLocal 存储。
 * Looper 从 MessageQueue 中取出消息，分发给对应的 Handler 处理。
 *
 * 使用方式：
 * 1. Looper.prepare() - 创建 Looper
 * 2. 创建 Handler
 * 3. Looper.loop() - 开始消息循环
 *
 * 或使用 prepareMainLooper() 创建主线程 Looper
 */
class NanoLooper private constructor() {

    companion object {
        private const val TAG = "NanoLooper"

        /** 线程本地存储 */
        private val threadLocal = ThreadLocal<NanoLooper>()

        /** 主线程 Looper */
        @Volatile
        private var mainLooper: NanoLooper? = null

        /**
         * 为当前线程创建 Looper
         */
        fun prepare() {
            if (threadLocal.get() != null) {
                throw RuntimeException("Only one Looper may be created per thread")
            }
            threadLocal.set(NanoLooper())
        }

        /**
         * 为主线程创建 Looper
         */
        fun prepareMainLooper() {
            prepare()
            synchronized(NanoLooper::class.java) {
                if (mainLooper != null) {
                    throw IllegalStateException("Main looper has already been prepared")
                }
                mainLooper = myLooper()
            }
        }

        /**
         * 获取当前线程的 Looper
         */
        fun myLooper(): NanoLooper? = threadLocal.get()

        /**
         * 获取主线程的 Looper
         */
        fun getMainLooper(): NanoLooper =
            mainLooper ?: throw IllegalStateException("Main looper not prepared")

        /**
         * 开始消息循环
         *
         * 这是一个阻塞方法，会一直运行直到 quit() 被调用
         */
        fun loop() {
            val looper = myLooper()
                ?: throw RuntimeException("No Looper; Looper.prepare() wasn't called on this thread")

            NanoLog.d(TAG, "Starting message loop on thread: ${Thread.currentThread().name}")

            looper.isLooping = true

            while (looper.isLooping) {
                // 从消息队列取出消息（阻塞）
                val msg = looper.queue.next() ?: break

                // 分发消息给 Handler
                msg.target?.dispatchMessage(msg)

                // 回收消息
                msg.isInUse = false
                msg.recycle()
            }

            NanoLog.d(TAG, "Message loop ended on thread: ${Thread.currentThread().name}")
        }
    }

    /** 消息队列 */
    internal val queue = NanoMessageQueue()

    /** 是否正在循环 */
    @Volatile
    private var isLooping = false

    /** 当前线程 */
    val thread: Thread = Thread.currentThread()

    /**
     * 退出消息循环
     *
     * @param safe 是否等待所有消息处理完毕
     */
    fun quit(safe: Boolean = false) {
        NanoLog.d(TAG, "Quitting looper (safe=$safe)")

        if (safe) {
            queue.quitSafely()
        } else {
            queue.quit()
        }
        isLooping = false
    }

    /**
     * 安全退出
     */
    fun quitSafely() {
        quit(safe = true)
    }

    /**
     * 是否是当前线程的 Looper
     */
    fun isCurrentThread(): Boolean = Thread.currentThread() == thread
}

/**
 * NanoMessageQueue - 消息队列
 *
 * 使用优先队列按时间排序消息，支持延迟消息。
 */
class NanoMessageQueue {

    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()

    /** 消息队列（按执行时间排序，时间相同时按序列号排序以保证FIFO） */
    private val messages = PriorityQueue<NanoMessage>(compareBy<NanoMessage> { it.`when` }.thenBy { it.seq })

    /** 是否已退出 */
    @Volatile
    private var isQuitting = false

    /**
     * 入队消息
     *
     * @param msg 消息
     * @param uptimeMillis 执行时间（毫秒）
     */
    fun enqueueMessage(msg: NanoMessage, uptimeMillis: Long): Boolean {
        if (msg.target == null) {
            throw IllegalArgumentException("Message must have a target handler")
        }

        lock.withLock {
            if (isQuitting) {
                NanoLog.w("MessageQueue", "Sending message to a quitting queue")
                return false
            }

            msg.isInUse = true
            msg.`when` = uptimeMillis
            messages.offer(msg)
            notEmpty.signal()
        }

        return true
    }

    /**
     * 获取下一个消息（阻塞）
     */
    fun next(): NanoMessage? {
        while (true) {
            lock.withLock {
                if (isQuitting) {
                    return null
                }

                val now = System.currentTimeMillis()
                val msg = messages.peek()

                if (msg == null) {
                    // 队列为空，等待新消息
                    notEmpty.await()
                } else if (msg.`when` <= now) {
                    // 消息到期，返回
                    return messages.poll()
                } else {
                    // 等待到消息到期时间
                    val timeout = msg.`when` - now
                    notEmpty.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                }
            }
        }
    }

    /**
     * 移除所有匹配的消息
     */
    fun removeMessages(handler: NanoHandler, what: Int) {
        lock.withLock {
            messages.removeIf { it.target == handler && it.what == what }
        }
    }

    /**
     * 移除所有匹配的回调
     */
    fun removeCallbacks(handler: NanoHandler, r: Runnable) {
        lock.withLock {
            messages.removeIf { it.target == handler && it.callback == r }
        }
    }

    /**
     * 退出
     */
    fun quit() {
        lock.withLock {
            isQuitting = true
            messages.clear()
            notEmpty.signalAll()
        }
    }

    /**
     * 安全退出（处理完所有已到期的消息）
     */
    fun quitSafely() {
        lock.withLock {
            isQuitting = true
            val now = System.currentTimeMillis()
            messages.removeIf { it.`when` > now }
            notEmpty.signalAll()
        }
    }

    /**
     * 检查是否有消息
     */
    fun hasMessages(handler: NanoHandler, what: Int): Boolean {
        lock.withLock {
            return messages.any { it.target == handler && it.what == what }
        }
    }
}
