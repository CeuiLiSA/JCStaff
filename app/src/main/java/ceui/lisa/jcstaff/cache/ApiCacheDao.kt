package ceui.lisa.jcstaff.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * API 缓存 DAO
 */
@Dao
interface ApiCacheDao {

    @Query("SELECT * FROM api_cache WHERE cache_key = :key")
    suspend fun get(key: String): ApiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApiCacheEntity)

    @Query("DELETE FROM api_cache WHERE cache_key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM api_cache")
    suspend fun deleteAll()

    @Query("DELETE FROM api_cache WHERE timestamp < :expireTime")
    suspend fun deleteExpired(expireTime: Long)

    @Query("SELECT COUNT(*) FROM api_cache")
    suspend fun count(): Int

    @Query("SELECT SUM(LENGTH(response_body)) FROM api_cache")
    suspend fun totalSize(): Long?

    /**
     * 获取最旧的 N 条记录的 key，用于 LRU 淘汰
     */
    @Query("SELECT cache_key FROM api_cache ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getOldestKeys(limit: Int): List<String>

    /**
     * 删除指定的 keys
     */
    @Query("DELETE FROM api_cache WHERE cache_key IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>)
}
