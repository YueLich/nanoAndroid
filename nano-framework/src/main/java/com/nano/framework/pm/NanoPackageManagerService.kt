package com.nano.framework.pm

import android.content.Context
import com.nano.kernel.NanoLog
import com.nano.kernel.binder.NanoBinder
import com.nano.kernel.binder.NanoParcel
import java.util.concurrent.ConcurrentHashMap

/**
 * NanoPackageManagerService - 包管理服务
 *
 * 负责管理应用包和组件信息
 *
 * 简化实现：
 * - 不支持真实的 APK 安装
 * - 应用信息通过代码注册
 * - 不支持权限管理
 */
class NanoPackageManagerService(
    private val context: Context
) : NanoBinder(), INanoPackageManager {

    companion object {
        private const val TAG = "NanoPackageManagerService"
    }

    /** 已安装的应用 */
    private val applications = ConcurrentHashMap<String, NanoApplicationInfo>()

    /** Activity 组件信息 */
    private val activities = ConcurrentHashMap<String, NanoActivityInfo>()

    /** Service 组件信息 */
    private val services = ConcurrentHashMap<String, NanoServiceInfo>()

    init {
        attachInterface(this, INanoPackageManager.DESCRIPTOR)
        NanoLog.d(TAG, "NanoPackageManagerService created")

        // 注册系统应用
        registerSystemApplications()
    }

    /**
     * 注册系统应用和组件
     */
    private fun registerSystemApplications() {
        // 注册 Launcher
        registerApplication(
            NanoApplicationInfo(
                packageName = "com.nano.android",
                name = "NanoLauncher",
                isSystem = true
            )
        )

        // 注册 Launcher 的主 Activity
        registerActivity(
            NanoActivityInfo(
                name = "com.nano.android.shell.NanoShellActivity",
                packageName = "com.nano.android",
                label = "Launcher",
                launchMode = NanoActivityInfo.LaunchMode.SINGLE_TASK,
                isLauncher = true
            )
        )

        NanoLog.i(TAG, "System applications registered")
    }

    // ==================== Public API ====================

    /**
     * 注册应用
     */
    fun registerApplication(appInfo: NanoApplicationInfo) {
        applications[appInfo.packageName] = appInfo
        NanoLog.d(TAG, "Registered application: ${appInfo.packageName}")
    }

    /**
     * 注册 Activity
     */
    fun registerActivity(activityInfo: NanoActivityInfo) {
        val key = activityInfo.getFullName()
        activities[key] = activityInfo
        NanoLog.d(TAG, "Registered activity: $key")
    }

    /**
     * 注册 Service
     */
    fun registerService(serviceInfo: NanoServiceInfo) {
        val key = serviceInfo.getFullName()
        services[key] = serviceInfo
        NanoLog.d(TAG, "Registered service: $key")
    }

    // ==================== INanoPackageManager Implementation ====================

    override fun getApplicationInfo(packageName: String): NanoApplicationInfo? {
        return applications[packageName]
    }

    override fun getInstalledApplications(flags: Int): List<NanoApplicationInfo> {
        return applications.values.toList()
    }

    override fun getActivityInfo(packageName: String, activityName: String): NanoActivityInfo? {
        val key = "$packageName/$activityName"
        return activities[key]
    }

    override fun getLauncherActivities(): List<NanoActivityInfo> {
        return activities.values.filter { it.isLauncher }
    }

    override fun queryActivitiesForAction(action: String): List<NanoActivityInfo> {
        // 简化实现：根据 action 返回匹配的 Activity
        return when (action) {
            "android.intent.action.MAIN" -> getLauncherActivities()
            else -> emptyList()
        }
    }

    override fun isPackageInstalled(packageName: String): Boolean {
        return applications.containsKey(packageName)
    }

    // ==================== Binder Implementation ====================

    override fun onTransact(
        code: Int,
        data: NanoParcel,
        reply: NanoParcel?,
        flags: Int
    ): Boolean {
        when (code) {
            INanoPackageManager.TRANSACTION_GET_APPLICATION_INFO -> {
                data.enforceInterface(INanoPackageManager.DESCRIPTOR)
                val packageName = data.readString() ?: return false
                val appInfo = getApplicationInfo(packageName)
                reply?.writeString(appInfo?.packageName)
                reply?.writeString(appInfo?.name)
                return true
            }

            INanoPackageManager.TRANSACTION_GET_INSTALLED_APPLICATIONS -> {
                data.enforceInterface(INanoPackageManager.DESCRIPTOR)
                val flags = data.readInt()
                val apps = getInstalledApplications(flags)
                reply?.writeInt(apps.size)
                apps.forEach {
                    reply?.writeString(it.packageName)
                    reply?.writeString(it.name)
                }
                return true
            }

            INanoPackageManager.TRANSACTION_GET_ACTIVITY_INFO -> {
                data.enforceInterface(INanoPackageManager.DESCRIPTOR)
                val packageName = data.readString() ?: return false
                val activityName = data.readString() ?: return false
                val activityInfo = getActivityInfo(packageName, activityName)
                reply?.writeBoolean(activityInfo != null)
                if (activityInfo != null) {
                    reply?.writeString(activityInfo.name)
                    reply?.writeString(activityInfo.packageName)
                    reply?.writeString(activityInfo.label)
                }
                return true
            }

            INanoPackageManager.TRANSACTION_GET_LAUNCHER_ACTIVITIES -> {
                data.enforceInterface(INanoPackageManager.DESCRIPTOR)
                val launcherActivities = getLauncherActivities()
                reply?.writeInt(launcherActivities.size)
                launcherActivities.forEach {
                    reply?.writeString(it.name)
                    reply?.writeString(it.packageName)
                    reply?.writeString(it.label)
                }
                return true
            }

            INanoPackageManager.TRANSACTION_IS_PACKAGE_INSTALLED -> {
                data.enforceInterface(INanoPackageManager.DESCRIPTOR)
                val packageName = data.readString() ?: return false
                val installed = isPackageInstalled(packageName)
                reply?.writeBoolean(installed)
                return true
            }
        }

        return false
    }

    /**
     * 系统就绪回调
     */
    fun systemReady() {
        NanoLog.i(TAG, "PackageManagerService is ready")
    }
}
