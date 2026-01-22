package ceui.lisa.jcstaff.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 浏览历史 DAO
 */
@Dao
interface BrowseHistoryDao {

    /**
     * 获取所有历史记录（按时间倒序），返回 Flow 实时更新 UI
     */
    @Query("SELECT * FROM browse_history ORDER BY viewed_at DESC")
    fun getAllFlow(): Flow<List<BrowseHistoryEntity>>

    /**
     * 插入或更新历史记录（REPLACE 策略，重复浏览更新时间戳）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: BrowseHistoryEntity)

    /**
     * 删除指定历史记录
     */
    @Query("DELETE FROM browse_history WHERE illust_id = :illustId")
    suspend fun delete(illustId: Long)

    /**
     * 清空所有历史记录
     */
    @Query("DELETE FROM browse_history")
    suspend fun deleteAll()

    /**
     * 获取历史记录数量
     */
    @Query("SELECT COUNT(*) FROM browse_history")
    suspend fun count(): Int

    /**
     * LRU 清理：保留最新的 N 条记录
     */
    @Query("DELETE FROM browse_history WHERE illust_id NOT IN (SELECT illust_id FROM browse_history ORDER BY viewed_at DESC LIMIT :keepCount)")
    suspend fun keepRecent(keepCount: Int)

    /**
     * 删除指定时间之前的记录
     */
    @Query("DELETE FROM browse_history WHERE viewed_at < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)
}
