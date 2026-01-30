package ceui.lisa.jcstaff.network

import android.util.Log
import ceui.lisa.jcstaff.cache.ApiCacheManager
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
        val request = chain.request().newBuilder()
            .addHeader("Referer", "https://app-api.pixiv.net/")
            .build()
        return chain.proceed(request)
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
            .addInterceptor(RefererInterceptor())
            .build()
    }

    private var _pixivApi: PixivApi? = null
    private var currentPkce: PKCEItem? = null
    private val gson: Gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create()

    private val cookieJar = object : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies.toMutableList()
            Log.d("CookieJar", "saveFromResponse [${url.host}] cookies:")
            cookies.forEach { cookie ->
                Log.d(
                    "CookieJar", "  ${cookie.name}=${cookie.value} " +
                            "(domain=${cookie.domain}, path=${cookie.path}, " +
                            "secure=${cookie.secure}, httpOnly=${cookie.httpOnly}, " +
                            "expiresAt=${cookie.expiresAt})"
                )
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host] ?: emptyList()
            Log.d("CookieJar", "loadForRequest [${url.host}] sending ${cookies.size} cookies:")
            cookies.forEach { cookie ->
                Log.d("CookieJar", "  ${cookie.name}=${cookie.value}")
            }
            return cookies
        }
    }

    val oAuthApi: OAuthApi by lazy {
        createOAuthClient().create(OAuthApi::class.java)
    }

    val pixivApi: PixivApi
        get() {
            return _pixivApi ?: createAppClient().create(PixivApi::class.java).also {
                _pixivApi = it
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
     * 清除 API 缓存（用于下拉刷新等场景）
     */
    fun clearApiCache() {
        ApiCacheManager.clearAllSync()
    }

    /**
     * 使指定 URL 的缓存失效
     */
    fun invalidateCache(url: String) {
        ApiCacheManager.invalidateSync(url)
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): String {
        return ApiCacheManager.getStatsSync()
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
     * 从缓存获取数据（即使过期也返回，用于 stale-while-revalidate）
     * @param path API 路径，如 "/v1/illust/recommended"
     * @param queryParams 查询参数
     * @param clazz 响应类型
     * @return 缓存的数据，如果没有缓存则返回 null
     */
    suspend fun <T> getFromStaleCache(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        clazz: Class<T>
    ): T? {
        val url = buildUrl(path, queryParams)
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)
        android.util.Log.d("PixivClient", "🔍 Looking for cache: $cacheKey")
        val cached = ApiCacheManager.getStale(cacheKey)
        if (cached == null) {
            android.util.Log.d("PixivClient", "❌ Cache not found for: $cacheKey")
            return null
        }
        android.util.Log.d("PixivClient", "✅ Cache found for: $cacheKey")

        return withContext(Dispatchers.Default) {
            try {
                val json = String(cached.responseBody, Charsets.UTF_8)
                gson.fromJson(json, clazz)
            } catch (e: Exception) {
                android.util.Log.e("PixivClient", "❗ Parse cache failed: ${e.message}")
                null
            }
        }
    }

    private fun buildUrl(path: String, queryParams: Map<String, String>): String {
        val baseUrl = APP_API_HOST + path
        if (queryParams.isEmpty()) return baseUrl

        val queryString = queryParams.entries.joinToString("&") { (key, value) ->
            "$key=$value"
        }
        return "$baseUrl?$queryString"
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
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
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
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(APP_API_HOST)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }
}