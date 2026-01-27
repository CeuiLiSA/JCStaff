package ceui.lisa.jcstaff.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Global DataStore for language (shared across all accounts)
 */
private val Context.globalSettingsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "settings_global")

object SettingsStore {
    private val SHOW_ILLUST_INFO = booleanPreferencesKey("show_illust_info")
    private val ILLUST_CARD_CORNER_RADIUS = intPreferencesKey("illust_card_corner_radius")
    private val GRID_SPACING_ENABLED = booleanPreferencesKey("grid_spacing_enabled")
    private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")

    private var dataStore: DataStore<Preferences>? = null
    private var globalDataStore: DataStore<Preferences>? = null

    /**
     * Per-user DataStore 单例缓存：每个文件名只创建一个实例
     */
    private val dataStoreCache = mutableMapOf<String, DataStore<Preferences>>()

    @Synchronized
    private fun getOrCreateDataStore(context: Context, name: String): DataStore<Preferences> {
        return dataStoreCache.getOrPut(name) {
            PreferenceDataStoreFactory.create(
                produceFile = { context.applicationContext.preferencesDataStoreFile(name) }
            )
        }
    }

    /**
     * Legacy initialize (uses global settings datastore for language)
     */
    fun initialize(context: Context) {
        globalDataStore = context.globalSettingsDataStore
        // For backward compat: if no per-user store set yet, use global for everything
        if (dataStore == null) {
            dataStore = globalDataStore
        }
    }

    /**
     * Initialize per-user settings
     */
    fun initialize(context: Context, userId: Long) {
        globalDataStore = context.globalSettingsDataStore
        dataStore = getOrCreateDataStore(context, "settings_$userId")
    }

    /**
     * Reset per-user settings (keeps global)
     */
    fun reset() {
        dataStore = globalDataStore
    }

    /**
     * 是否在 IllustCard 上显示标题和作者名
     * 默认为 true
     */
    val showIllustInfo: Flow<Boolean>
        get() = dataStore?.data?.map { preferences ->
            preferences[SHOW_ILLUST_INFO] ?: true
        } ?: kotlinx.coroutines.flow.flowOf(true)

    suspend fun setShowIllustInfo(show: Boolean) {
        dataStore?.edit { preferences ->
            preferences[SHOW_ILLUST_INFO] = show
        }
    }

    /**
     * IllustCard 圆角大小 (dp)
     * 默认为 8，范围 0-24
     */
    val illustCardCornerRadius: Flow<Int>
        get() = dataStore?.data?.map { preferences ->
            preferences[ILLUST_CARD_CORNER_RADIUS] ?: 8
        } ?: kotlinx.coroutines.flow.flowOf(8)

    suspend fun setIllustCardCornerRadius(radius: Int) {
        dataStore?.edit { preferences ->
            preferences[ILLUST_CARD_CORNER_RADIUS] = radius.coerceIn(0, 24)
        }
    }

    /**
     * 瀑布流是否开启间距
     * 默认为 true（开启，间距 8dp）
     * 关闭时使用最小间距 1px
     */
    val gridSpacingEnabled: Flow<Boolean>
        get() = dataStore?.data?.map { preferences ->
            preferences[GRID_SPACING_ENABLED] ?: true
        } ?: kotlinx.coroutines.flow.flowOf(true)

    suspend fun setGridSpacingEnabled(enabled: Boolean) {
        dataStore?.edit { preferences ->
            preferences[GRID_SPACING_ENABLED] = enabled
        }
    }

    /**
     * Language is global (not per-account)
     */
    val selectedLanguage: Flow<String?>
        get() = globalDataStore?.data?.map { preferences ->
            preferences[SELECTED_LANGUAGE]
        } ?: kotlinx.coroutines.flow.flowOf(null)

    suspend fun setSelectedLanguage(tag: String) {
        globalDataStore?.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = tag
        }
    }

    fun getSelectedLanguageBlocking(): String? {
        return runBlocking(Dispatchers.IO) {
            globalDataStore?.data?.map { preferences ->
                preferences[SELECTED_LANGUAGE]
            }?.first()
        }
    }
}
