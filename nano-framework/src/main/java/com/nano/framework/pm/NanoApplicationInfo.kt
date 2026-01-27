package com.nano.framework.pm

/**
 * NanoApplicationInfo - 应用信息
 *
 * 描述一个应用的基本信息
 */
data class NanoApplicationInfo(
    /** 包名 */
    val packageName: String,

    /** 应用名称 */
    val name: String,

    /** 版本号 */
    val versionCode: Int = 1,

    /** 版本名称 */
    val versionName: String = "1.0",

    /** 是否为系统应用 */
    val isSystem: Boolean = false,

    /** 应用图标资源 ID */
    val icon: Int = 0,

    /** 应用描述 */
    val description: String = ""
) {
    override fun toString(): String {
        return "ApplicationInfo(packageName=$packageName, name=$name, version=$versionName)"
    }
}
