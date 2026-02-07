package ceui.lisa.jcstaff.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 可过滤内容的统一契约
 *
 * 所有需要参与内容过滤的 Model 类（Illust, Novel, UserPreview, Comment）
 * 应实现此接口，提供过滤所需的字段。
 * 不实现此接口的类型（如 SpotlightArticle, BookmarkTag）将直接通过过滤。
 */
interface Filterable {
    /** 内容自身 ID，用于屏蔽单个作品 */
    val contentId: Long? get() = null

    /** 内容作者的用户 ID，用于屏蔽用户 */
    val authorId: Long?

    /** 内容标签名列表，用于屏蔽标签 */
    val contentTags: List<String>

    /** 内容是否有效可见（未被删除/隐藏） */
    val isContentVisible: Boolean
}

private val Context.contentFilterDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "content_filter")

/**
 * 全局内容过滤管理器
 *
 * 管理屏蔽用户、屏蔽作品和屏蔽标签列表，提供统一的过滤判定方法。
 * 通过 filterVersion 通知所有活跃列表响应式地重新过滤。
 * 屏蔽数据持久化到 DataStore，不跟随用户切换，全局一致。
 *
 * 设计模式：
 * - Strategy: shouldShow() 封装可组合的过滤策略
 * - Observer: filterVersion StateFlow 通知所有订阅者
 * - Singleton: 全局唯一实例，所有列表共享同一过滤规则
 */
object ContentFilterManager {

    private val BLOCKED_USER_IDS = stringPreferencesKey("blocked_user_ids")
    private val BLOCKED_CONTENT_IDS = stringPreferencesKey("blocked_content_ids")
    private val BLOCKED_TAGS = stringPreferencesKey("blocked_tags")

    private val gson = Gson()
    private lateinit var dataStore: DataStore<Preferences>
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _blockedUserIds = MutableStateFlow<Set<Long>>(emptySet())
    val blockedUserIds: StateFlow<Set<Long>> = _blockedUserIds.asStateFlow()

    private val _blockedContentIds = MutableStateFlow<Set<Long>>(emptySet())
    val blockedContentIds: StateFlow<Set<Long>> = _blockedContentIds.asStateFlow()

    private val _blockedTags = MutableStateFlow<Set<String>>(emptySet())
    val blockedTags: StateFlow<Set<String>> = _blockedTags.asStateFlow()

    /**
     * 过滤版本号，每次屏蔽/解除屏蔽操作 +1。
     * ViewModel 监听此值变化后对已加载的列表重新过滤。
     */
    private val _filterVersion = MutableStateFlow(0)
    val filterVersion: StateFlow<Int> = _filterVersion.asStateFlow()

    fun initialize(context: Context) {
        dataStore = context.contentFilterDataStore
        scope.launch {
            dataStore.data.collect { prefs ->
                _blockedUserIds.value = deserializeLongSet(prefs[BLOCKED_USER_IDS])
                _blockedContentIds.value = deserializeLongSet(prefs[BLOCKED_CONTENT_IDS])
                _blockedTags.value = deserializeStringSet(prefs[BLOCKED_TAGS])
            }
        }
    }

    fun blockUser(userId: Long) {
        _blockedUserIds.update { it + userId }
        _filterVersion.update { it + 1 }
        persistUserIds()
    }

    fun unblockUser(userId: Long) {
        _blockedUserIds.update { it - userId }
        _filterVersion.update { it + 1 }
        persistUserIds()
    }

    fun blockContent(contentId: Long) {
        _blockedContentIds.update { it + contentId }
        _filterVersion.update { it + 1 }
        persistContentIds()
    }

    fun unblockContent(contentId: Long) {
        _blockedContentIds.update { it - contentId }
        _filterVersion.update { it + 1 }
        persistContentIds()
    }

    fun blockTag(tag: String) {
        _blockedTags.update { it + tag }
        _filterVersion.update { it + 1 }
        persistTags()
    }

    fun unblockTag(tag: String) {
        _blockedTags.update { it - tag }
        _filterVersion.update { it + 1 }
        persistTags()
    }

    fun isUserBlocked(userId: Long): Boolean = userId in _blockedUserIds.value

    fun isContentBlocked(contentId: Long): Boolean = contentId in _blockedContentIds.value

    fun isTagBlocked(tag: String): Boolean = tag in _blockedTags.value

    /**
     * 统一过滤判定：返回 true 表示该 item 应该展示，false 表示应该被过滤掉。
     *
     * 对非 Filterable 类型直接返回 true（通过过滤）。
     * 对 Filterable 类型依次检查：
     * 1. 内容本身是否有效（未删除/隐藏）
     * 2. 内容是否被单独屏蔽
     * 3. 作者是否被用户屏蔽
     * 4. 标签是否被用户屏蔽
     */
    fun <T> shouldShow(item: T): Boolean {
        if (item !is Filterable) return true
        if (!item.isContentVisible) return false
        val contentId = item.contentId
        if (contentId != null && contentId in _blockedContentIds.value) return false
        val authorId = item.authorId
        if (authorId != null && authorId in _blockedUserIds.value) return false
        if (item.contentTags.any { it in _blockedTags.value }) return false
        return true
    }

    /**
     * 在给定 scope 中监听过滤规则变化。
     * 用于不使用 PagedDataLoader 的 ViewModel（如 RecommendedContentViewModel）。
     */
    fun onFilterChanged(scope: CoroutineScope, action: () -> Unit) {
        scope.launch {
            filterVersion.drop(1).collect { action() }
        }
    }

    // ===== Persistence helpers =====

    private fun persistUserIds() {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[BLOCKED_USER_IDS] = gson.toJson(_blockedUserIds.value.toList())
            }
        }
    }

    private fun persistContentIds() {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[BLOCKED_CONTENT_IDS] = gson.toJson(_blockedContentIds.value.toList())
            }
        }
    }

    private fun persistTags() {
        scope.launch {
            dataStore.edit { prefs ->
                prefs[BLOCKED_TAGS] = gson.toJson(_blockedTags.value.toList())
            }
        }
    }

    private fun deserializeLongSet(json: String?): Set<Long> {
        if (json.isNullOrEmpty()) return emptySet()
        return try {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson<List<Long>>(json, type).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun deserializeStringSet(json: String?): Set<String> {
        if (json.isNullOrEmpty()) return emptySet()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
