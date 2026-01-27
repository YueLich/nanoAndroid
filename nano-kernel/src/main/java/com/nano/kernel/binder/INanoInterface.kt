package com.nano.kernel.binder

/**
 * INanoInterface - Binder 服务接口基类
 *
 * 所有通过 Binder 暴露的服务接口都需要继承此接口。
 * 这模拟了 Android 中的 IInterface 接口。
 */
interface INanoInterface {

    /**
     * 获取底层 Binder 对象
     */
    fun asBinder(): NanoBinder
}
