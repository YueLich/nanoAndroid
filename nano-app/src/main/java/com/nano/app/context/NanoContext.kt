package com.nano.app.context

import android.content.Context
import com.nano.app.intent.NanoIntent
import com.nano.framework.am.INanoActivityManager
import com.nano.framework.pm.INanoPackageManager
import com.nano.framework.wm.INanoWindowManager
import com.nano.kernel.binder.NanoBinder
import com.nano.kernel.binder.NanoServiceManager
import com.nano.kernel.handler.NanoHandler
import com.nano.kernel.handler.NanoLooper

/**
 * NanoContext - 应用上下文
 *
 * 模拟 Android Context，提供访问系统服务和资源的能力
 */
abstract class NanoContext(
    /** Android 原生 Context（用于访问真实的系统资源） */
    internal val androidContext: Context
) {

    /** 主线程 Handler */
    internal val mainHandler: NanoHandler by lazy {
        NanoHandler(NanoLooper.getMainLooper())
    }

    // ==================== 系统服务访问 ====================

    /**
     * 获取系统服务
     */
    fun getSystemService(name: String): Any? {
        val binder = NanoServiceManager.getService(name)
        return when (name) {
            ACTIVITY_SERVICE -> binder?.queryLocalInterface(
                INanoActivityManager.DESCRIPTOR
            ) as? INanoActivityManager

            WINDOW_SERVICE -> binder?.queryLocalInterface(
                INanoWindowManager.DESCRIPTOR
            ) as? INanoWindowManager

            PACKAGE_SERVICE -> binder?.queryLocalInterface(
                INanoPackageManager.DESCRIPTOR
            ) as? INanoPackageManager

            else -> null
        }
    }

    /**
     * 获取 ActivityManager
     */
    fun getActivityManager(): INanoActivityManager? {
        return getSystemService(ACTIVITY_SERVICE) as? INanoActivityManager
    }

    /**
     * 获取 WindowManager
     */
    fun getWindowManager(): INanoWindowManager? {
        return getSystemService(WINDOW_SERVICE) as? INanoWindowManager
    }

    /**
     * 获取 PackageManager
     */
    fun getPackageManager(): INanoPackageManager? {
        return getSystemService(PACKAGE_SERVICE) as? INanoPackageManager
    }

    // ==================== 组件启动 ====================

    /**
     * 启动 Activity
     */
    open fun startActivity(intent: NanoIntent) {
        val packageName = intent.packageName
        val className = intent.className

        if (packageName == null || className == null) {
            throw IllegalArgumentException("Intent must specify component")
        }

        val activityManager = getActivityManager()
        if (activityManager == null) {
            throw IllegalStateException("ActivityManager not available")
        }

        // 在主线程上启动
        mainHandler.post {
            val token = activityManager.startActivity(
                packageName = packageName,
                activityName = className,
                flags = intent.flags
            )

            if (token == null) {
                onStartActivityFailed(intent)
            }
        }
    }

    /**
     * Activity 启动失败回调
     */
    protected open fun onStartActivityFailed(intent: NanoIntent) {
        // 子类可以重写
    }

    // ==================== 资源访问 ====================

    /**
     * 获取应用包名
     */
    fun getPackageName(): String = androidContext.packageName

    /**
     * 获取应用信息
     */
    fun getApplicationInfo() = androidContext.applicationInfo

    // ==================== 线程和 Handler ====================

    /**
     * 在主线程运行
     */
    fun runOnMainThread(action: () -> Unit) {
        mainHandler.post(action)
    }

    /**
     * 延迟在主线程运行
     */
    fun postDelayed(action: () -> Unit, delayMillis: Long) {
        mainHandler.postDelayed(action, delayMillis)
    }

    companion object {
        // 系统服务名称
        const val ACTIVITY_SERVICE = "activity"
        const val WINDOW_SERVICE = "window"
        const val PACKAGE_SERVICE = "package"
        const val LLM_SERVICE = "llm"
    }
}
