package com.nano.framework.pm

/**
 * NanoComponentInfo - 组件信息基类
 */
abstract class NanoComponentInfo(
    /** 组件名称 */
    open val name: String,

    /** 所属包名 */
    open val packageName: String,

    /** 是否已启用 */
    open val enabled: Boolean = true
) {
    /**
     * 获取完整组件名
     */
    fun getFullName(): String = "$packageName/$name"

    override fun toString(): String {
        return "${javaClass.simpleName}(name=$name, package=$packageName)"
    }
}

/**
 * NanoActivityInfo - Activity 组件信息
 */
data class NanoActivityInfo(
    override val name: String,
    override val packageName: String,
    override val enabled: Boolean = true,

    /** 标签（显示名称） */
    val label: String = name,

    /** 图标资源 ID */
    val icon: Int = 0,

    /** 启动模式 */
    val launchMode: LaunchMode = LaunchMode.STANDARD,

    /** 任务亲和性 */
    val taskAffinity: String = packageName,

    /** 是否为启动器入口 */
    val isLauncher: Boolean = false
) : NanoComponentInfo(name, packageName, enabled) {

    enum class LaunchMode {
        STANDARD,        // 标准模式
        SINGLE_TOP,      // 栈顶复用
        SINGLE_TASK,     // 栈内复用
        SINGLE_INSTANCE  // 单实例
    }
}

/**
 * NanoServiceInfo - Service 组件信息
 */
data class NanoServiceInfo(
    override val name: String,
    override val packageName: String,
    override val enabled: Boolean = true,

    /** 是否在独立进程运行 */
    val isolatedProcess: Boolean = false
) : NanoComponentInfo(name, packageName, enabled)
