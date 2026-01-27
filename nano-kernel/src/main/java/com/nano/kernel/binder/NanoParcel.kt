package com.nano.kernel.binder

import java.util.ArrayDeque

/**
 * NanoParcel - 数据序列化容器
 *
 * 模拟 Android Parcel 的读写操作。
 * Parcel 是 Binder 通信中数据传输的载体。
 *
 * 设计说明：
 * - 使用 List 存储数据，按写入顺序排列
 * - 读取时按顺序从头开始读
 * - 使用对象池复用 Parcel 实例，减少内存分配
 */
class NanoParcel private constructor() {

    companion object {
        private const val MAX_POOL_SIZE = 10
        private val pool = ArrayDeque<NanoParcel>()

        /**
         * 从对象池获取 Parcel 实例
         */
        fun obtain(): NanoParcel {
            synchronized(pool) {
                return pool.pollFirst() ?: NanoParcel()
            }
        }
    }

    /** 数据存储 */
    private val data = mutableListOf<Any?>()

    /** 当前读取位置 */
    private var readPosition = 0

    /**
     * 回收到对象池
     */
    fun recycle() {
        data.clear()
        readPosition = 0

        synchronized(pool) {
            if (pool.size < MAX_POOL_SIZE) {
                pool.addLast(this)
            }
        }
    }

    /**
     * 重置数据位置
     */
    fun setDataPosition(position: Int) {
        readPosition = position
    }

    /**
     * 获取当前数据位置
     */
    fun dataPosition(): Int = readPosition

    /**
     * 获取数据大小
     */
    fun dataSize(): Int = data.size

    // ==================== 写入操作 ====================

    fun writeInt(value: Int) {
        data.add(value)
    }

    fun writeLong(value: Long) {
        data.add(value)
    }

    fun writeFloat(value: Float) {
        data.add(value)
    }

    fun writeDouble(value: Double) {
        data.add(value)
    }

    fun writeBoolean(value: Boolean) {
        data.add(value)
    }

    fun writeString(value: String?) {
        data.add(value)
    }

    fun writeByteArray(value: ByteArray?) {
        data.add(value?.copyOf())
    }

    /**
     * 写入接口描述符（用于验证调用方和服务方的一致性）
     */
    fun writeInterfaceToken(descriptor: String) {
        data.add(descriptor)
    }

    /**
     * 写入 Parcelable 对象
     */
    fun writeParcelable(p: NanoParcelable?) {
        if (p == null) {
            writeString(null)
        } else {
            writeString(p.javaClass.name)
            p.writeToParcel(this)
        }
    }

    /**
     * 写入 Bundle
     */
    fun writeBundle(bundle: NanoBundle?) {
        data.add(bundle?.copy())
    }

    /**
     * 写入 Binder 对象
     */
    fun writeStrongBinder(binder: NanoBinder?) {
        data.add(binder)
    }

    /**
     * 写入 List
     */
    fun writeList(list: List<*>?) {
        if (list == null) {
            writeInt(-1)
        } else {
            writeInt(list.size)
            list.forEach { data.add(it) }
        }
    }

    /**
     * 写入 Map
     */
    fun writeMap(map: Map<*, *>?) {
        if (map == null) {
            writeInt(-1)
        } else {
            writeInt(map.size)
            map.forEach { (key, value) ->
                data.add(key)
                data.add(value)
            }
        }
    }

    // ==================== 读取操作 ====================

    fun readInt(): Int {
        return data.getOrNull(readPosition++)?.let { it as? Int } ?: 0
    }

    fun readLong(): Long {
        return data.getOrNull(readPosition++)?.let { it as? Long } ?: 0L
    }

    fun readFloat(): Float {
        return data.getOrNull(readPosition++)?.let { it as? Float } ?: 0f
    }

    fun readDouble(): Double {
        return data.getOrNull(readPosition++)?.let { it as? Double } ?: 0.0
    }

    fun readBoolean(): Boolean {
        return data.getOrNull(readPosition++)?.let { it as? Boolean } ?: false
    }

    fun readString(): String? {
        return data.getOrNull(readPosition++) as? String
    }

    fun readByteArray(): ByteArray? {
        return (data.getOrNull(readPosition++) as? ByteArray)?.copyOf()
    }

    /**
     * 验证接口描述符
     */
    fun enforceInterface(descriptor: String) {
        val token = data.getOrNull(readPosition++) as? String
        require(token == descriptor) {
            "Interface descriptor mismatch: expected=$descriptor, actual=$token"
        }
    }

    /**
     * 读取 Parcelable 对象
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : NanoParcelable> readParcelable(loader: ClassLoader? = null): T? {
        val className = readString() ?: return null
        return try {
            val clazz = Class.forName(className, true, loader ?: javaClass.classLoader)
            val creator = clazz.getField("CREATOR").get(null) as? NanoParcelable.Creator<T>
            creator?.createFromParcel(this)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取 Bundle
     */
    fun readBundle(): NanoBundle? {
        return (data.getOrNull(readPosition++) as? NanoBundle)?.copy()
    }

    /**
     * 读取 Binder 对象
     */
    fun readStrongBinder(): NanoBinder? {
        return data.getOrNull(readPosition++) as? NanoBinder
    }

    /**
     * 读取 List
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> readList(): List<T>? {
        val size = readInt()
        if (size < 0) return null

        return (0 until size).map {
            data.getOrNull(readPosition++) as T
        }
    }

    /**
     * 读取 Map
     */
    @Suppress("UNCHECKED_CAST")
    fun <K, V> readMap(): Map<K, V>? {
        val size = readInt()
        if (size < 0) return null

        return (0 until size).associate {
            val key = data.getOrNull(readPosition++) as K
            val value = data.getOrNull(readPosition++) as V
            key to value
        }
    }
}

/**
 * NanoParcelable - 可序列化接口
 *
 * 类似 Android 的 Parcelable 接口
 */
interface NanoParcelable {

    /**
     * 将对象写入 Parcel
     */
    fun writeToParcel(parcel: NanoParcel)

    /**
     * 创建者接口
     */
    interface Creator<T : NanoParcelable> {
        fun createFromParcel(parcel: NanoParcel): T
    }
}

/**
 * NanoBundle - 键值对数据容器
 *
 * 类似 Android 的 Bundle，用于在组件间传递数据
 */
class NanoBundle(
    private val data: MutableMap<String, Any?> = mutableMapOf()
) {

    fun putInt(key: String, value: Int) {
        data[key] = value
    }

    fun putLong(key: String, value: Long) {
        data[key] = value
    }

    fun putFloat(key: String, value: Float) {
        data[key] = value
    }

    fun putDouble(key: String, value: Double) {
        data[key] = value
    }

    fun putBoolean(key: String, value: Boolean) {
        data[key] = value
    }

    fun putString(key: String, value: String?) {
        data[key] = value
    }

    fun putParcelable(key: String, value: NanoParcelable?) {
        data[key] = value
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return data[key] as? Int ?: defaultValue
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return data[key] as? Long ?: defaultValue
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return data[key] as? Float ?: defaultValue
    }

    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return data[key] as? Double ?: defaultValue
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return data[key] as? Boolean ?: defaultValue
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return data[key] as? String ?: defaultValue
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : NanoParcelable> getParcelable(key: String): T? {
        return data[key] as? T
    }

    fun containsKey(key: String): Boolean = data.containsKey(key)

    fun remove(key: String) {
        data.remove(key)
    }

    fun clear() {
        data.clear()
    }

    fun isEmpty(): Boolean = data.isEmpty()

    fun keySet(): Set<String> = data.keys

    fun copy(): NanoBundle = NanoBundle(data.toMutableMap())

    override fun toString(): String = "NanoBundle($data)"
}
