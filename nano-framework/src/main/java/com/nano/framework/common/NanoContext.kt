package com.nano.framework.common

/**
 * NanoContext - 纯 Kotlin 的上下文抽象
 *
 * 不依赖 Android Context，提供操作系统级别的上下文信息
 */
interface NanoContext {
    /**
     * 获取应用包名
     */
    fun getPackageName(): String

    /**
     * 获取应用名称
     */
    fun getApplicationName(): String

    /**
     * 获取文件目录
     */
    fun getFilesDir(): String

    /**
     * 获取缓存目录
     */
    fun getCacheDir(): String

    /**
     * 获取系统服务
     */
    fun getSystemService(name: String): Any?
}

/**
 * 默认的 NanoContext 实现
 */
class DefaultNanoContext(
    private val packageName: String = "com.nano.android",
    private val applicationName: String = "NanoAndroid"
) : NanoContext {

    override fun getPackageName(): String = packageName

    override fun getApplicationName(): String = applicationName

    override fun getFilesDir(): String = "./files"

    override fun getCacheDir(): String = "./cache"

    override fun getSystemService(name: String): Any? {
        // 由系统服务管理器提供
        return com.nano.kernel.binder.NanoServiceManager.getService(name)
    }
}
