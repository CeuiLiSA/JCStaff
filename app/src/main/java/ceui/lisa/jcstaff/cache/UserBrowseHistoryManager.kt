package ceui.lisa.jcstaff.cache

import android.content.Context
import android.util.Log
import ceui.lisa.jcstaff.network.User
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 用户浏览历史管理器
 *
 * 单例模式，与 BrowseHistoryManager 保持一致
 */
object UserBrowseHistoryManager {

    private const val TAG = "UserBrowseHistory"
    private const val MAX_HISTORY_SIZE = 500
    private val EXPIRE_DURATION_MS = TimeUnit.DAYS.toMillis(30)

    private var dao: UserBrowseHistoryDao? = null
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun initialize(context: Context, userId: Long) {
        dao = AppDatabase.getInstanceForUser(context, userId).userBrowseHistoryDao()
        Log.d(TAG, "UserBrowseHistoryManager initialized for user $userId")
        scope.launch { cleanupExpired() }
    }

    fun reset() {
        dao = null
        Log.d(TAG, "UserBrowseHistoryManager reset")
    }

    fun recordView(user: User) {
        val historyDao = dao ?: return

        scope.launch {
            try {
                val entity = UserBrowseHistoryEntity(
                    userId = user.id,
                    userName = user.name ?: "",
                    userAccount = user.account ?: "",
                    avatarUrl = user.profile_image_urls?.findAvatarUrl(),
                    comment = user.comment,
                    userJson = gson.toJson(user),
                    viewedAt = System.currentTimeMillis()
                )

                historyDao.insertOrUpdate(entity)

                val count = historyDao.count()
                if (count > MAX_HISTORY_SIZE) {
                    historyDao.keepRecent(MAX_HISTORY_SIZE)
                    Log.d(TAG, "LRU cleanup: kept $MAX_HISTORY_SIZE records")
                }

                Log.d(TAG, "Recorded view: ${user.id} - ${user.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record view: ${e.message}")
            }
        }
    }

    fun getHistoryFlow(): Flow<List<User>> {
        val historyDao =
            dao ?: throw IllegalStateException("UserBrowseHistoryManager not initialized")

        return historyDao.getAllFlow().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    gson.fromJson(entity.userJson, User::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse user JSON: ${e.message}")
                    null
                }
            }
        }
    }

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

    fun delete(userId: Long) {
        val historyDao = dao ?: return
        scope.launch {
            try {
                historyDao.delete(userId)
                Log.d(TAG, "Deleted history: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete history: ${e.message}")
            }
        }
    }

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
