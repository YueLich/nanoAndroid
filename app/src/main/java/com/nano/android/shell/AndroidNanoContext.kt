package com.nano.android.shell

import android.content.Context
import com.nano.framework.common.NanoContext

/**
 * AndroidNanoContext - Android 平台的 NanoContext 实现
 *
 * 将 Android Context 桥接到 NanoContext 接口
 */
class AndroidNanoContext(
    private val androidContext: Context
) : NanoContext {

    override fun getPackageName(): String {
        return androidContext.packageName
    }

    override fun getApplicationName(): String {
        return androidContext.applicationInfo?.loadLabel(androidContext.packageManager)?.toString()
            ?: androidContext.packageName
    }

    override fun getFilesDir(): String {
        return androidContext.filesDir?.absolutePath ?: "./files"
    }

    override fun getCacheDir(): String {
        return androidContext.cacheDir?.absolutePath ?: "./cache"
    }

    override fun getSystemService(name: String): Any? {
        // 由 NanoServiceManager 提供
        return com.nano.kernel.binder.NanoServiceManager.getService(name)
    }
}
