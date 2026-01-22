package ceui.lisa.jcstaff.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 浏览历史实体
 *
 * 记录用户浏览过的插画，支持：
 * - 以 illustId 为主键自动去重
 * - 重复浏览更新时间戳
 * - 存储显示字段避免 JSON 解析开销
 * - 存储完整 JSON 支持离线查看详情
 */
@Entity(tableName = "browse_history")
data class BrowseHistoryEntity(
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

    @ColumnInfo(name = "viewed_at")
    val viewedAt: Long
)
