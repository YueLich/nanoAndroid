package com.nano.framework.pm

import com.nano.kernel.binder.INanoInterface
import com.nano.kernel.binder.NanoBinder

/**
 * INanoPackageManager - PackageManager Binder 接口
 */
interface INanoPackageManager : INanoInterface {

    /**
     * 获取应用信息
     */
    fun getApplicationInfo(packageName: String): NanoApplicationInfo?

    /**
     * 获取所有已安装的应用
     */
    fun getInstalledApplications(flags: Int = 0): List<NanoApplicationInfo>

    /**
     * 查询 Activity 信息
     */
    fun getActivityInfo(packageName: String, activityName: String): NanoActivityInfo?

    /**
     * 查询所有 Launcher Activity
     */
    fun getLauncherActivities(): List<NanoActivityInfo>

    /**
     * 查询可以处理指定 action 的 Activity
     */
    fun queryActivitiesForAction(action: String): List<NanoActivityInfo>

    /**
     * 检查包是否存在
     */
    fun isPackageInstalled(packageName: String): Boolean

    companion object {
        const val DESCRIPTOR = "com.nano.framework.pm.INanoPackageManager"

        // 事务代码
        const val TRANSACTION_GET_APPLICATION_INFO = NanoBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_GET_INSTALLED_APPLICATIONS = NanoBinder.FIRST_CALL_TRANSACTION + 2
        const val TRANSACTION_GET_ACTIVITY_INFO = NanoBinder.FIRST_CALL_TRANSACTION + 3
        const val TRANSACTION_GET_LAUNCHER_ACTIVITIES = NanoBinder.FIRST_CALL_TRANSACTION + 4
        const val TRANSACTION_QUERY_ACTIVITIES_FOR_ACTION = NanoBinder.FIRST_CALL_TRANSACTION + 5
        const val TRANSACTION_IS_PACKAGE_INSTALLED = NanoBinder.FIRST_CALL_TRANSACTION + 6
    }
}
