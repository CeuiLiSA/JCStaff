package ceui.lisa.jcstaff.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * 可存储对象的接口
 * 实现此接口的对象可以被 ObjectStore 管理
 */
interface Storable {
    val storeKey: StoreKey
}

/**
 * 存储键，由 ID 和类型组成
 */
data class StoreKey(
    val id: Long,
    val type: StoreType
)

/**
 * 存储类型枚举
 */
enum class StoreType {
    ILLUST,
    USER,
    NOVEL,
    TAG
}

/**
 * 全局对象存储池
 *
 * 设计模式：
 * - 单例模式：全局唯一实例
 * - 对象池模式：复用对象，避免重复创建
 * - 观察者模式：通过 StateFlow 实现数据变更通知
 *
 * 特点：
 * - 线程安全：使用 ConcurrentHashMap
 * - 响应式：使用 StateFlow，可直接在 Compose 中观察
 * - 类型安全：泛型方法确保类型安全
 * - 自动更新：列表和详情共享同一数据源
 */
object ObjectStore {

    private val store = ConcurrentHashMap<StoreKey, MutableStateFlow<Storable>>()

    /**
     * 获取指定对象的 StateFlow
     * 如果对象不存在，返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Storable> get(key: StoreKey): StateFlow<T>? {
        return store[key]?.asStateFlow() as? StateFlow<T>
    }

    /**
     * 获取指定对象的 StateFlow
     * 便捷方法，通过 ID 和类型获取
     */
    inline fun <reified T : Storable> get(id: Long, type: StoreType): StateFlow<T>? {
        return get(StoreKey(id, type))
    }

    /**
     * 获取对象的当前值
     * 如果对象不存在，返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Storable> peek(key: StoreKey): T? {
        return store[key]?.value as? T
    }

    /**
     * 获取对象的当前值
     * 便捷方法
     */
    inline fun <reified T : Storable> peek(id: Long, type: StoreType): T? {
        return peek(StoreKey(id, type))
    }

    /**
     * 存储或更新对象
     * 如果对象已存在，更新其值；否则创建新的 StateFlow
     */
    fun <T : Storable> put(obj: T) {
        val key = obj.storeKey
        val existing = store[key]
        if (existing != null) {
            existing.update { obj }
        } else {
            store[key] = MutableStateFlow(obj)
        }
    }

    /**
     * 批量存储对象
     */
    fun <T : Storable> putAll(objects: List<T>) {
        objects.forEach { put(it) }
    }

    /**
     * 更新对象的部分属性
     * 使用 transform 函数对现有对象进行修改
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Storable> update(key: StoreKey, clazz: Class<T>, transform: (T) -> T) {
        val flow = store[key] ?: return
        flow.update { current ->
            if (clazz.isInstance(current)) {
                transform(current as T)
            } else {
                current
            }
        }
    }

    /**
     * 便捷方法：使用 reified 类型参数更新对象
     */
    inline fun <reified T : Storable> updateTyped(key: StoreKey, noinline transform: (T) -> T) {
        update(key, T::class.java, transform)
    }

    /**
     * 移除指定对象
     */
    fun remove(key: StoreKey) {
        store.remove(key)
    }

    /**
     * 清空所有存储
     */
    fun clear() {
        store.clear()
    }

    /**
     * 获取存储的对象数量
     */
    fun size(): Int = store.size

    /**
     * 检查对象是否存在
     */
    fun contains(key: StoreKey): Boolean = store.containsKey(key)
}

/**
 * 扩展函数：获取或创建 StateFlow
 * 如果对象不存在，使用 default 创建
 */
@Suppress("UNCHECKED_CAST")
fun <T : Storable> ObjectStore.getOrPut(key: StoreKey, default: () -> T): StateFlow<T> {
    val existing = get<T>(key)
    if (existing != null) return existing

    val obj = default()
    put(obj)
    return get<T>(key)!!
}
