package com.nano.framework.am

/**
 * NanoTaskRecord - 任务栈记录
 *
 * 一个任务栈包含一组相关的 Activity
 */
data class NanoTaskRecord(
    /** 任务 ID */
    val taskId: Int,

    /** 任务亲和性 */
    val affinity: String,

    /** Activity 栈（栈底到栈顶） */
    val activities: MutableList<NanoActivityRecord> = mutableListOf(),

    /** 创建时间 */
    val createTime: Long = System.currentTimeMillis()
) {

    /**
     * 获取栈顶 Activity
     */
    fun getTopActivity(): NanoActivityRecord? {
        return activities.lastOrNull()
    }

    /**
     * 获取栈底 Activity（Root Activity）
     */
    fun getRootActivity(): NanoActivityRecord? {
        return activities.firstOrNull()
    }

    /**
     * 添加 Activity 到栈顶
     */
    fun addActivity(record: NanoActivityRecord) {
        record.task = this
        activities.add(record)
    }

    /**
     * 移除 Activity
     */
    fun removeActivity(record: NanoActivityRecord): Boolean {
        return activities.remove(record)
    }

    /**
     * 检查任务是否为空
     */
    fun isEmpty(): Boolean {
        return activities.isEmpty()
    }

    /**
     * 获取任务中的 Activity 数量
     */
    fun size(): Int {
        return activities.size
    }

    /**
     * 查找指定 Activity
     */
    fun findActivity(token: String): NanoActivityRecord? {
        return activities.find { it.token == token }
    }

    /**
     * 查找指定组件的 Activity
     */
    fun findActivityByComponent(packageName: String, activityName: String): NanoActivityRecord? {
        val fullName = "$packageName/$activityName"
        return activities.find { it.getFullName() == fullName }
    }

    override fun toString(): String {
        return "TaskRecord(id=$taskId, affinity=$affinity, size=${activities.size})"
    }
}
