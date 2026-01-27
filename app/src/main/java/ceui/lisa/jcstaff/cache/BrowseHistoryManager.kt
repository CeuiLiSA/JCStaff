package ceui.lisa.jcstaff.cache

import android.content.Context
import android.util.Log
import ceui.lisa.jcstaff.network.Illust
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 浏览历史管理器
 *
 * 单例模式，与 ApiCacheManager 保持一致：
 * - initialize(context) - MainActivity 初始化
 * - recordView(illust) - 记录浏览（fire-and-forget）
 * - getHistoryFlow() - 获取历史列表 Flow
 * - 自动清理 30 天前的记录
 */
object BrowseHistoryManager {

    private const val TAG = "BrowseHistory"
    private const val MAX_HISTORY_SIZE = 500
    private val EXPIRE_DURATION_MS = TimeUnit.DAYS.toMillis(30)

    private var dao: BrowseHistoryDao? = null
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 初始化浏览历史管理器
     */
    fun initialize(context: Context) {
        if (dao == null) {
            dao = AppDatabase.getInstance(context).browseHistoryDao()
            Log.d(TAG, "BrowseHistoryManager initialized")

            // 启动时清理过期记录
            scope.launch {
                cleanupExpired()
            }
        }
    }

    /**
     * 初始化浏览历史管理器（用户隔离）
     */
    fun initialize(context: Context, userId: Long) {
        dao = AppDatabase.getInstanceForUser(context, userId).browseHistoryDao()
        Log.d(TAG, "BrowseHistoryManager initialized for user $userId")
        scope.launch { cleanupExpired() }
    }

    /**
     * 重置浏览历史管理器
     */
    fun reset() {
        dao = null
        Log.d(TAG, "BrowseHistoryManager reset")
    }

    /**
     * 记录浏览历史（fire-and-forget）
     */
    fun recordView(illust: Illust) {
        val historyDao = dao ?: return

        scope.launch {
            try {
                val entity = BrowseHistoryEntity(
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

                historyDao.insertOrUpdate(entity)

                // LRU 清理
                val count = historyDao.count()
                if (count > MAX_HISTORY_SIZE) {
                    historyDao.keepRecent(MAX_HISTORY_SIZE)
                    Log.d(TAG, "LRU cleanup: kept $MAX_HISTORY_SIZE records")
                }

                Log.d(TAG, "Recorded view: ${illust.id} - ${illust.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record view: ${e.message}")
            }
        }
    }

    /**
     * 获取历史列表 Flow
     * 返回 Illust 列表，按浏览时间倒序
     */
    fun getHistoryFlow(): Flow<List<Illust>> {
        val historyDao = dao ?: throw IllegalStateException("BrowseHistoryManager not initialized")

        return historyDao.getAllFlow().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    gson.fromJson(entity.illustJson, Illust::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse illust JSON: ${e.message}")
                    null
                }
            }
        }
    }

    /**
     * 清空所有历史记录
     */
    fun clearAll() {
        val historyDao = dao ?: return

        scope.launch {
            try {
                historyDao.deleteAll()
                Log.d(TAG, "All history cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear history: ${e.message}")
            }
        }
    }

    /**
     * 删除指定历史记录
     */
    fun delete(illustId: Long) {
        val historyDao = dao ?: return

        scope.launch {
            try {
                historyDao.delete(illustId)
                Log.d(TAG, "Deleted history: $illustId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete history: ${e.message}")
            }
        }
    }

    /**
     * 清理过期记录（30 天前）
     */
    private suspend fun cleanupExpired() {
        val historyDao = dao ?: return

        try {
            val expireTime = System.currentTimeMillis() - EXPIRE_DURATION_MS
            historyDao.deleteBefore(expireTime)
            Log.d(TAG, "Cleaned up expired records (older than 30 days)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup expired records: ${e.message}")
        }
    }
}
