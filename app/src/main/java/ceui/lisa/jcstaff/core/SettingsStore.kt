package ceui.lisa.jcstaff.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsStore {
    private val SHOW_ILLUST_INFO = booleanPreferencesKey("show_illust_info")
    private val ILLUST_CARD_CORNER_RADIUS = intPreferencesKey("illust_card_corner_radius")
    private val GRID_SPACING_ENABLED = booleanPreferencesKey("grid_spacing_enabled")

    private var dataStore: DataStore<Preferences>? = null

    fun initialize(context: Context) {
        dataStore = context.dataStore
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
}