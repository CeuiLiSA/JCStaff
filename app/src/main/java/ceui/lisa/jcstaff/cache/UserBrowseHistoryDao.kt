package ceui.lisa.jcstaff.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 用户浏览历史 DAO
 */
@Dao
interface UserBrowseHistoryDao {

    @Query("SELECT * FROM user_browse_history ORDER BY viewed_at DESC")
    fun getAllFlow(): Flow<List<UserBrowseHistoryEntity>>

    /**
     * 分页获取历史记录（按时间倒序）
     */
    @Query("SELECT * FROM user_browse_history ORDER BY viewed_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<UserBrowseHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: UserBrowseHistoryEntity)

    @Query("DELETE FROM user_browse_history WHERE user_id = :userId")
    suspend fun delete(userId: Long)

    @Query("DELETE FROM user_browse_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM user_browse_history")
    suspend fun count(): Int

    @Query("DELETE FROM user_browse_history WHERE user_id NOT IN (SELECT user_id FROM user_browse_history ORDER BY viewed_at DESC LIMIT :keepCount)")
    suspend fun keepRecent(keepCount: Int)

    @Query("DELETE FROM user_browse_history WHERE viewed_at < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)
}
