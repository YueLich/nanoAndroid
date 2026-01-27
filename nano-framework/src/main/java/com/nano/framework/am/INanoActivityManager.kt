package com.nano.framework.am

import com.nano.kernel.binder.INanoInterface
import com.nano.kernel.binder.NanoBinder

/**
 * INanoActivityManager - ActivityManager Binder 接口
 */
interface INanoActivityManager : INanoInterface {

    /**
     * 启动 Activity
     *
     * @param packageName 包名
     * @param activityName Activity 名称
     * @param flags 启动标志
     * @return Activity Token，失败返回 null
     */
    fun startActivity(
        packageName: String,
        activityName: String,
        flags: Int = 0
    ): String?

    /**
     * 结束 Activity
     *
     * @param token Activity Token
     * @return 是否成功
     */
    fun finishActivity(token: String): Boolean

    /**
     * 获取当前前台 Activity
     */
    fun getForegroundActivity(): NanoActivityRecord?

    /**
     * 获取所有运行中的任务
     */
    fun getRunningTasks(maxNum: Int = 10): List<NanoTaskRecord>

    /**
     * 获取任务栈信息
     */
    fun getTaskInfo(taskId: Int): NanoTaskRecord?

    /**
     * 将任务移到前台
     */
    fun moveTaskToFront(taskId: Int): Boolean

    /**
     * 移除任务
     */
    fun removeTask(taskId: Int): Boolean

    companion object {
        const val DESCRIPTOR = "com.nano.framework.am.INanoActivityManager"

        // 事务代码
        const val TRANSACTION_START_ACTIVITY = NanoBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_FINISH_ACTIVITY = NanoBinder.FIRST_CALL_TRANSACTION + 2
        const val TRANSACTION_GET_FOREGROUND_ACTIVITY = NanoBinder.FIRST_CALL_TRANSACTION + 3
        const val TRANSACTION_GET_RUNNING_TASKS = NanoBinder.FIRST_CALL_TRANSACTION + 4
        const val TRANSACTION_GET_TASK_INFO = NanoBinder.FIRST_CALL_TRANSACTION + 5
        const val TRANSACTION_MOVE_TASK_TO_FRONT = NanoBinder.FIRST_CALL_TRANSACTION + 6
        const val TRANSACTION_REMOVE_TASK = NanoBinder.FIRST_CALL_TRANSACTION + 7
    }
}
