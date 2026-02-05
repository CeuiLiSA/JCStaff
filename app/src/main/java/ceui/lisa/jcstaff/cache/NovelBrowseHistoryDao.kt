package ceui.lisa.jcstaff.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 小说浏览历史 DAO
 */
@Dao
interface NovelBrowseHistoryDao {

    @Query("SELECT * FROM novel_browse_history ORDER BY viewed_at DESC")
    fun getAllFlow(): Flow<List<NovelBrowseHistoryEntity>>

    /**
     * 分页获取历史记录（按时间倒序）
     */
    @Query("SELECT * FROM novel_browse_history ORDER BY viewed_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<NovelBrowseHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: NovelBrowseHistoryEntity)

    @Query("DELETE FROM novel_browse_history WHERE novel_id = :novelId")
    suspend fun delete(novelId: Long)

    @Query("DELETE FROM novel_browse_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM novel_browse_history")
    suspend fun count(): Int

    @Query("DELETE FROM novel_browse_history WHERE novel_id NOT IN (SELECT novel_id FROM novel_browse_history ORDER BY viewed_at DESC LIMIT :keepCount)")
    suspend fun keepRecent(keepCount: Int)

    @Query("DELETE FROM novel_browse_history WHERE viewed_at < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)
}
