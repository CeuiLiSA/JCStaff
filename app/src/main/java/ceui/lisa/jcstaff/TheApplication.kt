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
import kotlinx.coroutines.flow.first
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
        val savedTag = runBlocking(Dispatchers.IO) {
            SettingsStore.selectedLanguage.first()
        }
        LanguageManager.initialize(savedTag)
    }
}