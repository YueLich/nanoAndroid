package com.nano.android.shell

import android.util.Log
import com.nano.kernel.NanoLog

/**
 * AndroidLogOutput - 将 NanoLog 桥接到 Android Log
 */
class AndroidLogOutput : NanoLog.LogOutput {

    override fun log(
        level: NanoLog.Level,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        val formattedTag = "[Nano] $tag"

        when (level) {
            NanoLog.Level.DEBUG -> {
                if (throwable != null) {
                    Log.d(formattedTag, message, throwable)
                } else {
                    Log.d(formattedTag, message)
                }
            }
            NanoLog.Level.INFO -> {
                if (throwable != null) {
                    Log.i(formattedTag, message, throwable)
                } else {
                    Log.i(formattedTag, message)
                }
            }
            NanoLog.Level.WARN -> {
                if (throwable != null) {
                    Log.w(formattedTag, message, throwable)
                } else {
                    Log.w(formattedTag, message)
                }
            }
            NanoLog.Level.ERROR -> {
                if (throwable != null) {
                    Log.e(formattedTag, message, throwable)
                } else {
                    Log.e(formattedTag, message)
                }
            }
            NanoLog.Level.VERBOSE -> {
                if (throwable != null) {
                    Log.v(formattedTag, message, throwable)
                } else {
                    Log.v(formattedTag, message)
                }
            }
        }
    }
}
