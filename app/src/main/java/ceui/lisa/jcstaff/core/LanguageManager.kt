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
        val localeList = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
