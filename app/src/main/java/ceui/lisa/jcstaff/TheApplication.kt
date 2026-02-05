package ceui.lisa.jcstaff

import android.app.Application
import ceui.lisa.jcstaff.auth.AccountRegistry
import ceui.lisa.jcstaff.core.LanguageManager
import ceui.lisa.jcstaff.core.SettingsStore
import ceui.lisa.jcstaff.network.PixivClient
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TheApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        AccountRegistry.initialize(this)

        // 设置全局 Coil ImageLoader，使用共享的 imageClient（带 Referer 头）
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { PixivClient.imageClient }
                .build()
        )

        // 初始化全局设置（语言等）
        SettingsStore.initialize(this)

        applicationScope.launch {
            val savedTag = SettingsStore.selectedLanguage.first()
            LanguageManager.initialize(savedTag)
        }
    }
}