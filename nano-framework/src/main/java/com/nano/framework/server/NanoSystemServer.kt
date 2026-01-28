package com.nano.framework.server

import android.content.Context
import android.os.HandlerThread
import com.nano.kernel.NanoLog
import com.nano.kernel.binder.NanoServiceManager
import com.nano.kernel.handler.NanoHandler
import com.nano.kernel.handler.NanoLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * NanoSystemServer - 系统服务启动器
 *
 * 模拟 Android SystemServer 的启动流程，负责启动所有系统服务。
 *
 * 启动顺序：
 * 1. Bootstrap Services - 引导服务（必须最先启动）
 *    - PackageManagerService
 *    - ActivityManagerService
 *
 * 2. Core Services - 核心服务（基础功能服务）
 *    - WindowManagerService
 *
 * 3. Other Services - 其他服务（可延迟启动）
 *    - LLMService
 *
 * 4. System Ready - 系统就绪
 *    - 通知所有服务
 *    - 启动 Launcher
 */
class NanoSystemServer(
    private val context: Context
) {

    companion object {
        private const val TAG = "NanoSystemServer"

        /** 服务名称常量 */
        const val ACTIVITY_SERVICE = "activity"
        const val WINDOW_SERVICE = "window"
        const val PACKAGE_SERVICE = "package"
        const val LLM_SERVICE = "llm"
    }

    /** 系统服务线程 */
    private lateinit var systemThread: HandlerThread

    /** 系统服务 Handler */
    private lateinit var systemHandler: NanoHandler

    /** 服务就绪计数器 */
    private val servicesReadyLatch = CountDownLatch(4)

    /** 系统是否已就绪 */
    @Volatile
    private var isSystemReady = false

    /** 外部服务工厂列表（避免模块循环依赖） */
    private val externalServiceFactories = mutableListOf<Pair<String, () -> com.nano.kernel.binder.NanoBinder>>()

    /** 系统就绪回调列表 */
    private val systemReadyCallbacks = mutableListOf<() -> Unit>()

    // ==================== 系统服务实例 ====================

    private lateinit var packageManagerService: com.nano.framework.pm.NanoPackageManagerService
    private lateinit var activityManagerService: com.nano.framework.am.NanoActivityManagerService
    private lateinit var windowManagerService: com.nano.framework.wm.NanoWindowManagerService

    /**
     * 注册外部服务工厂
     *
     * 用于避免 nano-framework 对 nano-llm 等模块的直接依赖。
     * 外部模块在系统启动前调用此方法注册服务工厂，
     * Phase 3 时自动创建并注册到 NanoServiceManager。
     *
     * @param name 服务名称（如 "llm"）
     * @param factory 服务工厂 lambda，返回 NanoBinder 实例
     */
    fun registerExternalService(name: String, factory: () -> com.nano.kernel.binder.NanoBinder) {
        externalServiceFactories.add(name to factory)
    }

    /**
     * 注册系统就绪回调
     *
     * 外部服务可以注册回调，在系统所有服务启动后被通知。
     */
    fun registerSystemReadyCallback(callback: () -> Unit) {
        systemReadyCallbacks.add(callback)
    }

    /**
     * 启动系统服务器
     */
    fun run() {
        NanoLog.i(TAG, "============================================")
        NanoLog.i(TAG, "NanoSystemServer starting...")
        NanoLog.i(TAG, "============================================")

        // 创建系统服务线程
        systemThread = HandlerThread("NanoSystemServerThread").apply {
            start()
        }

        // 为系统服务线程创建 Looper（如果需要使用 NanoLooper）
        // 这里我们使用 Android 的 HandlerThread，它自带 Looper

        // 创建系统 Handler
        systemHandler = NanoHandler(NanoLooper.getMainLooper())

        // 在系统线程上启动服务
        android.os.Handler(systemThread.looper).post {
            try {
                // Phase 1: 启动引导服务
                startBootstrapServices()

                // Phase 2: 启动核心服务
                startCoreServices()

                // Phase 3: 启动其他服务
                startOtherServices()

                // Phase 4: 系统就绪
                systemReady()

            } catch (e: Exception) {
                NanoLog.e(TAG, "System server failed to start", e)
                throw RuntimeException("System server startup failed", e)
            }
        }
    }

    /**
     * Phase 1: 启动引导服务
     */
    private fun startBootstrapServices() {
        NanoLog.i(TAG, "--------------------------------------------")
        NanoLog.i(TAG, "Phase 1: Starting bootstrap services...")
        NanoLog.i(TAG, "--------------------------------------------")

        // 1. PackageManagerService - 管理应用包
        NanoLog.i(TAG, "Starting PackageManagerService...")
        packageManagerService = com.nano.framework.pm.NanoPackageManagerService(context)
        NanoServiceManager.addService(PACKAGE_SERVICE, packageManagerService)
        servicesReadyLatch.countDown()
        NanoLog.i(TAG, "PackageManagerService started")

        // 2. ActivityManagerService - 管理 Activity 和进程
        NanoLog.i(TAG, "Starting ActivityManagerService...")
        activityManagerService = com.nano.framework.am.NanoActivityManagerService(context, packageManagerService)
        NanoServiceManager.addService(ACTIVITY_SERVICE, activityManagerService)
        servicesReadyLatch.countDown()
        NanoLog.i(TAG, "ActivityManagerService started")
    }

    /**
     * Phase 2: 启动核心服务
     */
    private fun startCoreServices() {
        NanoLog.i(TAG, "--------------------------------------------")
        NanoLog.i(TAG, "Phase 2: Starting core services...")
        NanoLog.i(TAG, "--------------------------------------------")

        // WindowManagerService - 管理窗口
        NanoLog.i(TAG, "Starting WindowManagerService...")
        windowManagerService = com.nano.framework.wm.NanoWindowManagerService(context, activityManagerService)
        NanoServiceManager.addService(WINDOW_SERVICE, windowManagerService)

        servicesReadyLatch.countDown()
        NanoLog.i(TAG, "WindowManagerService started")
    }

    /**
     * Phase 3: 启动其他服务
     *
     * 包括外部模块通过 registerExternalService() 注册的服务（如 LLMService）
     */
    private fun startOtherServices() {
        NanoLog.i(TAG, "--------------------------------------------")
        NanoLog.i(TAG, "Phase 3: Starting other services...")
        NanoLog.i(TAG, "--------------------------------------------")

        // 启动外部注册的服务（如 LLMService，由 nano-llm 模块注册）
        externalServiceFactories.forEach { (name, factory) ->
            NanoLog.i(TAG, "Starting external service: $name...")
            val service = factory()
            NanoServiceManager.addService(name, service)
            NanoLog.i(TAG, "External service started: $name")
        }

        servicesReadyLatch.countDown()
        NanoLog.i(TAG, "Other services started")
    }

    /**
     * Phase 4: 系统就绪
     */
    private fun systemReady() {
        NanoLog.i(TAG, "============================================")
        NanoLog.i(TAG, "System is ready!")
        NanoLog.i(TAG, "============================================")

        isSystemReady = true

        // 通知所有服务系统就绪
        packageManagerService.systemReady()
        activityManagerService.systemReady()
        windowManagerService.systemReady()

        // 通知外部服务系统就绪
        systemReadyCallbacks.forEach { callback ->
            try {
                callback()
            } catch (e: Exception) {
                NanoLog.e(TAG, "System ready callback failed", e)
            }
        }

        // 列出已注册的服务
        val services = NanoServiceManager.listServices()
        NanoLog.i(TAG, "Registered services: $services")

        // 启动 Home Activity (Launcher)
        NanoLog.i(TAG, "Starting Home Activity...")
        activityManagerService.startHomeActivity()
    }

    /**
     * 等待系统就绪
     *
     * @param timeout 超时时间（毫秒）
     * @return 是否在超时前就绪
     */
    fun awaitReady(timeout: Long = 30000): Boolean {
        return try {
            servicesReadyLatch.await(timeout, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            NanoLog.e(TAG, "Interrupted while waiting for system ready", e)
            false
        }
    }

    /**
     * 系统是否已就绪
     */
    fun isReady(): Boolean = isSystemReady

    /**
     * 关闭系统服务器
     */
    fun shutdown() {
        NanoLog.i(TAG, "Shutting down NanoSystemServer...")

        // 停止系统线程
        systemThread.quitSafely()

        isSystemReady = false
    }
}
