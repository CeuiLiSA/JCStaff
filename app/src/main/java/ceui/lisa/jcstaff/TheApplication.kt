package ceui.lisa.jcstaff

import android.app.Application
import ceui.lisa.jcstaff.auth.AccountRegistry
import ceui.lisa.jcstaff.core.ContentFilterManager
import ceui.lisa.jcstaff.core.LanguageManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.network.PixivClient
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TheApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AccountRegistry.initialize(this)
        ContentFilterManager.initialize(this)

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { PixivClient.imageClient }
                .build()
        )

        SettingsStore.initialize(this)

        // Initialize language synchronously to avoid flash of default language
        // 直接从 DataStore 读取，不用 StateFlow（startSync 是异步的，StateFlow 初始值为 null）
        val savedTag = runBlocking(Dispatchers.IO) {
            SettingsStore.readSelectedLanguageDirectly()
        }
        LanguageManager.initialize(savedTag)

        // 在 Activity 创建前同步系统 locale，不会触发重建
        val language = LanguageManager.currentLanguage.value
        if (language != null) {
            LanguageManager.applySystemLocale(language)
        }
    }
}