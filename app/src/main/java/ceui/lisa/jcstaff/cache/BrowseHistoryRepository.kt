package ceui.lisa.jcstaff.cache

import android.content.Context
import android.util.Log
import ceui.lisa.jcstaff.network.Illust
import ceui.lisa.jcstaff.network.Novel
import ceui.lisa.jcstaff.network.Tag
import ceui.lisa.jcstaff.network.User
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 统一的浏览历史仓库
 *
 * 管理三种类型的浏览历史：插画、小说、用户
 * 替代原来的三个独立 Manager
 */
object BrowseHistoryRepository {

    private const val TAG = "BrowseHistory"
    private const val MAX_HISTORY_SIZE = 500
    private val EXPIRE_DURATION_MS = TimeUnit.DAYS.toMillis(30)

    private var illustDao: BrowseHistoryDao? = null
    private var novelDao: NovelBrowseHistoryDao? = null
    private var userDao: UserBrowseHistoryDao? = null
    private var searchDao: SearchHistoryDao? = null

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context, userId: Long) {
        val db = AppDatabase.getInstanceForUser(context, userId)
        illustDao = db.browseHistoryDao()
        novelDao = db.novelBrowseHistoryDao()
        userDao = db.userBrowseHistoryDao()
        searchDao = db.searchHistoryDao()
        Log.d(TAG, "BrowseHistoryRepository initialized for user $userId")
        scope.launch { cleanupAllExpired() }
    }

    fun reset() {
        illustDao = null
        novelDao = null
        userDao = null
        searchDao = null
        Log.d(TAG, "BrowseHistoryRepository reset")
    }

    // ==================== 插画历史 ====================

    fun recordIllust(illust: Illust) {
        val dao = illustDao ?: return
        scope.launch {
            runCatching {
                dao.insertOrUpdate(
                    BrowseHistoryEntity(
                        illustId = illust.id,
                        title = illust.title ?: "",
                        previewUrl = illust.previewUrl(),
                        width = illust.width,
                        height = illust.height,
                        userId = illust.user?.id ?: 0L,
                        userName = illust.user?.name ?: "",
                        userAvatarUrl = illust.user?.profile_image_urls?.findAvatarUrl(),
                        illustJson = gson.toJson(illust),
                        viewedAt = System.currentTimeMillis()
                    )
                )
                enforceLimit(dao::count, dao::keepRecent)
                Log.d(TAG, "Recorded illust: ${illust.id}")
            }.onFailure { Log.e(TAG, "Failed to record illust: ${it.message}") }
        }
    }

    fun getIllustHistoryFlow(): Flow<List<Illust>> {
        val dao = illustDao ?: return emptyFlow()
        return dao.getAllFlow().map { entities ->
            entities.mapNotNull { runCatching { gson.fromJson(it.illustJson, Illust::class.java) }.getOrNull() }
        }
    }

    fun clearIllustHistory() {
        illustDao?.let { dao -> scope.launch { runCatching { dao.deleteAll() } } }
    }

    fun deleteIllust(illustId: Long) {
        illustDao?.let { dao -> scope.launch { runCatching { dao.delete(illustId) } } }
    }

    // ==================== 小说历史 ====================

    fun recordNovel(novel: Novel) {
        val dao = novelDao ?: return
        scope.launch {
            runCatching {
                dao.insertOrUpdate(
                    NovelBrowseHistoryEntity(
                        novelId = novel.id,
                        title = novel.title ?: "",
                        coverUrl = novel.image_urls?.findMaxSizeUrl() ?: "",
                        textLength = novel.text_length ?: 0,
                        userId = novel.user?.id ?: 0L,
                        userName = novel.user?.name ?: "",
                        userAvatarUrl = novel.user?.profile_image_urls?.findAvatarUrl(),
                        novelJson = gson.toJson(novel),
                        viewedAt = System.currentTimeMillis()
                    )
                )
                enforceLimit(dao::count, dao::keepRecent)
                Log.d(TAG, "Recorded novel: ${novel.id}")
            }.onFailure { Log.e(TAG, "Failed to record novel: ${it.message}") }
        }
    }

    fun getNovelHistoryFlow(): Flow<List<Novel>> {
        val dao = novelDao ?: return emptyFlow()
        return dao.getAllFlow().map { entities ->
            entities.mapNotNull { runCatching { gson.fromJson(it.novelJson, Novel::class.java) }.getOrNull() }
        }
    }

    fun clearNovelHistory() {
        novelDao?.let { dao -> scope.launch { runCatching { dao.deleteAll() } } }
    }

    fun deleteNovel(novelId: Long) {
        novelDao?.let { dao -> scope.launch { runCatching { dao.delete(novelId) } } }
    }

    // ==================== 用户历史 ====================

    fun recordUser(user: User) {
        val dao = userDao ?: return
        scope.launch {
            runCatching {
                dao.insertOrUpdate(
                    UserBrowseHistoryEntity(
                        userId = user.id,
                        userName = user.name ?: "",
                        userAccount = user.account ?: "",
                        avatarUrl = user.profile_image_urls?.findAvatarUrl(),
                        comment = user.comment,
                        userJson = gson.toJson(user),
                        viewedAt = System.currentTimeMillis()
                    )
                )
                enforceLimit(dao::count, dao::keepRecent)
                Log.d(TAG, "Recorded user: ${user.id}")
            }.onFailure { Log.e(TAG, "Failed to record user: ${it.message}") }
        }
    }

    fun getUserHistoryFlow(): Flow<List<User>> {
        val dao = userDao ?: return emptyFlow()
        return dao.getAllFlow().map { entities ->
            entities.mapNotNull { runCatching { gson.fromJson(it.userJson, User::class.java) }.getOrNull() }
        }
    }

    fun clearUserHistory() {
        userDao?.let { dao -> scope.launch { runCatching { dao.deleteAll() } } }
    }

    fun deleteUser(userId: Long) {
        userDao?.let { dao -> scope.launch { runCatching { dao.delete(userId) } } }
    }

    // ==================== 搜索历史 ====================

    private const val MAX_SEARCH_HISTORY_SIZE = 50

    fun recordSearch(tag: Tag) {
        val dao = searchDao ?: return
        val entity = SearchHistoryEntity.fromTag(tag) ?: return
        scope.launch {
            runCatching {
                dao.insertOrUpdate(entity)
                if (dao.count() > MAX_SEARCH_HISTORY_SIZE) {
                    dao.keepRecent(MAX_SEARCH_HISTORY_SIZE)
                }
                Log.d(TAG, "Recorded search: ${tag.name}")
            }.onFailure { Log.e(TAG, "Failed to record search: ${it.message}") }
        }
    }

    fun getSearchHistoryFlow(): Flow<List<Tag>> {
        val dao = searchDao ?: return emptyFlow()
        return dao.getAllFlow().map { entities ->
            entities.map { it.toTag() }
        }
    }

    fun clearSearchHistory() {
        searchDao?.let { dao -> scope.launch { runCatching { dao.deleteAll() } } }
    }

    fun deleteSearchTag(tagName: String) {
        searchDao?.let { dao -> scope.launch { runCatching { dao.delete(tagName) } } }
    }

    // ==================== 内部工具 ====================

    private suspend fun enforceLimit(count: suspend () -> Int, keepRecent: suspend (Int) -> Unit) {
        if (count() > MAX_HISTORY_SIZE) {
            keepRecent(MAX_HISTORY_SIZE)
            Log.d(TAG, "LRU cleanup: kept $MAX_HISTORY_SIZE records")
        }
    }

    private suspend fun cleanupAllExpired() {
        val expireTime = System.currentTimeMillis() - EXPIRE_DURATION_MS
        runCatching { illustDao?.deleteBefore(expireTime) }
        runCatching { novelDao?.deleteBefore(expireTime) }
        runCatching { userDao?.deleteBefore(expireTime) }
        Log.d(TAG, "Cleaned up expired records (older than 30 days)")
    }
}
