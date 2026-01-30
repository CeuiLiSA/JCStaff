package ceui.lisa.jcstaff.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ceui.lisa.jcstaff.network.Tag

/**
 * 搜索历史实体
 *
 * 记录用户搜索过的标签（包含原名和翻译名）
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "tag_name")
    val tagName: String,

    @ColumnInfo(name = "translated_name")
    val translatedName: String?,

    @ColumnInfo(name = "searched_at")
    val searchedAt: Long
) {
    fun toTag(): Tag = Tag(name = tagName, translated_name = translatedName)

    companion object {
        fun fromTag(tag: Tag): SearchHistoryEntity? {
            val name = tag.name?.trim() ?: return null
            if (name.isEmpty()) return null
            return SearchHistoryEntity(
                tagName = name,
                translatedName = tag.translated_name,
                searchedAt = System.currentTimeMillis()
            )
        }
    }
}
