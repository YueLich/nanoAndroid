package com.nano.kernel

import java.text.SimpleDateFormat
import java.util.*

/**
 * NanoAndroid 日志工具
 *
 * 统一的日志输出接口，方便调试和追踪
 * 纯 Kotlin 实现，不依赖 Android
 */
object NanoLog {

    private const val PREFIX = "[Nano] "
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    var isDebugEnabled = true

    /**
     * 日志级别
     */
    enum class Level(val prefix: String) {
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E"),
        VERBOSE("V")
    }

    /**
     * 日志输出接口（可替换实现）
     */
    interface LogOutput {
        fun log(level: Level, tag: String, message: String, throwable: Throwable? = null)
    }

    /**
     * 默认日志输出（控制台）
     */
    private var logOutput: LogOutput = object : LogOutput {
        override fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
            val timestamp = dateFormat.format(Date())
            val formattedTag = PREFIX + tag
            val logLine = "$timestamp ${level.prefix}/$formattedTag: $message"

            when (level) {
                Level.ERROR -> {
                    System.err.println(logLine)
                    throwable?.printStackTrace(System.err)
                }
                else -> {
                    println(logLine)
                }
            }
        }
    }

    /**
     * 设置自定义日志输出
     */
    fun setLogOutput(output: LogOutput) {
        logOutput = output
    }

    /**
     * Debug 日志
     */
    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            logOutput.log(Level.DEBUG, tag, message)
        }
    }

    /**
     * Info 日志
     */
    fun i(tag: String, message: String) {
        logOutput.log(Level.INFO, tag, message)
    }

    /**
     * Warning 日志
     */
    fun w(tag: String, message: String) {
        logOutput.log(Level.WARN, tag, message)
    }

    /**
     * Error 日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logOutput.log(Level.ERROR, tag, message, throwable)
    }

    /**
     * Verbose 日志
     */
    fun v(tag: String, message: String) {
        if (isDebugEnabled) {
            logOutput.log(Level.VERBOSE, tag, message)
        }
    }
}
