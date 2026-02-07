package ceui.lisa.jcstaff.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

data class LanguageHeaders(
    val acceptLanguage: String,
    val appAcceptLanguage: String
)

object LanguageManager {

    private val _currentHeaders = AtomicReference(
        LanguageHeaders(
            acceptLanguage = AppLanguage.SIMPLIFIED_CHINESE.acceptLanguage,
            appAcceptLanguage = AppLanguage.SIMPLIFIED_CHINESE.appAcceptLanguage
        )
    )
    val currentHeaders: LanguageHeaders get() = _currentHeaders.get()

    private val _currentLanguage = MutableStateFlow<AppLanguage?>(null)
    val currentLanguage: StateFlow<AppLanguage?> = _currentLanguage.asStateFlow()

    /**
     * 语言是否已加载完成（区分"还没读完DataStore"和"确实没选过语言"）
     */
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isLanguageSelected = MutableStateFlow(false)
    val isLanguageSelected: StateFlow<Boolean> = _isLanguageSelected.asStateFlow()

    fun initialize(savedTag: String?) {
        val language = AppLanguage.fromTag(savedTag)
        if (language != null) {
            _currentLanguage.value = language
            _currentHeaders.set(
                LanguageHeaders(
                    acceptLanguage = language.acceptLanguage,
                    appAcceptLanguage = language.appAcceptLanguage
                )
            )
            _isLanguageSelected.value = true
        } else {
            _isLanguageSelected.value = false
        }
        _isInitialized.value = true
    }

    fun resetLanguageSelection() {
        _isLanguageSelected.value = false
    }

    suspend fun setLanguage(language: AppLanguage) {
        SettingsStore.setSelectedLanguage(language.tag)
        _currentLanguage.value = language
        _currentHeaders.set(
            LanguageHeaders(
                acceptLanguage = language.acceptLanguage,
                appAcceptLanguage = language.appAcceptLanguage
            )
        )
        _isLanguageSelected.value = true
        // 不在运行时调 setApplicationLocales()，避免 Activity 重建。
        // 语言切换通过 Compose 的 LocalizedContext 即时生效。
        // 系统级 locale 在下次冷启动时由 applySystemLocale() 同步。
    }

    /**
     * 同步系统级 per-app locale。仅在 Application.onCreate() 中调用，
     * 此时 Activity 尚未创建，不会触发重建。
     */
    fun applySystemLocale(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
