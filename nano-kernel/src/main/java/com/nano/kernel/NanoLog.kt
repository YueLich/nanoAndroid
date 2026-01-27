package com.nano.kernel

import android.util.Log

/**
 * NanoAndroid 日志工具
 *
 * 统一的日志输出接口，方便调试和追踪
 */
object NanoLog {

    private const val PREFIX = "[Nano] "

    var isDebugEnabled = true

    /**
     * Debug 日志
     */
    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d(PREFIX + tag, message)
        }
    }

    /**
     * Info 日志
     */
    fun i(tag: String, message: String) {
        Log.i(PREFIX + tag, message)
    }

    /**
     * Warning 日志
     */
    fun w(tag: String, message: String) {
        Log.w(PREFIX + tag, message)
    }

    /**
     * Error 日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(PREFIX + tag, message, throwable)
        } else {
            Log.e(PREFIX + tag, message)
        }
    }

    /**
     * Verbose 日志
     */
    fun v(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.v(PREFIX + tag, message)
        }
    }
}
