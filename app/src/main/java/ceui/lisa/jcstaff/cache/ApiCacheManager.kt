package ceui.lisa.jcstaff.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * API 缓存管理器
 *
 * 使用 Room 数据库持久化缓存，支持：
 * - 15 分钟缓存过期
 * - LRU 淘汰策略
 * - 最大 100 条缓存
 */
object ApiCacheManager {

    private const val TAG = "ApiCache"
    private const val HOME_KEY = "v1/illust/recommended?include_ranking_illusts=true"

    /** 缓存有效期：15 分钟内认为是新鲜的，直接使用 */
    private val CACHE_FRESH_DURATION_MS = TimeUnit.MINUTES.toMillis(15)

    /** 缓存最大保留时间：7 天后才删除，支持 stale-while-revalidate */
    private val CACHE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)
    private const val MAX_CACHE_SIZE = 100

    private var dao: ApiCacheDao? = null
    private val mutex = Mutex()

    data class CacheEntry(
        val responseBody: ByteArray,
        val contentType: String?,
        val httpCode: Int,
        val httpMessage: String,
        val timestamp: Long
    )

    /**
     * 初始化缓存管理器（用户隔离）
     */
    fun initialize(context: Context, userId: Long) {
        dao = AppDatabase.getInstanceForUser(context, userId).apiCacheDao()
        Log.d(TAG, "🚀 Cache initialized for user $userId")
        CoroutineScope(Dispatchers.IO).launch { cleanupExpired() }
    }

    /**
     * 重置缓存管理器
     */
    fun reset() {
        dao = null
        Log.d(TAG, "🔄 Cache reset")
    }

    /**
     * 获取缓存（检查过期）
     */
    fun getSync(key: String): CacheEntry? = runBlocking { get(key) }

    suspend fun get(key: String): CacheEntry? = withContext(Dispatchers.IO) {
        val cacheDao = dao ?: return@withContext null

        try {
            val entity = cacheDao.get(key) ?: return@withContext null

            // 检查是否新鲜（15分钟内）
            if (isFresh(entity.timestamp).not()) {
                val ageMinutes = (System.currentTimeMillis() - entity.timestamp) / 60000.0
                Log.d(TAG, "⏰ EXPIRED (%.1f min) ${shortenKey(key)}".format(ageMinutes))
                return@withContext null
            }

            val ageSeconds = (System.currentTimeMillis() - entity.timestamp) / 1000
            val remainingSeconds =
                (CACHE_FRESH_DURATION_MS - (System.currentTimeMillis() - entity.timestamp)) / 1000

            Log.d(TAG, "✅ HIT (fresh) ${shortenKey(key)}")
            Log.d(TAG, "   ├─ Age: ${ageSeconds}s")
            Log.d(TAG, "   ├─ Expires in: ${remainingSeconds}s")
            Log.d(TAG, "   └─ Size: ${entity.responseBody.size} bytes")

            CacheEntry(
                responseBody = entity.responseBody,
                contentType = entity.contentType,
                httpCode = entity.httpCode,
                httpMessage = entity.httpMessage,
                timestamp = entity.timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "❗ Read cache failed: ${e.message}")
            null
        }
    }

    suspend fun getStale(key: String): CacheEntry? = withContext(Dispatchers.IO) {
        val cacheDao = dao ?: return@withContext null

        try {
            val entity = cacheDao.get(key) ?: return@withContext null

            val ageSeconds = (System.currentTimeMillis() - entity.timestamp) / 1000
            val expired = isExpired(entity.timestamp)

            Log.d(TAG, "📦 STALE ${shortenKey(key)}")
            Log.d(TAG, "   ├─ Age: ${ageSeconds}s")
            Log.d(TAG, "   ├─ Expired: $expired")
            Log.d(TAG, "   └─ Size: ${entity.responseBody.size} bytes")

            if (shortenKey(key) == HOME_KEY) {
                delay(700L)
            }

            CacheEntry(
                responseBody = entity.responseBody,
                contentType = entity.contentType,
                httpCode = entity.httpCode,
                httpMessage = entity.httpMessage,
                timestamp = entity.timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "❗ Read stale cache failed: ${e.message}")
            null
        }
    }

    /**
     * 构建缓存 key（供 ViewModel 使用）
     */
    fun buildCacheKey(method: String, url: String): String {
        return "$method:$url"
    }

    /**
     * 存储缓存
     */
    fun putSync(
        key: String,
        responseBody: ByteArray,
        contentType: String?,
        httpCode: Int,
        httpMessage: String
    ) = runBlocking {
        put(key, responseBody, contentType, httpCode, httpMessage)
    }

    suspend fun put(
        key: String,
        responseBody: ByteArray,
        contentType: String?,
        httpCode: Int,
        httpMessage: String
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cacheDao = dao ?: return@withContext

            try {
                // 检查缓存大小，必要时淘汰旧条目
                enforceMaxSize(cacheDao)

                val entity = ApiCacheEntity(
                    cacheKey = key,
                    responseBody = responseBody,
                    contentType = contentType,
                    httpCode = httpCode,
                    httpMessage = httpMessage,
                    timestamp = System.currentTimeMillis()
                )

                cacheDao.insert(entity)

                val count = cacheDao.count()
                Log.d(TAG, "💾 STORED ${shortenKey(key)}")
                Log.d(TAG, "   ├─ Size: ${responseBody.size} bytes")
                Log.d(TAG, "   └─ Cache entries: $count/$MAX_CACHE_SIZE")
            } catch (e: Exception) {
                Log.e(TAG, "❗ Write cache failed: ${e.message}")
            }
        }
    }

    /**
     * 清除所有缓存
     */
    fun clearAllSync() = runBlocking { clearAll() }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val cacheDao = dao ?: return@withContext
        val count = cacheDao.count()
        cacheDao.deleteAll()
        Log.d(TAG, "🗑️ CLEAR ALL ($count entries removed)")
    }

    /**
     * 使指定 URL 模式的缓存失效
     */
    fun invalidateSync(urlPattern: String) = runBlocking { invalidate(urlPattern) }

    suspend fun invalidate(urlPattern: String) = withContext(Dispatchers.IO) {
        // 简化处理：清除所有缓存
        clearAll()
        Log.d(TAG, "🗑️ INVALIDATE (cleared all due to pattern match)")
    }

    /**
     * 清理过期缓存（超过 7 天的才删除）
     */
    suspend fun cleanupExpired() = withContext(Dispatchers.IO) {
        val cacheDao = dao ?: return@withContext
        val expireTime = System.currentTimeMillis() - CACHE_MAX_AGE_MS
        val countBefore = cacheDao.count()
        cacheDao.deleteExpired(expireTime)
        val countAfter = cacheDao.count()
        val cleaned = countBefore - countAfter

        if (cleaned > 0) {
            Log.d(TAG, "🧹 Cleaned $cleaned old entries (>7 days)")
        }
    }

    /**
     * 获取缓存统计信息
     */
    fun getStatsSync(): String = runBlocking { getStats() }

    suspend fun getStats(): String = withContext(Dispatchers.IO) {
        val cacheDao = dao ?: return@withContext "Cache not initialized"
        val count = cacheDao.count()
        val totalSize = cacheDao.totalSize() ?: 0
        """
            |📊 Cache Stats:
            |   ├─ Entries: $count/$MAX_CACHE_SIZE
            |   └─ Total size: ${totalSize / 1024} KB
        """.trimMargin()
    }

    private fun shortenKey(key: String): String {
        // 提取 path 部分用于日志
        return key.substringAfter("://").substringAfter("/").take(50)
    }

    /** 检查缓存是否新鲜（15分钟内） */
    private fun isFresh(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp <= CACHE_FRESH_DURATION_MS
    }

    /** 检查缓存是否已过最大保留期（7天） */
    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_MAX_AGE_MS
    }

    private suspend fun enforceMaxSize(cacheDao: ApiCacheDao) {
        val count = cacheDao.count()

        if (count >= MAX_CACHE_SIZE) {
            // 删除最旧的 10 个
            val oldestKeys = cacheDao.getOldestKeys(10)
            if (oldestKeys.isNotEmpty()) {
                cacheDao.deleteByKeys(oldestKeys)
                Log.d(TAG, "🗑️ Evicted ${oldestKeys.size} old entries")
            }
        }
    }
}
