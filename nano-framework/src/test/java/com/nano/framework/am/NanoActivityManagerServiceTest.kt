package com.nano.framework.am

import com.nano.framework.common.NanoContext
import com.nano.framework.common.DefaultNanoContext
import com.nano.framework.pm.NanoActivityInfo
import com.nano.framework.pm.NanoApplicationInfo
import com.nano.framework.pm.NanoPackageManagerService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * NanoActivityManagerService 单元测试
 */
class NanoActivityManagerServiceTest {

    private lateinit var context: NanoContext
    private lateinit var packageManager: NanoPackageManagerService
    private lateinit var activityManager: NanoActivityManagerService

    @Before
    fun setUp() {
        context = DefaultNanoContext()
        packageManager = NanoPackageManagerService(context)
        activityManager = NanoActivityManagerService(context, packageManager)

        // 注册测试应用和 Activity
        registerTestApp()
    }

    private fun registerTestApp() {
        packageManager.registerApplication(
            NanoApplicationInfo(
                packageName = "com.test.app",
                name = "Test App"
            )
        )

        packageManager.registerActivity(
            NanoActivityInfo(
                name = "com.test.app.MainActivity",
                packageName = "com.test.app",
                label = "Main Activity"
            )
        )

        packageManager.registerActivity(
            NanoActivityInfo(
                name = "com.test.app.SecondActivity",
                packageName = "com.test.app",
                label = "Second Activity"
            )
        )

        packageManager.registerActivity(
            NanoActivityInfo(
                name = "com.test.app.SingleTopActivity",
                packageName = "com.test.app",
                label = "Single Top Activity",
                launchMode = NanoActivityInfo.LaunchMode.SINGLE_TOP
            )
        )

        packageManager.registerActivity(
            NanoActivityInfo(
                name = "com.test.app.SingleTaskActivity",
                packageName = "com.test.app",
                label = "Single Task Activity",
                launchMode = NanoActivityInfo.LaunchMode.SINGLE_TASK
            )
        )
    }

    @Test
    fun `test start activity`() {
        val token = activityManager.startActivity(
            packageName = "com.test.app",
            activityName = "com.test.app.MainActivity"
        )

        assertNotNull(token)

        val foreground = activityManager.getForegroundActivity()
        assertNotNull(foreground)
        assertEquals("com.test.app.MainActivity", foreground?.activityInfo?.name)
        assertEquals(NanoActivityRecord.LifecycleState.RESUMED, foreground?.state)
    }

    @Test
    fun `test start non-existent activity`() {
        val token = activityManager.startActivity(
            packageName = "com.test.app",
            activityName = "com.test.app.NonExistent"
        )

        assertNull(token)
    }

    @Test
    fun `test start multiple activities creates task stack`() {
        // 启动第一个 Activity
        val token1 = activityManager.startActivity(
            "com.test.app",
            "com.test.app.MainActivity"
        )
        assertNotNull(token1)

        // 启动第二个 Activity
        val token2 = activityManager.startActivity(
            "com.test.app",
            "com.test.app.SecondActivity"
        )
        assertNotNull(token2)

        // 检查前台 Activity
        val foreground = activityManager.getForegroundActivity()
        assertEquals("com.test.app.SecondActivity", foreground?.activityInfo?.name)

        // 检查任务栈
        val tasks = activityManager.getRunningTasks(10)
        assertEquals(1, tasks.size)

        val task = tasks.first()
        assertEquals(2, task.size())
        assertEquals("com.test.app.MainActivity", task.activities[0].activityInfo.name)
        assertEquals("com.test.app.SecondActivity", task.activities[1].activityInfo.name)
    }

    @Test
    fun `test finish activity`() {
        val token = activityManager.startActivity(
            "com.test.app",
            "com.test.app.MainActivity"
        )
        assertNotNull(token!!)

        val result = activityManager.finishActivity(token)
        assertTrue(result)

        val foreground = activityManager.getForegroundActivity()
        assertNull(foreground)
    }

    @Test
    fun `test finish activity in stack resumes previous`() {
        // 启动两个 Activity
        activityManager.startActivity("com.test.app", "com.test.app.MainActivity")
        val token2 = activityManager.startActivity("com.test.app", "com.test.app.SecondActivity")
        assertNotNull(token2!!)

        // 结束第二个
        activityManager.finishActivity(token2)

        // 第一个应该恢复到前台
        val foreground = activityManager.getForegroundActivity()
        assertEquals("com.test.app.MainActivity", foreground?.activityInfo?.name)
        assertEquals(NanoActivityRecord.LifecycleState.RESUMED, foreground?.state)
    }

    @Test
    fun `test activity lifecycle states`() {
        val token = activityManager.startActivity(
            "com.test.app",
            "com.test.app.MainActivity"
        )
        assertNotNull(token!!)

        val foreground = activityManager.getForegroundActivity()
        assertEquals(NanoActivityRecord.LifecycleState.RESUMED, foreground?.state)

        // 启动另一个 Activity 会暂停当前的
        activityManager.startActivity("com.test.app", "com.test.app.SecondActivity")

        val tasks = activityManager.getRunningTasks(10)
        val task = tasks.first()
        val firstActivity = task.activities[0]
        assertEquals(NanoActivityRecord.LifecycleState.PAUSED, firstActivity.state)
    }

    @Test
    fun `test single top launch mode`() {
        // 启动 SingleTop Activity
        val token1 = activityManager.startActivity(
            "com.test.app",
            "com.test.app.SingleTopActivity"
        )
        assertNotNull(token1!!)

        // 再次启动同样的 Activity（它已经在栈顶）
        val token2 = activityManager.startActivity(
            "com.test.app",
            "com.test.app.SingleTopActivity"
        )

        // 应该复用同一个实例
        assertEquals(token1, token2)

        val tasks = activityManager.getRunningTasks(10)
        val task = tasks.first()
        assertEquals(1, task.size())
    }

    @Test
    fun `test single task launch mode`() {
        // 启动 MainActivity
        activityManager.startActivity("com.test.app", "com.test.app.MainActivity")

        // 启动 SingleTask Activity
        val token1 = activityManager.startActivity(
            "com.test.app",
            "com.test.app.SingleTaskActivity"
        )
        assertNotNull(token1!!)

        // 启动另一个 Activity
        activityManager.startActivity("com.test.app", "com.test.app.SecondActivity")

        // 再次启动 SingleTask Activity（它已经在栈中）
        val token2 = activityManager.startActivity(
            "com.test.app",
            "com.test.app.SingleTaskActivity"
        )

        // 应该复用同一个实例，并清除其上的 Activity
        assertEquals(token1, token2)

        val tasks = activityManager.getRunningTasks(10)
        val task = tasks.first()
        assertEquals(2, task.size())  // MainActivity + SingleTaskActivity
        assertEquals("com.test.app.SingleTaskActivity", task.getTopActivity()?.activityInfo?.name)
    }

    @Test
    fun `test get running tasks`() {
        activityManager.startActivity("com.test.app", "com.test.app.MainActivity")

        val tasks = activityManager.getRunningTasks(10)
        assertEquals(1, tasks.size)

        val task = tasks.first()
        assertTrue(task.taskId > 0)
        assertEquals("com.test.app", task.affinity)
    }

    @Test
    fun `test get task info`() {
        activityManager.startActivity("com.test.app", "com.test.app.MainActivity")

        val tasks = activityManager.getRunningTasks(1)
        val taskId = tasks.first().taskId

        val taskInfo = activityManager.getTaskInfo(taskId)
        assertNotNull(taskInfo)
        assertEquals(taskId, taskInfo?.taskId)
    }

    @Test
    fun `test move task to front`() {
        // 启动第一个任务
        activityManager.startActivity("com.test.app", "com.test.app.MainActivity")
        val task1 = activityManager.getRunningTasks(1).first()

        // 启动 Launcher（不同的 affinity，创建新任务）
        activityManager.startActivity("com.nano.android", "com.nano.android.shell.NanoShellActivity")

        // 将第一个任务移到前台
        val result = activityManager.moveTaskToFront(task1.taskId)
        assertTrue(result)

        val foreground = activityManager.getForegroundActivity()
        assertEquals("com.test.app.MainActivity", foreground?.activityInfo?.name)
    }

    @Test
    fun `test remove task`() {
        activityManager.startActivity("com.test.app", "com.test.app.MainActivity")
        val task = activityManager.getRunningTasks(1).first()

        val result = activityManager.removeTask(task.taskId)
        assertTrue(result)

        val tasks = activityManager.getRunningTasks(10)
        assertEquals(0, tasks.size)

        val foreground = activityManager.getForegroundActivity()
        assertNull(foreground)
    }

    @Test
    fun `test start home activity`() {
        activityManager.startHomeActivity()

        val foreground = activityManager.getForegroundActivity()
        assertNotNull(foreground)
        assertTrue(foreground?.activityInfo?.isLauncher == true)
    }

    @Test
    fun `test activity record properties`() {
        val activityInfo = NanoActivityInfo(
            name = "TestActivity",
            packageName = "com.test"
        )
        val record = NanoActivityRecord(activityInfo = activityInfo)

        assertNotNull(record.token)
        assertEquals(NanoActivityRecord.LifecycleState.INITIALIZED, record.state)
        assertFalse(record.isVisible())
        assertFalse(record.isForeground())

        record.state = NanoActivityRecord.LifecycleState.RESUMED
        assertTrue(record.isVisible())
        assertTrue(record.isForeground())
    }

    @Test
    fun `test task record operations`() {
        val task = NanoTaskRecord(
            taskId = 1,
            affinity = "com.test"
        )

        assertTrue(task.isEmpty())
        assertEquals(0, task.size())
        assertNull(task.getTopActivity())
        assertNull(task.getRootActivity())

        val activityInfo = NanoActivityInfo(
            name = "TestActivity",
            packageName = "com.test"
        )
        val record1 = NanoActivityRecord(activityInfo = activityInfo)
        val record2 = NanoActivityRecord(activityInfo = activityInfo)

        task.addActivity(record1)
        task.addActivity(record2)

        assertFalse(task.isEmpty())
        assertEquals(2, task.size())
        assertEquals(record1, task.getRootActivity())
        assertEquals(record2, task.getTopActivity())

        val found = task.findActivity(record1.token)
        assertEquals(record1, found)

        task.removeActivity(record1)
        assertEquals(1, task.size())
    }
}
