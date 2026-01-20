package ceui.lisa.jcstaff.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * API 缓存实体
 */
@Entity(tableName = "api_cache")
data class ApiCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,

    @ColumnInfo(name = "response_body", typeAffinity = ColumnInfo.BLOB)
    val responseBody: ByteArray,

    @ColumnInfo(name = "content_type")
    val contentType: String?,

    @ColumnInfo(name = "http_code")
    val httpCode: Int,

    @ColumnInfo(name = "http_message")
    val httpMessage: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiCacheEntity

        if (cacheKey != other.cacheKey) return false
        if (!responseBody.contentEquals(other.responseBody)) return false
        if (contentType != other.contentType) return false
        if (httpCode != other.httpCode) return false
        if (httpMessage != other.httpMessage) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cacheKey.hashCode()
        result = 31 * result + responseBody.contentHashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + httpCode
        result = 31 * result + httpMessage.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
