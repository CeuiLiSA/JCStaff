package ceui.lisa.jcstaff.core

import java.util.Locale

enum class AppLanguage(
    val tag: String,
    val acceptLanguage: String,
    val appAcceptLanguage: String,
    val displayName: String
) {
    SIMPLIFIED_CHINESE(
        tag = "zh-Hans",
        acceptLanguage = "zh-CN,zh-Hans;q=0.9",
        appAcceptLanguage = "zh-hans",
        displayName = "简体中文"
    ),
    TRADITIONAL_CHINESE(
        tag = "zh-Hant",
        acceptLanguage = "zh-TW,zh-Hant;q=0.9",
        appAcceptLanguage = "zh-hant",
        displayName = "繁體中文"
    ),
    JAPANESE(
        tag = "ja",
        acceptLanguage = "ja;q=1.0",
        appAcceptLanguage = "ja",
        displayName = "日本語"
    ),
    ENGLISH(
        tag = "en",
        acceptLanguage = "en-US,en;q=0.9",
        appAcceptLanguage = "en-us",
        displayName = "English"
    ),
    KOREAN(
        tag = "ko",
        acceptLanguage = "ko;q=1.0",
        appAcceptLanguage = "ko",
        displayName = "한국어"
    );

    companion object {
        fun fromTag(tag: String?): AppLanguage? {
            if (tag == null) return null
            return entries.find { it.tag.equals(tag, ignoreCase = true) }
        }

        fun fromSystemLocale(): AppLanguage {
            val locale = Locale.getDefault()
            val language = locale.language
            val script = locale.script
            val country = locale.country

            return when {
                language == "zh" && (script == "Hant" || country in listOf("TW", "HK", "MO")) ->
                    TRADITIONAL_CHINESE
                language == "zh" -> SIMPLIFIED_CHINESE
                language == "ja" -> JAPANESE
                language == "ko" -> KOREAN
                else -> ENGLISH
            }
        }
    }
}
