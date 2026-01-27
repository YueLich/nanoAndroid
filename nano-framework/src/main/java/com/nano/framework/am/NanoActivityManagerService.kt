package com.nano.framework.am

import android.content.Context
import com.nano.framework.pm.NanoPackageManagerService
import com.nano.kernel.NanoLog
import com.nano.kernel.binder.NanoBinder
import com.nano.kernel.binder.NanoParcel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * NanoActivityManagerService - Activity 管理服务
 *
 * 负责：
 * 1. Activity 生命周期管理
 * 2. 任务栈管理
 * 3. Activity 启动和销毁
 */
class NanoActivityManagerService(
    private val context: Context,
    private val packageManager: NanoPackageManagerService
) : NanoBinder(), INanoActivityManager {

    companion object {
        private const val TAG = "NanoActivityManagerService"
        private const val MAX_TASK_HISTORY = 20
    }

    /** 下一个任务 ID */
    private val nextTaskId = AtomicInteger(1)

    /** 所有任务栈（按最近使用排序） */
    private val tasks = mutableListOf<NanoTaskRecord>()

    /** Activity 记录映射（token -> record） */
    private val activityRecords = ConcurrentHashMap<String, NanoActivityRecord>()

    /** 当前前台任务 */
    private var foregroundTaskId: Int? = null

    /** 锁对象 */
    private val lock = Any()

    init {
        attachInterface(this, INanoActivityManager.DESCRIPTOR)
        NanoLog.d(TAG, "NanoActivityManagerService created")
    }

    // ==================== Activity 启动 ====================

    override fun startActivity(
        packageName: String,
        activityName: String,
        flags: Int
    ): String? {
        synchronized(lock) {
            NanoLog.i(TAG, "Starting activity: $packageName/$activityName")

            // 1. 查询 Activity 信息
            val activityInfo = packageManager.getActivityInfo(packageName, activityName)
            if (activityInfo == null) {
                NanoLog.e(TAG, "Activity not found: $packageName/$activityName")
                return null
            }

            // 2. 创建 ActivityRecord
            val record = NanoActivityRecord(
                activityInfo = activityInfo
            )
            activityRecords[record.token] = record

            // 3. 确定任务栈
            val task = findOrCreateTask(activityInfo)

            // 4. 处理启动模式
            when (activityInfo.launchMode) {
                com.nano.framework.pm.NanoActivityInfo.LaunchMode.SINGLE_TASK -> {
                    // 如果任务中已存在，清除其上的所有 Activity
                    val existing = task.findActivityByComponent(packageName, activityName)
                    if (existing != null) {
                        NanoLog.d(TAG, "Activity exists in task, clearing top")
                        clearActivityAbove(task, existing)
                        resumeActivity(existing)
                        return existing.token
                    }
                }
                com.nano.framework.pm.NanoActivityInfo.LaunchMode.SINGLE_TOP -> {
                    // 如果在栈顶，复用
                    val top = task.getTopActivity()
                    if (top != null && top.getFullName() == record.getFullName()) {
                        NanoLog.d(TAG, "Activity is already on top, reusing")
                        return top.token
                    }
                }
                else -> {
                    // STANDARD 和 SINGLE_INSTANCE 模式，创建新实例
                }
            }

            // 5. 暂停当前 Activity
            pauseCurrentActivity()

            // 6. 添加到任务栈
            task.addActivity(record)
            foregroundTaskId = task.taskId

            // 7. 启动 Activity 生命周期
            startActivityLifecycle(record)

            NanoLog.i(TAG, "Activity started: ${record.token}")
            return record.token
        }
    }

    /**
     * 查找或创建任务栈
     */
    private fun findOrCreateTask(activityInfo: com.nano.framework.pm.NanoActivityInfo): NanoTaskRecord {
        // 根据 taskAffinity 查找现有任务
        val existingTask = tasks.find { it.affinity == activityInfo.taskAffinity }
        if (existingTask != null) {
            // 将任务移到最前
            tasks.remove(existingTask)
            tasks.add(0, existingTask)
            return existingTask
        }

        // 创建新任务
        val newTask = NanoTaskRecord(
            taskId = nextTaskId.getAndIncrement(),
            affinity = activityInfo.taskAffinity
        )
        tasks.add(0, newTask)

        // 限制任务栈数量
        if (tasks.size > MAX_TASK_HISTORY) {
            val removedTask = tasks.removeLast()
            NanoLog.d(TAG, "Removed old task: ${removedTask.taskId}")
        }

        NanoLog.d(TAG, "Created new task: ${newTask.taskId}")
        return newTask
    }

    /**
     * 清除指定 Activity 之上的所有 Activity
     */
    private fun clearActivityAbove(task: NanoTaskRecord, record: NanoActivityRecord) {
        val index = task.activities.indexOf(record)
        if (index < 0) return

        // 销毁上面的所有 Activity
        val toRemove = task.activities.subList(index + 1, task.activities.size)
        toRemove.forEach { destroyActivity(it) }
        toRemove.clear()
    }

    /**
     * 暂停当前 Activity
     */
    private fun pauseCurrentActivity() {
        val currentTask = foregroundTaskId?.let { getTaskInfo(it) }
        val currentActivity = currentTask?.getTopActivity()

        if (currentActivity != null && currentActivity.state == NanoActivityRecord.LifecycleState.RESUMED) {
            pauseActivity(currentActivity)
        }
    }

    /**
     * 启动 Activity 生命周期
     */
    private fun startActivityLifecycle(record: NanoActivityRecord) {
        // onCreate
        record.state = NanoActivityRecord.LifecycleState.CREATED
        NanoLog.d(TAG, "Activity onCreate: ${record.getFullName()}")

        // onStart
        record.state = NanoActivityRecord.LifecycleState.STARTED
        NanoLog.d(TAG, "Activity onStart: ${record.getFullName()}")

        // onResume
        resumeActivity(record)
    }

    /**
     * 恢复 Activity
     */
    private fun resumeActivity(record: NanoActivityRecord) {
        record.state = NanoActivityRecord.LifecycleState.RESUMED
        NanoLog.d(TAG, "Activity onResume: ${record.getFullName()}")
    }

    /**
     * 暂停 Activity
     */
    private fun pauseActivity(record: NanoActivityRecord) {
        record.state = NanoActivityRecord.LifecycleState.PAUSED
        NanoLog.d(TAG, "Activity onPause: ${record.getFullName()}")
    }

    /**
     * 停止 Activity
     */
    private fun stopActivity(record: NanoActivityRecord) {
        record.state = NanoActivityRecord.LifecycleState.STOPPED
        NanoLog.d(TAG, "Activity onStop: ${record.getFullName()}")
    }

    /**
     * 销毁 Activity
     */
    private fun destroyActivity(record: NanoActivityRecord) {
        record.state = NanoActivityRecord.LifecycleState.DESTROYED
        activityRecords.remove(record.token)
        NanoLog.d(TAG, "Activity onDestroy: ${record.getFullName()}")
    }

    // ==================== Activity 结束 ====================

    override fun finishActivity(token: String): Boolean {
        synchronized(lock) {
            val record = activityRecords[token]
            if (record == null) {
                NanoLog.w(TAG, "Activity not found: $token")
                return false
            }

            val task = record.task
            if (task == null) {
                NanoLog.w(TAG, "Activity has no task: $token")
                return false
            }

            NanoLog.i(TAG, "Finishing activity: ${record.getFullName()}")

            // 1. 从任务栈中移除
            task.removeActivity(record)

            // 2. 销毁 Activity
            destroyActivity(record)

            // 3. 如果任务为空，移除任务
            if (task.isEmpty()) {
                tasks.remove(task)
                if (foregroundTaskId == task.taskId) {
                    foregroundTaskId = null
                }
                NanoLog.d(TAG, "Task is empty, removed: ${task.taskId}")
            } else {
                // 4. 恢复下一个 Activity
                val nextActivity = task.getTopActivity()
                if (nextActivity != null && foregroundTaskId == task.taskId) {
                    resumeActivity(nextActivity)
                }
            }

            return true
        }
    }

    // ==================== 查询接口 ====================

    override fun getForegroundActivity(): NanoActivityRecord? {
        synchronized(lock) {
            val task = foregroundTaskId?.let { getTaskInfo(it) }
            return task?.getTopActivity()
        }
    }

    override fun getRunningTasks(maxNum: Int): List<NanoTaskRecord> {
        synchronized(lock) {
            return tasks.take(maxNum).toList()
        }
    }

    override fun getTaskInfo(taskId: Int): NanoTaskRecord? {
        synchronized(lock) {
            return tasks.find { it.taskId == taskId }
        }
    }

    override fun moveTaskToFront(taskId: Int): Boolean {
        synchronized(lock) {
            val task = tasks.find { it.taskId == taskId }
            if (task == null) {
                NanoLog.w(TAG, "Task not found: $taskId")
                return false
            }

            // 暂停当前任务
            pauseCurrentActivity()

            // 移到前台
            tasks.remove(task)
            tasks.add(0, task)
            foregroundTaskId = taskId

            // 恢复栈顶 Activity
            task.getTopActivity()?.let { resumeActivity(it) }

            NanoLog.i(TAG, "Moved task to front: $taskId")
            return true
        }
    }

    override fun removeTask(taskId: Int): Boolean {
        synchronized(lock) {
            val task = tasks.find { it.taskId == taskId }
            if (task == null) {
                NanoLog.w(TAG, "Task not found: $taskId")
                return false
            }

            // 销毁所有 Activity
            task.activities.forEach { destroyActivity(it) }

            // 移除任务
            tasks.remove(task)

            if (foregroundTaskId == taskId) {
                foregroundTaskId = null
                // 恢复下一个任务
                tasks.firstOrNull()?.let {
                    foregroundTaskId = it.taskId
                    it.getTopActivity()?.let { activity -> resumeActivity(activity) }
                }
            }

            NanoLog.i(TAG, "Removed task: $taskId")
            return true
        }
    }

    // ==================== Binder Implementation ====================

    override fun onTransact(
        code: Int,
        data: NanoParcel,
        reply: NanoParcel?,
        flags: Int
    ): Boolean {
        when (code) {
            INanoActivityManager.TRANSACTION_START_ACTIVITY -> {
                data.enforceInterface(INanoActivityManager.DESCRIPTOR)
                val packageName = data.readString() ?: return false
                val activityName = data.readString() ?: return false
                val startFlags = data.readInt()
                val token = startActivity(packageName, activityName, startFlags)
                reply?.writeString(token)
                return true
            }

            INanoActivityManager.TRANSACTION_FINISH_ACTIVITY -> {
                data.enforceInterface(INanoActivityManager.DESCRIPTOR)
                val token = data.readString() ?: return false
                val result = finishActivity(token)
                reply?.writeBoolean(result)
                return true
            }

            INanoActivityManager.TRANSACTION_GET_RUNNING_TASKS -> {
                data.enforceInterface(INanoActivityManager.DESCRIPTOR)
                val maxNum = data.readInt()
                val runningTasks = getRunningTasks(maxNum)
                reply?.writeInt(runningTasks.size)
                runningTasks.forEach {
                    reply?.writeInt(it.taskId)
                    reply?.writeString(it.affinity)
                    reply?.writeInt(it.size())
                }
                return true
            }
        }

        return false
    }

    /**
     * 系统就绪回调
     */
    fun systemReady() {
        NanoLog.i(TAG, "ActivityManagerService is ready")
    }

    /**
     * 启动 Home Activity
     */
    fun startHomeActivity() {
        val launcherActivities = packageManager.getLauncherActivities()
        if (launcherActivities.isEmpty()) {
            NanoLog.w(TAG, "No launcher activity found")
            return
        }

        val launcher = launcherActivities.first()
        NanoLog.i(TAG, "Starting Home Activity: ${launcher.getFullName()}")
        startActivity(launcher.packageName, launcher.name)
    }
}
