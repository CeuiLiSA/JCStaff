package ceui.lisa.jcstaff.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户浏览历史实体
 *
 * 记录用户浏览过的其他用户主页，支持：
 * - 以 userId 为主键自动去重
 * - 重复浏览更新时间戳
 * - 存储显示字段避免 JSON 解析开销
 * - 存储完整 JSON 支持离线查看详情
 */
@Entity(tableName = "user_browse_history")
data class UserBrowseHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "user_name")
    val userName: String,

    @ColumnInfo(name = "user_account")
    val userAccount: String,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    @ColumnInfo(name = "comment")
    val comment: String?,

    @ColumnInfo(name = "user_json")
    val userJson: String,

    @ColumnInfo(name = "viewed_at")
    val viewedAt: Long
)
