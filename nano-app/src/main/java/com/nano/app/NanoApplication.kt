package com.nano.app

import com.nano.app.context.NanoContext
import com.nano.kernel.NanoLog
import com.nano.kernel.handler.NanoLooper

/**
 * NanoApplication - 应用基类
 *
 * 纯 Kotlin 实现，不依赖 Android Application
 */
abstract class NanoApplication {

    companion object {
        private const val TAG = "NanoApplication"

        /** 全局单例 */
        @Volatile
        private var instance: NanoApplication? = null

        /**
         * 获取 Application 实例
         */
        fun getInstance(): NanoApplication? = instance
    }

    /** 应用包名 */
    open val packageName: String = "com.nano.app"

    /** Application Context 包装 */
    private lateinit var nanoContext: ApplicationContext

    /**
     * Application Context 实现
     */
    private inner class ApplicationContext(val app: NanoApplication) :
        NanoContext() {

        override fun getPackageName(): String = app.packageName

        override fun onStartActivityFailed(intent: com.nano.app.intent.NanoIntent) {
            NanoLog.e(TAG, "Failed to start activity: $intent")
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 创建应用
     */
    open fun onCreate() {
        instance = this

        NanoLog.i(TAG, "============================================")
        NanoLog.i(TAG, "NanoApplication onCreate: ${javaClass.simpleName}")
        NanoLog.i(TAG, "Package: $packageName")
        NanoLog.i(TAG, "============================================")

        // 准备主线程 Looper
        try {
            NanoLooper.prepareMainLooper()
            NanoLog.d(TAG, "Main Looper prepared")
        } catch (e: Exception) {
            NanoLog.w(TAG, "Main Looper already prepared")
        }

        // 创建 NanoContext
        nanoContext = ApplicationContext(this)

        // 调用子类初始化
        onNanoCreate()
    }

    /**
     * 终止应用
     */
    open fun onTerminate() {
        NanoLog.i(TAG, "NanoApplication onTerminate: ${javaClass.simpleName}")
        onNanoTerminate()
        instance = null
    }

    // ==================== Nano 生命周期（子类可重写） ====================

    /**
     * Nano 应用创建
     *
     * 在 onCreate 之后调用，子类可以在这里初始化
     */
    protected open fun onNanoCreate() {
        // 子类可以重写
    }

    /**
     * Nano 应用终止
     *
     * 在 onTerminate 之前调用
     */
    protected open fun onNanoTerminate() {
        // 子类可以重写
    }

    // ==================== Context 访问 ====================

    /**
     * 获取 NanoContext
     */
    fun getNanoContext(): NanoContext {
        return nanoContext
    }

    /**
     * 启动 Activity
     */
    fun startActivity(intent: com.nano.app.intent.NanoIntent) {
        nanoContext.startActivity(intent)
    }

    /**
     * 在主线程运行
     */
    fun runOnMainThread(action: () -> Unit) {
        nanoContext.runOnMainThread(action)
    }
}
