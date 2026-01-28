package ceui.lisa.jcstaff.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 应用数据库
 */
@Database(
    entities = [
        ApiCacheEntity::class,
        BrowseHistoryEntity::class,
        NovelBrowseHistoryEntity::class,
        UserBrowseHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun apiCacheDao(): ApiCacheDao

    abstract fun browseHistoryDao(): BrowseHistoryDao

    abstract fun novelBrowseHistoryDao(): NovelBrowseHistoryDao

    abstract fun userBrowseHistoryDao(): UserBrowseHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstanceForUser(context: Context, userId: Long): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jcstaff_db_$userId"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
