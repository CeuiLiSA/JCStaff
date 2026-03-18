package ceui.lisa.jcstaff.network

import ceui.lisa.jcstaff.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Interceptor that adds Referer header for Pixiv image downloads
 */
class RefererInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val host = chain.request().url.host
        val requestBuilder = chain.request().newBuilder()

        if (host.endsWith("pixiv.net")) {
            // embed.pixiv.net 等 pixiv 域名需要浏览器级 headers
            requestBuilder
                .addHeader("Referer", "https://www.pixiv.net/")
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
        } else {
            requestBuilder.addHeader("Referer", "https://app-api.pixiv.net/")
        }

        return chain.proceed(requestBuilder.build())
    }
}

object PixivClient {

    const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    const val GRANT_TYPE_AUTH_CODE = "authorization_code"
    const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    const val CALLBACK_URL = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"

    private const val APP_API_HOST = "https://app-api.pixiv.net"
    private const val OAUTH_HOST = "https://oauth.secure.pixiv.net"
    private const val REQUEST_TIMEOUT = 15L
    private const val IMAGE_TIMEOUT = 60L

    /**
     * Shared OkHttpClient for image downloads with Referer header.
     * Use this for Coil, image downloads, ugoira downloads, etc.
     * Can be extended with .newBuilder() if additional interceptors are needed.
     */
    val imageClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(IMAGE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(IMAGE_TIMEOUT, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1)) // Pixiv CDN drops HTTP/2 streams mid-download
            .addInterceptor(RefererInterceptor())
            .build()
    }

    @Volatile
    private var _pixivApi: PixivApi? = null
    private var currentPkce: PKCEItem? = null
    private val gson: Gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create()

    private val cookieJar = object : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies.toMutableList()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    val oAuthApi: OAuthApi by lazy {
        createOAuthClient().create(OAuthApi::class.java)
    }

    val pixivApi: PixivApi
        get() {
            return _pixivApi ?: synchronized(this) {
                _pixivApi ?: createAppClient().create(PixivApi::class.java).also {
                    _pixivApi = it
                }
            }
        }

    /**
     * 初始化 token
     */
    fun initializeTokens(accessToken: String?, refreshToken: String?) {
        TokenManager.setTokens(accessToken, refreshToken)
        _pixivApi = null
    }

    /**
     * 执行 token 刷新 API 调用
     * 供 AuthRepository 的刷新回调使用
     */
    suspend fun refreshTokenApi(refreshToken: String): AccountResponse {
        return oAuthApi.refreshToken(
            clientId = CLIENT_ID,
            clientSecret = CLIENT_SECRET,
            grantType = GRANT_TYPE_REFRESH_TOKEN,
            refreshToken = refreshToken
        )
    }

    fun resetClient() {
        _pixivApi = null
        TokenManager.clear()
    }


    /**
     * 通用分页加载方法
     * 使用 next_url 加载下一页数据，返回解析后的响应对象
     * @param nextUrl 下一页的 URL
     * @param clazz 响应类型
     * @return 解析后的响应对象
     */
    suspend fun <T> getNextPage(nextUrl: String, clazz: Class<T>): T {
        val responseBody = pixivApi.getNextPage(nextUrl)
        return withContext(Dispatchers.Default) {
            val json = responseBody.string()
            gson.fromJson(json, clazz)
        }
    }

    /**
     * 获取小说正文：调用 webview/v2/novel，用 Jsoup 从 HTML 中提取嵌入的 pixiv JSON
     */
    suspend fun getNovelText(novelId: Long): NovelTextResponse {
        val responseBody = pixivApi.getNovelWebview(novelId)
        return withContext(Dispatchers.Default) {
            val html = responseBody.string()
            val pixivObj = parsePixivHtmlObject(html)
                ?: throw Exception("Failed to extract novel content")
            val webNovel = pixivObj.novel
                ?: throw Exception("Novel object is null")
            NovelTextResponse(
                novel_text = webNovel.text,
                series_prev = webNovel.seriesNavigation?.prevNovel?.toNovel(),
                series_next = webNovel.seriesNavigation?.nextNovel?.toNovel(),
                webNovel = webNovel
            )
        }
    }

    /**
     * 从 webview HTML 中解析 pixiv 对象（学习自 Shaft 的 WebNovelParser）
     * 用 Jsoup 解析 HTML，找到 Object.defineProperty(window, 'pixiv') 的 script，
     * 提取 value: {...} 中的 JSON，去掉尾逗号后用 Gson 反序列化
     */
    private fun parsePixivHtmlObject(html: String): PixivHtmlObject? {
        val doc = org.jsoup.Jsoup.parse(html)
        for (script in doc.getElementsByTag("script")) {
            val content = script.html()
            if (content.contains("Object.defineProperty(window, 'pixiv'")) {
                val start = content.indexOf("value: {") + 7
                val end = content.indexOf("});", start)
                if (start < 7 || end < 0) continue
                val trailingCommaRegex = ",(?=\\s*[}\\]])".toRegex()
                val json = content.substring(start, end).trim().replace(trailingCommaRegex, "")
                return gson.fromJson(json, PixivHtmlObject::class.java)
            }
        }
        return null
    }

    fun getPkce(): PKCEItem {
        return currentPkce ?: PKCEItem.create().also {
            currentPkce = it
        }
    }

    fun resetPkce() {
        currentPkce = null
    }

    fun getLoginUrl(): String {
        val pkce = getPkce()
        return "https://app-api.pixiv.net/web/v1/login?" +
                "code_challenge=${pkce.challenge}&" +
                "code_challenge_method=S256&" +
                "client=pixiv-android"
    }

    fun getSignupUrl(): String {
        val pkce = getPkce()
        return "https://app-api.pixiv.net/web/v1/provisional-accounts/create?" +
                "code_challenge=${pkce.challenge}&" +
                "code_challenge_method=S256&" +
                "client=pixiv-android"
    }

    private fun createOAuthClient(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .cookieJar(cookieJar)
            .addInterceptor(HeaderInterceptor())
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(OAUTH_HOST)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    private fun createAppClient(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .cookieJar(cookieJar)
            .addInterceptor(ApiCacheInterceptor())
            .addInterceptor(TokenRefreshInterceptor())
            .addInterceptor(HeaderInterceptor { TokenManager.getAccessToken() })
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(APP_API_HOST)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }
}