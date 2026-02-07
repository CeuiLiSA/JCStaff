package ceui.lisa.jcstaff.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var globalSyncJob: Job? = null

    /**
     * Per-user DataStore 单例缓存：每个文件名只创建一个实例
     */
    private val dataStoreCache = mutableMapOf<String, DataStore<Preferences>>()

    // StateFlow 缓存当前值，避免 Cold Flow 导致的闪烁
    private val _showIllustInfo = MutableStateFlow(true)
    val showIllustInfo: StateFlow<Boolean> = _showIllustInfo.asStateFlow()

    private val _illustCardCornerRadius = MutableStateFlow(8)
    val illustCardCornerRadius: StateFlow<Int> = _illustCardCornerRadius.asStateFlow()

    private val _gridSpacingEnabled = MutableStateFlow(true)
    val gridSpacingEnabled: StateFlow<Boolean> = _gridSpacingEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    @Synchronized
    private fun getOrCreateDataStore(context: Context, name: String): DataStore<Preferences> {
        return dataStoreCache.getOrPut(name) {
            PreferenceDataStoreFactory.create(
                produceFile = { context.applicationContext.preferencesDataStoreFile(name) }
            )
        }
    }

    /**
     * 启动同步 DataStore → StateFlow 的协程
     */
    private fun startSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            dataStore?.data?.collect { preferences ->
                _showIllustInfo.value = preferences[SHOW_ILLUST_INFO] ?: true
                _illustCardCornerRadius.value = preferences[ILLUST_CARD_CORNER_RADIUS] ?: 8
                _gridSpacingEnabled.value = preferences[GRID_SPACING_ENABLED] ?: true
            }
        }
        globalSyncJob?.cancel()
        globalSyncJob = scope.launch {
            globalDataStore?.data?.collect { preferences ->
                _selectedLanguage.value = preferences[SELECTED_LANGUAGE]
            }
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
        startSync()
    }

    /**
     * Initialize per-user settings
     */
    fun initialize(context: Context, userId: Long) {
        globalDataStore = context.globalSettingsDataStore
        dataStore = getOrCreateDataStore(context, "settings_$userId")
        startSync()
    }

    /**
     * Reset per-user settings (keeps global)
     */
    fun reset() {
        dataStore = globalDataStore
        // 重置为默认值
        _showIllustInfo.value = true
        _illustCardCornerRadius.value = 8
        _gridSpacingEnabled.value = true
        startSync()
    }

    suspend fun setShowIllustInfo(show: Boolean) {
        _showIllustInfo.value = show
        dataStore?.edit { preferences ->
            preferences[SHOW_ILLUST_INFO] = show
        }
    }

    suspend fun setIllustCardCornerRadius(radius: Int) {
        val clamped = radius.coerceIn(0, 24)
        _illustCardCornerRadius.value = clamped
        dataStore?.edit { preferences ->
            preferences[ILLUST_CARD_CORNER_RADIUS] = clamped
        }
    }

    suspend fun setGridSpacingEnabled(enabled: Boolean) {
        _gridSpacingEnabled.value = enabled
        dataStore?.edit { preferences ->
            preferences[GRID_SPACING_ENABLED] = enabled
        }
    }

    suspend fun setSelectedLanguage(tag: String) {
        _selectedLanguage.value = tag
        globalDataStore?.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = tag
        }
    }

}
