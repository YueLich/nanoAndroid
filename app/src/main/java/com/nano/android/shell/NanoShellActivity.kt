package com.nano.android.shell

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.nano.kernel.NanoLog

/**
 * NanoAndroid Shell Activity
 *
 * 这是 NanoAndroid 的主 Activity，作为所有 Nano 窗口的容器。
 * 它将真实 Android 的 View 系统与 NanoAndroid 的 WMS 桥接起来。
 */
class NanoShellActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NanoShellActivity"
    }

    // 窗口容器，所有 Nano 窗口都会添加到这里
    private lateinit var windowContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NanoLog.i(TAG, "NanoShellActivity onCreate")

        // 创建窗口容器
        windowContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(windowContainer)

        // TODO: 将窗口容器注册到 WMS
        // NanoWindowManagerService.setRootContainer(windowContainer)

        // TODO: 启动 Launcher
        // NanoActivityManagerService.startHomeActivity()

        NanoLog.i(TAG, "NanoShellActivity ready")
    }

    override fun onResume() {
        super.onResume()
        NanoLog.i(TAG, "NanoShellActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        NanoLog.i(TAG, "NanoShellActivity onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        NanoLog.i(TAG, "NanoShellActivity onDestroy")
    }

    /**
     * 获取窗口容器
     */
    fun getWindowContainer(): FrameLayout = windowContainer
}
