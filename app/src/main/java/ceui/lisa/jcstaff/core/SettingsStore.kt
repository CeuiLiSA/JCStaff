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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Global DataStore for language (shared across all accounts)
 */
private val Context.globalSettingsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "settings_global")

object SettingsStore {
    private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    private val IMAGE_CACHE_LIMIT_MB = intPreferencesKey("image_cache_limit_mb")
    private val HIDE_AI_CONTENT = booleanPreferencesKey("hide_ai_content")
    private val DOWNLOAD_FILENAME_TEMPLATE = stringPreferencesKey("download_filename_template")

    const val DEFAULT_FILENAME_TEMPLATE = "{id}_{title}_p{page}"

    private var dataStore: DataStore<Preferences>? = null
    private var globalDataStore: DataStore<Preferences>? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var globalSyncJob: Job? = null

    /**
     * Per-user DataStore 单例缓存：每个文件名只创建一个实例
     */
    private val dataStoreCache = mutableMapOf<String, DataStore<Preferences>>()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _imageCacheLimitMb = MutableStateFlow(DEFAULT_IMAGE_CACHE_LIMIT_MB)
    val imageCacheLimitMb: StateFlow<Int> = _imageCacheLimitMb.asStateFlow()

    private val _hideAiContent = MutableStateFlow(false)
    val hideAiContent: StateFlow<Boolean> = _hideAiContent.asStateFlow()

    private val _downloadFilenameTemplate = MutableStateFlow(DEFAULT_FILENAME_TEMPLATE)
    val downloadFilenameTemplate: StateFlow<String> = _downloadFilenameTemplate.asStateFlow()

    const val MIN_IMAGE_CACHE_LIMIT_MB = 256
    const val MAX_IMAGE_CACHE_LIMIT_MB = 4096
    const val DEFAULT_IMAGE_CACHE_LIMIT_MB = 512

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
                _imageCacheLimitMb.value = preferences[IMAGE_CACHE_LIMIT_MB] ?: DEFAULT_IMAGE_CACHE_LIMIT_MB
                _hideAiContent.value = preferences[HIDE_AI_CONTENT] ?: false
                _downloadFilenameTemplate.value = preferences[DOWNLOAD_FILENAME_TEMPLATE] ?: DEFAULT_FILENAME_TEMPLATE
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
        _imageCacheLimitMb.value = DEFAULT_IMAGE_CACHE_LIMIT_MB
        _hideAiContent.value = false
        _downloadFilenameTemplate.value = DEFAULT_FILENAME_TEMPLATE
        startSync()
    }

    suspend fun setHideAiContent(hide: Boolean) {
        _hideAiContent.value = hide
        ContentFilterManager.notifyFilterChanged()
        dataStore?.edit { preferences ->
            preferences[HIDE_AI_CONTENT] = hide
        }
    }

    suspend fun setImageCacheLimitMb(limitMb: Int) {
        val clamped = limitMb.coerceIn(MIN_IMAGE_CACHE_LIMIT_MB, MAX_IMAGE_CACHE_LIMIT_MB)
        _imageCacheLimitMb.value = clamped
        dataStore?.edit { preferences ->
            preferences[IMAGE_CACHE_LIMIT_MB] = clamped
        }
    }

    suspend fun setDownloadFilenameTemplate(template: String) {
        val effective = template.ifBlank { DEFAULT_FILENAME_TEMPLATE }
        _downloadFilenameTemplate.value = effective
        dataStore?.edit { preferences ->
            preferences[DOWNLOAD_FILENAME_TEMPLATE] = effective
        }
    }

    suspend fun setSelectedLanguage(tag: String) {
        _selectedLanguage.value = tag
        globalDataStore?.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = tag
        }
    }

    /**
     * 直接从 DataStore 读取语言设置，绕过 StateFlow 的初始值。
     * 用于 Application.onCreate() 中同步读取，避免和 startSync() 的竞态。
     */
    suspend fun readSelectedLanguageDirectly(): String? {
        return globalDataStore?.data?.first()?.get(SELECTED_LANGUAGE)
    }

}
