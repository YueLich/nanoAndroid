package com.nano.app.activity

import android.content.Context
import com.nano.app.context.NanoContext
import com.nano.app.intent.NanoIntent
import com.nano.kernel.NanoLog

/**
 * NanoActivity - Activity 基类
 *
 * 模拟 Android Activity 的生命周期和功能
 */
abstract class NanoActivity(androidContext: Context) : NanoContext(androidContext) {

    companion object {
        private const val TAG = "NanoActivity"
    }

    /** Activity Token（由 AMS 分配） */
    var activityToken: String? = null
        internal set

    /** 启动此 Activity 的 Intent */
    var intent: NanoIntent? = null
        internal set

    /** Activity 是否已完成 */
    private var isFinishing = false

    /** Activity 是否已销毁 */
    private var isDestroyed = false

    // ==================== 生命周期方法 ====================

    /**
     * onCreate - 创建
     *
     * Activity 首次创建时调用
     */
    protected open fun onCreate() {
        NanoLog.d(TAG, "onCreate: ${javaClass.simpleName}")
    }

    /**
     * onStart - 启动
     *
     * Activity 即将可见时调用
     */
    protected open fun onStart() {
        NanoLog.d(TAG, "onStart: ${javaClass.simpleName}")
    }

    /**
     * onResume - 恢复
     *
     * Activity 即将与用户交互时调用
     */
    protected open fun onResume() {
        NanoLog.d(TAG, "onResume: ${javaClass.simpleName}")
    }

    /**
     * onPause - 暂停
     *
     * Activity 即将失去焦点时调用
     */
    protected open fun onPause() {
        NanoLog.d(TAG, "onPause: ${javaClass.simpleName}")
    }

    /**
     * onStop - 停止
     *
     * Activity 不再可见时调用
     */
    protected open fun onStop() {
        NanoLog.d(TAG, "onStop: ${javaClass.simpleName}")
    }

    /**
     * onDestroy - 销毁
     *
     * Activity 即将被销毁时调用
     */
    protected open fun onDestroy() {
        NanoLog.d(TAG, "onDestroy: ${javaClass.simpleName}")
        isDestroyed = true
    }

    /**
     * onNewIntent - 接收新 Intent
     *
     * 当 Activity 处于 singleTop/singleTask 模式被复用时调用
     */
    protected open fun onNewIntent(intent: NanoIntent) {
        NanoLog.d(TAG, "onNewIntent: ${javaClass.simpleName}")
        this.intent = intent
    }

    // ==================== 生命周期触发（由框架调用） ====================

    /**
     * 内部方法：执行 onCreate
     */
    internal fun performCreate(intent: NanoIntent) {
        this.intent = intent
        onCreate()
    }

    /**
     * 内部方法：执行 onStart
     */
    internal fun performStart() {
        onStart()
    }

    /**
     * 内部方法：执行 onResume
     */
    internal fun performResume() {
        onResume()
    }

    /**
     * 内部方法：执行 onPause
     */
    internal fun performPause() {
        onPause()
    }

    /**
     * 内部方法：执行 onStop
     */
    internal fun performStop() {
        onStop()
    }

    /**
     * 内部方法：执行 onDestroy
     */
    internal fun performDestroy() {
        onDestroy()
    }

    /**
     * 内部方法：执行 onNewIntent
     */
    internal fun performNewIntent(intent: NanoIntent) {
        onNewIntent(intent)
    }

    // ==================== Activity 操作 ====================

    /**
     * 启动另一个 Activity
     */
    override fun startActivity(intent: NanoIntent) {
        super.startActivity(intent)
    }

    /**
     * 结束当前 Activity
     */
    fun finish() {
        if (isFinishing || isDestroyed) {
            return
        }

        isFinishing = true
        NanoLog.d(TAG, "finish: ${javaClass.simpleName}")

        val token = activityToken
        if (token == null) {
            NanoLog.w(TAG, "Cannot finish: activity token is null")
            return
        }

        // 通知 AMS 结束 Activity
        runOnMainThread {
            val activityManager = getActivityManager()
            activityManager?.finishActivity(token)
        }
    }

    /**
     * 检查 Activity 是否正在结束
     */
    fun isFinishing(): Boolean = isFinishing

    /**
     * 检查 Activity 是否已销毁
     */
    fun isDestroyed(): Boolean = isDestroyed

    // ==================== Intent 数据获取 ====================

    /**
     * 设置新的 Intent
     */
    fun setIntent(intent: NanoIntent) {
        this.intent = intent
    }

    override fun toString(): String {
        return "${javaClass.simpleName}@${hashCode().toString(16)}"
    }
}
