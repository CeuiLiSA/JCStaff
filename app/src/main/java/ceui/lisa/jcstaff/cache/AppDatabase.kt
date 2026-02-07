package ceui.lisa.jcstaff.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Room 类型转换器
 */
class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}

/**
 * 应用数据库
 */
@Database(
    entities = [
        ApiCacheEntity::class,
        BrowseHistoryEntity::class,
        NovelBrowseHistoryEntity::class,
        UserBrowseHistoryEntity::class,
        SearchHistoryEntity::class,
        DownloadTaskEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun apiCacheDao(): ApiCacheDao

    abstract fun browseHistoryDao(): BrowseHistoryDao

    abstract fun novelBrowseHistoryDao(): NovelBrowseHistoryDao

    abstract fun userBrowseHistoryDao(): UserBrowseHistoryDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun downloadTaskDao(): DownloadTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private var currentUserId: Long? = null

        fun getInstanceForUser(context: Context, userId: Long): AppDatabase {
            return synchronized(this) {
                val existing = INSTANCE
                if (existing != null && currentUserId == userId) {
                    return existing
                }
                // Close previous instance if switching users
                if (existing != null && currentUserId != userId) {
                    existing.close()
                    INSTANCE = null
                }
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jcstaff_db_$userId"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also {
                        INSTANCE = it
                        currentUserId = userId
                    }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                currentUserId = null
            }
        }
    }
}
