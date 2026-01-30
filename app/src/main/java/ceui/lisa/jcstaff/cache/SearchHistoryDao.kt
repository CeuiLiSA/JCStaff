package ceui.lisa.jcstaff.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 搜索历史 DAO
 */
@Dao
interface SearchHistoryDao {

    /**
     * 获取所有历史记录（按时间倒序），返回 Flow 实时更新 UI
     */
    @Query("SELECT * FROM search_history ORDER BY searched_at DESC")
    fun getAllFlow(): Flow<List<SearchHistoryEntity>>

    /**
     * 插入或更新历史记录（REPLACE 策略，重复搜索更新时间戳）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: SearchHistoryEntity)

    /**
     * 删除指定历史记录
     */
    @Query("DELETE FROM search_history WHERE tag_name = :tagName")
    suspend fun delete(tagName: String)

    /**
     * 清空所有历史记录
     */
    @Query("DELETE FROM search_history")
    suspend fun deleteAll()

    /**
     * 获取历史记录数量
     */
    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun count(): Int

    /**
     * LRU 清理：保留最新的 N 条记录
     */
    @Query("DELETE FROM search_history WHERE tag_name NOT IN (SELECT tag_name FROM search_history ORDER BY searched_at DESC LIMIT :keepCount)")
    suspend fun keepRecent(keepCount: Int)
}
