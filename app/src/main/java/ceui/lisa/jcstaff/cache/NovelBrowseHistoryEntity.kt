package ceui.lisa.jcstaff.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 小说浏览历史实体
 *
 * 记录用户浏览过的小说，支持：
 * - 以 novelId 为主键自动去重
 * - 重复浏览更新时间戳
 * - 存储显示字段避免 JSON 解析开销
 * - 存储完整 JSON 支持离线查看详情
 */
@Entity(tableName = "novel_browse_history")
data class NovelBrowseHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "novel_id")
    val novelId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "cover_url")
    val coverUrl: String,

    @ColumnInfo(name = "text_length")
    val textLength: Int,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "user_name")
    val userName: String,

    @ColumnInfo(name = "user_avatar_url")
    val userAvatarUrl: String?,

    @ColumnInfo(name = "novel_json")
    val novelJson: String,

    @ColumnInfo(name = "viewed_at")
    val viewedAt: Long
)
