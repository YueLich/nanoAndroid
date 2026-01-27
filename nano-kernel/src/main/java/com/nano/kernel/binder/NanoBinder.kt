package com.nano.kernel.binder

import com.nano.kernel.NanoLog

/**
 * NanoBinder - 简化版 Binder 实现
 *
 * 真实 Android 的 Binder 通过内核驱动实现跨进程通信。
 * NanoBinder 通过内存调用模拟，但保持相同的接口设计。
 *
 * 核心概念：
 * 1. 事务（Transaction）：一次 RPC 调用
 * 2. 事务码（Transaction Code）：标识调用的方法
 * 3. Parcel：数据序列化容器
 */
abstract class NanoBinder : INanoInterface {

    companion object {
        private const val TAG = "NanoBinder"

        /** 第一个用户自定义的事务码 */
        const val FIRST_CALL_TRANSACTION = 1

        /** 最后一个用户自定义的事务码 */
        const val LAST_CALL_TRANSACTION = 0x00ffffff

        /** 异步调用标志 */
        const val FLAG_ONEWAY = 1
    }

    /** 服务描述符（唯一标识） */
    private var descriptor: String? = null

    /** 关联的接口实例 */
    private var owner: INanoInterface? = null

    /**
     * 附加服务接口
     *
     * @param owner 接口实现
     * @param descriptor 服务描述符（通常是接口的全限定名）
     */
    fun attachInterface(owner: INanoInterface, descriptor: String) {
        this.owner = owner
        this.descriptor = descriptor
    }

    /**
     * 查询本地接口
     *
     * 如果调用方和服务方在同一"进程"，可以直接返回本地实现，
     * 避免经过 Binder 调用的开销。
     *
     * @param descriptor 服务描述符
     * @return 本地接口实例，如果不匹配则返回 null
     */
    fun queryLocalInterface(descriptor: String): INanoInterface? {
        return if (this.descriptor == descriptor) owner else null
    }

    /**
     * 获取服务描述符
     */
    fun getInterfaceDescriptor(): String? = descriptor

    /**
     * 处理事务
     *
     * 子类需要重写此方法来处理具体的 RPC 调用。
     *
     * @param code 事务码，标识调用的方法
     * @param data 输入数据
     * @param reply 输出数据（FLAG_ONEWAY 时为 null）
     * @param flags 标志位（0=同步，FLAG_ONEWAY=异步）
     * @return 是否处理成功
     */
    protected abstract fun onTransact(
        code: Int,
        data: NanoParcel,
        reply: NanoParcel?,
        flags: Int
    ): Boolean

    /**
     * 执行事务
     *
     * 这是对外的调用入口，会进行日志记录和异常处理。
     */
    fun transact(code: Int, data: NanoParcel, reply: NanoParcel?, flags: Int): Boolean {
        NanoLog.v(TAG, "transact: code=$code, flags=$flags, descriptor=$descriptor")

        return try {
            onTransact(code, data, reply, flags)
        } catch (e: Exception) {
            NanoLog.e(TAG, "transact failed: code=$code", e)
            false
        }
    }

    /**
     * 返回自身作为 Binder 对象
     */
    override fun asBinder(): NanoBinder = this

    /**
     * 判断 Binder 是否存活
     */
    fun isBinderAlive(): Boolean = true

    /**
     * 判断 Binder 是否在本地
     */
    fun pingBinder(): Boolean = true
}
