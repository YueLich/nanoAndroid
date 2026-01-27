package com.nano.android.shell

import android.app.Application
import android.util.Log
import com.nano.framework.server.NanoSystemServer
import com.nano.kernel.NanoLog

/**
 * NanoAndroid 应用入口
 *
 * 负责初始化整个 NanoAndroid 系统，启动 NanoSystemServer
 */
class NanoApplication : Application() {

    companion object {
        private const val TAG = "NanoApplication"

        @Volatile
        private var instance: NanoApplication? = null

        fun getInstance(): NanoApplication =
            instance ?: throw IllegalStateException("NanoApplication not initialized")
    }

    // 系统服务器
    private lateinit var systemServer: NanoSystemServer

    override fun onCreate() {
        super.onCreate()
        instance = this

        NanoLog.i(TAG, "NanoAndroid starting...")

        // 初始化系统服务
        initializeSystemServer()
    }

    /**
     * 初始化系统服务器
     */
    private fun initializeSystemServer() {
        NanoLog.i(TAG, "Initializing NanoSystemServer...")

        // TODO: 创建并启动 NanoSystemServer
        // systemServer = NanoSystemServer(this)
        // systemServer.run()

        NanoLog.i(TAG, "NanoSystemServer initialized")
    }

    override fun onTerminate() {
        super.onTerminate()
        NanoLog.i(TAG, "NanoAndroid terminating...")
    }
}
