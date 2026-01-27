package com.nano.framework.am

import com.nano.framework.pm.NanoActivityInfo
import java.util.UUID

/**
 * NanoActivityRecord - Activity 记录
 *
 * 描述一个正在运行或曾经运行过的 Activity 实例
 */
data class NanoActivityRecord(
    /** 唯一标识符（Token） */
    val token: String = UUID.randomUUID().toString(),

    /** Activity 组件信息 */
    val activityInfo: NanoActivityInfo,

    /** 当前生命周期状态 */
    var state: LifecycleState = LifecycleState.INITIALIZED,

    /** 所属任务 */
    var task: NanoTaskRecord? = null,

    /** 创建时间 */
    val createTime: Long = System.currentTimeMillis()
) {

    /**
     * Activity 生命周期状态
     */
    enum class LifecycleState {
        INITIALIZED,    // 已初始化（未创建）
        CREATED,        // 已创建（onCreate 完成）
        STARTED,        // 已启动（onStart 完成）
        RESUMED,        // 已恢复（onResume 完成，可交互）
        PAUSED,         // 已暂停（onPause 完成）
        STOPPED,        // 已停止（onStop 完成）
        DESTROYED       // 已销毁（onDestroy 完成）
    }

    /**
     * 是否可见
     */
    fun isVisible(): Boolean {
        return state == LifecycleState.STARTED || state == LifecycleState.RESUMED
    }

    /**
     * 是否在前台
     */
    fun isForeground(): Boolean {
        return state == LifecycleState.RESUMED
    }

    /**
     * 获取完整的 Activity 名称
     */
    fun getFullName(): String = activityInfo.getFullName()

    override fun toString(): String {
        return "ActivityRecord(token=${token.substring(0, 8)}, " +
                "activity=${activityInfo.name}, state=$state)"
    }
}
