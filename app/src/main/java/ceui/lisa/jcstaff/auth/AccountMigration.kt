package ceui.lisa.jcstaff.auth

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.network.AccountResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.io.File

private const val TAG = "AccountMigration"

/**
 * 迁移用一次性 DataStore 缓存（防止重复创建同一文件的 DataStore）
 */
private val migrationDataStoreCache = mutableMapOf<String, DataStore<Preferences>>()

@Synchronized
private fun getOrCreateMigrationDataStore(file: File): DataStore<Preferences> {
    val key = file.absolutePath
    return migrationDataStoreCache.getOrPut(key) {
        PreferenceDataStoreFactory.create(produceFile = { file })
    }
}

/**
 * 单用户 → 多账号迁移
 *
 * 首次升级时执行一次：
 * 1. 检查旧 auth_prefs / jcstaff_database / settings 文件是否存在
 * 2. 读取旧 AccountResponse，注册到 AccountRegistry
 * 3. 重命名文件为 per-user 命名
 * 4. 标记迁移完成
 */
suspend fun migrateAccountIfNeeded(context: Context) {
    if (AccountRegistry.isMigrationDone()) {
        Log.d(TAG, "Migration already done, skipping")
        return
    }

    Log.d(TAG, "Starting migration check")

    // 尝试从旧 auth_prefs 读取账号信息
    val oldAuthPrefsFile = File(context.filesDir, "datastore/auth_prefs.preferences_pb")
    if (!oldAuthPrefsFile.exists()) {
        Log.d(TAG, "No old auth_prefs found, marking migration done (fresh install)")
        AccountRegistry.setMigrationDone()
        return
    }

    // 读取旧 DataStore 中的账号信息
    val account = readOldAuthPrefs(context)
    if (account?.user == null) {
        Log.d(TAG, "No valid account in old auth_prefs, marking migration done")
        AccountRegistry.setMigrationDone()
        return
    }

    val userId = account.user!!.id
    Log.d(TAG, "Migrating existing account: ${account.user!!.name} (ID: $userId)")

    // 注册到 AccountRegistry
    val entry = AccountEntry(
        userId = userId,
        userName = account.user!!.name ?: "",
        userAccount = account.user!!.account ?: "",
        avatarUrl = account.user!!.profile_image_urls?.findAvatarUrl()
    )
    AccountRegistry.addAccount(entry)
    AccountRegistry.setActiveAccount(userId)

    // 迁移语言设置到全局 DataStore（语言是全局的，不随账号切换）
    migrateLanguageSetting(context)

    // 重命名数据文件为 per-user 名称
    renameFile(context, "datastore/auth_prefs.preferences_pb", "datastore/auth_prefs_$userId.preferences_pb")
    // 复制旧 settings 到 per-user（保留用户偏好），原文件已迁移语言到 global
    renameFile(context, "datastore/settings.preferences_pb", "datastore/settings_$userId.preferences_pb")

    // 重命名数据库文件
    val dbDir = context.getDatabasePath("jcstaff_database").parentFile
    if (dbDir != null) {
        renameDbFile(dbDir, "jcstaff_database", "jcstaff_db_$userId")
        renameDbFile(dbDir, "jcstaff_database-shm", "jcstaff_db_$userId-shm")
        renameDbFile(dbDir, "jcstaff_database-wal", "jcstaff_db_$userId-wal")
    }

    // 重命名缓存目录
    val oldCacheDir = File(context.cacheDir, "image_load_cache")
    if (oldCacheDir.exists()) {
        val newCacheDir = File(context.cacheDir, "image_load_cache_$userId")
        oldCacheDir.renameTo(newCacheDir)
        Log.d(TAG, "Renamed cache dir to image_load_cache_$userId")
    }

    // 清除迁移用缓存（文件已重命名，旧 DataStore 不再有效）
    migrationDataStoreCache.clear()

    AccountRegistry.setMigrationDone()
    Log.d(TAG, "Migration completed successfully")
}

private suspend fun migrateLanguageSetting(context: Context) {
    try {
        val oldSettingsFile = File(context.filesDir, "datastore/settings.preferences_pb")
        if (!oldSettingsFile.exists()) return

        val oldStore = getOrCreateMigrationDataStore(oldSettingsFile)
        val prefs = oldStore.data.first()
        val languageTag = prefs[stringPreferencesKey("selected_language")]
        if (languageTag != null) {
            // Write language to global settings store
            SettingsStore.setSelectedLanguage(languageTag)
            Log.d(TAG, "Migrated language setting: $languageTag")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to migrate language setting: ${e.message}")
    }
}

private suspend fun readOldAuthPrefs(context: Context): AccountResponse? {
    return try {
        val oldFile = File(context.filesDir, "datastore/auth_prefs.preferences_pb")
        val oldStore = getOrCreateMigrationDataStore(oldFile)
        val prefs = oldStore.data.first()
        val json = prefs[stringPreferencesKey("account_json")]
        if (json != null) {
            Gson().fromJson(json, AccountResponse::class.java)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read old auth prefs: ${e.message}")
        null
    }
}

private fun renameFile(context: Context, oldRelPath: String, newRelPath: String) {
    val oldFile = File(context.filesDir, oldRelPath)
    if (oldFile.exists()) {
        val newFile = File(context.filesDir, newRelPath)
        newFile.parentFile?.mkdirs()
        val success = oldFile.renameTo(newFile)
        Log.d(TAG, "Rename $oldRelPath → $newRelPath: $success")
    }
}

private fun renameDbFile(dbDir: File, oldName: String, newName: String) {
    val oldFile = File(dbDir, oldName)
    if (oldFile.exists()) {
        val newFile = File(dbDir, newName)
        val success = oldFile.renameTo(newFile)
        Log.d(TAG, "Rename DB $oldName → $newName: $success")
    }
}
