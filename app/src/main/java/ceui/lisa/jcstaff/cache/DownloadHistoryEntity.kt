package ceui.lisa.jcstaff.cache

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 下载状态枚举
 */
enum class DownloadStatus {
    PENDING,      // 待下载
    DOWNLOADING,  // 下载中
    COMPLETED,    // 已完成
    FAILED        // 失败
}

/**
 * 下载任务实体
 *
 * 记录用户的下载任务，支持：
 * - 下载状态追踪（待下载、下载中、已完成、失败）
 * - 多图下载进度追踪
 * - App 重启后恢复未完成的任务
 */
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "illust_id")
    val illustId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "preview_url")
    val previewUrl: String,

    @ColumnInfo(name = "width")
    val width: Int,

    @ColumnInfo(name = "height")
    val height: Int,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "user_name")
    val userName: String,

    @ColumnInfo(name = "user_avatar_url")
    val userAvatarUrl: String?,

    @ColumnInfo(name = "illust_json")
    val illustJson: String,

    @ColumnInfo(name = "square_url")
    val squareUrl: String = "",

    @ColumnInfo(name = "status")
    val status: DownloadStatus = DownloadStatus.PENDING,

    @ColumnInfo(name = "total_pages")
    val totalPages: Int = 1,

    @ColumnInfo(name = "downloaded_pages")
    val downloadedPages: Int = 0,

    @ColumnInfo(name = "current_page_progress")
    val currentPageProgress: Int = 0,  // 当前页下载进度 0-100

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
) {
    /**
     * 总体进度 (0.0 - 1.0)
     * 对于多图：(已完成页数 + 当前页进度/100) / 总页数
     * 对于单图：当前页进度 / 100
     */
    val progress: Float
        get() = if (totalPages > 0) {
            (downloadedPages + currentPageProgress / 100f) / totalPages
        } else 0f

    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED

    val isPending: Boolean
        get() = status == DownloadStatus.PENDING

    val isDownloading: Boolean
        get() = status == DownloadStatus.DOWNLOADING

    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED
}

@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM download_tasks ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY created_at ASC")
    fun getByStatusFlow(status: DownloadStatus): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status IN (:statuses) ORDER BY created_at ASC")
    suspend fun getByStatuses(statuses: List<DownloadStatus>): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE illust_id = :illustId")
    suspend fun getById(illustId: Long): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DownloadTaskEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(entity: DownloadTaskEntity): Long

    @Update
    suspend fun update(entity: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status, error_message = :errorMessage WHERE illust_id = :illustId")
    suspend fun updateStatus(illustId: Long, status: DownloadStatus, errorMessage: String? = null)

    @Query("UPDATE download_tasks SET status = :status, downloaded_pages = :downloadedPages, current_page_progress = 0 WHERE illust_id = :illustId")
    suspend fun updateProgress(illustId: Long, status: DownloadStatus, downloadedPages: Int)

    @Query("UPDATE download_tasks SET current_page_progress = :progress WHERE illust_id = :illustId")
    suspend fun updateCurrentPageProgress(illustId: Long, progress: Int)

    @Query("UPDATE download_tasks SET status = :status, downloaded_pages = :downloadedPages, current_page_progress = 100, completed_at = :completedAt WHERE illust_id = :illustId")
    suspend fun markCompleted(illustId: Long, status: DownloadStatus = DownloadStatus.COMPLETED, downloadedPages: Int, completedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM download_tasks WHERE illust_id = :illustId")
    suspend fun delete(illustId: Long)

    @Query("DELETE FROM download_tasks WHERE status = :status")
    suspend fun deleteByStatus(status: DownloadStatus)

    @Query("DELETE FROM download_tasks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM download_tasks")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = :status")
    suspend fun countByStatus(status: DownloadStatus): Int

    @Query("DELETE FROM download_tasks WHERE illust_id NOT IN (SELECT illust_id FROM download_tasks ORDER BY created_at DESC LIMIT :keepCount)")
    suspend fun keepRecent(keepCount: Int)

    // 将所有 DOWNLOADING 状态重置为 PENDING（用于 App 重启后恢复）
    @Query("UPDATE download_tasks SET status = 'PENDING' WHERE status = 'DOWNLOADING'")
    suspend fun resetDownloadingToPending()
}
