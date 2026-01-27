package ceui.lisa.jcstaff.auth

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 账号数据清理
 *
 * 删除指定用户的所有数据：
 * - Room 数据库文件
 * - auth DataStore 文件
 * - settings DataStore 文件
 * - LoadTaskManager 缓存目录
 */
class AccountCleanup(private val context: Context, private val userId: Long) {


    fun deleteAccountData() {
        Log.d(TAG, "Deleting data for user $userId")

        // 删除 auth DataStore
        deleteFile(context, "datastore/auth_prefs_$userId.preferences_pb")

        // 删除 settings DataStore
        deleteFile(context, "datastore/settings_$userId.preferences_pb")

        // 删除 Room 数据库
        val dbDir = context.getDatabasePath("jcstaff_db_$userId").parentFile
        if (dbDir != null) {
            deleteDbFile(dbDir, "jcstaff_db_$userId")
            deleteDbFile(dbDir, "jcstaff_db_$userId-shm")
            deleteDbFile(dbDir, "jcstaff_db_$userId-wal")
        }

        // 删除缓存目录
        val cacheDir = File(context.cacheDir, "image_load_cache_$userId")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            Log.d(TAG, "Deleted cache dir for user $userId")
        }

        Log.d(TAG, "Data deleted for user $userId")
    }

    private fun deleteFile(context: Context, relPath: String) {
        val file = File(context.filesDir, relPath)
        if (file.exists()) {
            val success = file.delete()
            Log.d(TAG, "Delete $relPath: $success")
        }
    }

    private fun deleteDbFile(dbDir: File, name: String) {
        val file = File(dbDir, name)
        if (file.exists()) {
            val success = file.delete()
            Log.d(TAG, "Delete DB $name: $success")
        }
    }

    companion object {
        private const val TAG = "AccountCleanup"
    }
}
