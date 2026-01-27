package com.nano.kernel.binder

import com.nano.kernel.NanoLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * NanoServiceManager - 服务管理器
 *
 * 模拟 Android ServiceManager，负责系统服务的注册和查找。
 *
 * 在真实 Android 中，ServiceManager 是一个 native 服务，
 * 运行在 init 进程中，是所有 Binder 服务的根。
 *
 * 核心功能：
 * 1. 服务注册：系统服务启动后注册到 ServiceManager
 * 2. 服务查找：客户端通过名称获取服务的 Binder 引用
 * 3. 服务等待：等待某个服务可用
 */
object NanoServiceManager {

    private const val TAG = "ServiceManager"

    /** 已注册的服务 */
    private val services = ConcurrentHashMap<String, NanoBinder>()

    /** 等待服务可用的回调 */
    private val pendingCallbacks = ConcurrentHashMap<String, MutableList<(NanoBinder) -> Unit>>()

    /** 服务可用的 Latch（用于同步等待） */
    private val serviceLatches = ConcurrentHashMap<String, CountDownLatch>()

    /**
     * 注册系统服务
     *
     * @param name 服务名称（如 "activity", "window", "package"）
     * @param service 服务的 Binder 实例
     */
    fun addService(name: String, service: NanoBinder) {
        NanoLog.i(TAG, "Adding service: $name")

        services[name] = service

        // 通知等待该服务的回调
        pendingCallbacks.remove(name)?.forEach { callback ->
            try {
                callback(service)
            } catch (e: Exception) {
                NanoLog.e(TAG, "Callback failed for service: $name", e)
            }
        }

        // 释放同步等待的 Latch
        serviceLatches.remove(name)?.countDown()
    }

    /**
     * 获取系统服务
     *
     * @param name 服务名称
     * @return 服务的 Binder 实例，如果服务未注册则返回 null
     */
    fun getService(name: String): NanoBinder? {
        return services[name]
    }

    /**
     * 检查服务是否已注册
     *
     * @param name 服务名称
     * @return 服务是否已注册
     */
    fun checkService(name: String): NanoBinder? {
        return services[name]
    }

    /**
     * 异步等待服务可用
     *
     * @param name 服务名称
     * @param callback 服务可用时的回调
     */
    fun waitForService(name: String, callback: (NanoBinder) -> Unit) {
        // 如果服务已可用，直接回调
        services[name]?.let {
            callback(it)
            return
        }

        // 否则加入等待列表
        pendingCallbacks.getOrPut(name) { mutableListOf() }.add(callback)
    }

    /**
     * 同步等待服务可用
     *
     * @param name 服务名称
     * @param timeout 超时时间（毫秒）
     * @return 服务的 Binder 实例，如果超时则返回 null
     */
    fun waitForServiceSync(name: String, timeout: Long = 10000): NanoBinder? {
        // 如果服务已可用，直接返回
        services[name]?.let { return it }

        // 创建 Latch 等待
        val latch = serviceLatches.getOrPut(name) { CountDownLatch(1) }

        return if (latch.await(timeout, TimeUnit.MILLISECONDS)) {
            services[name]
        } else {
            NanoLog.w(TAG, "Timeout waiting for service: $name")
            null
        }
    }

    /**
     * 列出所有已注册的服务
     *
     * @return 服务名称列表
     */
    fun listServices(): List<String> {
        return services.keys.toList()
    }

    /**
     * 移除服务（通常不需要调用）
     *
     * @param name 服务名称
     */
    internal fun removeService(name: String) {
        NanoLog.w(TAG, "Removing service: $name")
        services.remove(name)
    }

    /**
     * 清空所有服务（仅用于测试）
     */
    internal fun clearAllServices() {
        NanoLog.w(TAG, "Clearing all services")
        services.clear()
        pendingCallbacks.clear()
        serviceLatches.clear()
    }
}
