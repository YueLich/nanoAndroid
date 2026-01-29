package com.nano.framework.pm

import com.nano.framework.common.NanoContext
import com.nano.framework.common.DefaultNanoContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * NanoPackageManagerService 单元测试
 */
class NanoPackageManagerServiceTest {

    private lateinit var context: NanoContext
    private lateinit var packageManager: NanoPackageManagerService

    @Before
    fun setUp() {
        context = DefaultNanoContext()
        packageManager = NanoPackageManagerService(context)
    }

    @Test
    fun `test get application info`() {
        // 系统应用应该已经注册
        val appInfo = packageManager.getApplicationInfo("com.nano.android")
        assertNotNull(appInfo)
        assertEquals("com.nano.android", appInfo?.packageName)
        assertEquals("NanoLauncher", appInfo?.name)
        assertTrue(appInfo?.isSystem == true)
    }

    @Test
    fun `test get application info for non-existent package`() {
        val appInfo = packageManager.getApplicationInfo("non.existent.package")
        assertNull(appInfo)
    }

    @Test
    fun `test get installed applications`() {
        val apps = packageManager.getInstalledApplications()
        assertTrue(apps.isNotEmpty())

        // 应该包含系统应用
        val systemApp = apps.find { it.packageName == "com.nano.android" }
        assertNotNull(systemApp)
    }

    @Test
    fun `test register application`() {
        val newApp = NanoApplicationInfo(
            packageName = "com.test.app",
            name = "Test App",
            versionCode = 1,
            versionName = "1.0"
        )

        packageManager.registerApplication(newApp)

        val retrieved = packageManager.getApplicationInfo("com.test.app")
        assertNotNull(retrieved)
        assertEquals("Test App", retrieved?.name)
    }

    @Test
    fun `test register activity`() {
        val activityInfo = NanoActivityInfo(
            name = "com.test.app.MainActivity",
            packageName = "com.test.app",
            label = "Main Activity"
        )

        packageManager.registerActivity(activityInfo)

        val retrieved = packageManager.getActivityInfo("com.test.app", "com.test.app.MainActivity")
        assertNotNull(retrieved)
        assertEquals("Main Activity", retrieved?.label)
    }

    @Test
    fun `test get launcher activities`() {
        val launcherActivities = packageManager.getLauncherActivities()
        assertTrue(launcherActivities.isNotEmpty())

        // 系统 Launcher 应该存在
        val systemLauncher = launcherActivities.find {
            it.packageName == "com.nano.android"
        }
        assertNotNull(systemLauncher)
        assertTrue(systemLauncher?.isLauncher == true)
    }

    @Test
    fun `test query activities for action`() {
        val activities = packageManager.queryActivitiesForAction("android.intent.action.MAIN")
        assertTrue(activities.isNotEmpty())
        assertTrue(activities.all { it.isLauncher })
    }

    @Test
    fun `test is package installed`() {
        assertTrue(packageManager.isPackageInstalled("com.nano.android"))
        assertFalse(packageManager.isPackageInstalled("non.existent.package"))
    }

    @Test
    fun `test register service`() {
        val serviceInfo = NanoServiceInfo(
            name = "com.test.app.TestService",
            packageName = "com.test.app"
        )

        packageManager.registerService(serviceInfo)
        // 服务注册后应该能够查询到（虽然当前没有公开的查询接口）
    }

    @Test
    fun `test activity launch modes`() {
        // 测试不同的启动模式
        val standardMode = NanoActivityInfo(
            name = "StandardActivity",
            packageName = "com.test",
            launchMode = NanoActivityInfo.LaunchMode.STANDARD
        )
        assertEquals(NanoActivityInfo.LaunchMode.STANDARD, standardMode.launchMode)

        val singleTopMode = NanoActivityInfo(
            name = "SingleTopActivity",
            packageName = "com.test",
            launchMode = NanoActivityInfo.LaunchMode.SINGLE_TOP
        )
        assertEquals(NanoActivityInfo.LaunchMode.SINGLE_TOP, singleTopMode.launchMode)

        val singleTaskMode = NanoActivityInfo(
            name = "SingleTaskActivity",
            packageName = "com.test",
            launchMode = NanoActivityInfo.LaunchMode.SINGLE_TASK
        )
        assertEquals(NanoActivityInfo.LaunchMode.SINGLE_TASK, singleTaskMode.launchMode)
    }

    @Test
    fun `test component info full name`() {
        val activityInfo = NanoActivityInfo(
            name = "MainActivity",
            packageName = "com.test.app"
        )
        assertEquals("com.test.app/MainActivity", activityInfo.getFullName())
    }
}
