package ceui.lisa.jcstaff.network

import android.util.Log
import ceui.lisa.jcstaff.cache.ApiCacheManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Pixiv 网页抓取服务
 * 用于获取移动 API 不支持的功能（如 ugoira 排行榜、网页版搜索等）
 * 支持通过 ApiCacheManager 进行缓存
 */
object PixivWebScraper {

    private const val TAG = "PixivWebScraper"
    private const val WEB_BASE_URL = "https://www.pixiv.net"

    private val gson: Gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create()

    // Web Cookie 存储（需要从 WebView 登录或手动设置）
    private var webCookie: String? = null
    private var webUserId: String? = null

    /**
     * 设置 Web Cookie（从 WebView 登录获取）
     * @param cookie 完整的 Cookie 字符串（包含 PHPSESSID）
     * @param userId 用户 ID
     */
    fun setWebCredentials(cookie: String, userId: String) {
        webCookie = cookie
        webUserId = userId
        Log.d(TAG, "Web credentials set for user: $userId")
    }

    /**
     * 清除 Web Cookie
     */
    fun clearWebCredentials() {
        webCookie = null
        webUserId = null
        Log.d(TAG, "Web credentials cleared")
    }

    /**
     * 检查是否有 Web 认证
     */
    fun hasWebCredentials(): Boolean = webCookie != null && webUserId != null

    /**
     * 用于网页抓取的 OkHttpClient（HTML 页面）
     */
    private val webClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .addHeader(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    )
                    .addHeader("Accept-Language", "ja,en-US;q=0.9,en;q=0.8")
                    .addHeader("Referer", "https://www.pixiv.net/")

                webCookie?.let { requestBuilder.addHeader("Cookie", it) }

                chain.proceed(requestBuilder.build())
            }
            .followRedirects(true)
            .build()
    }

    /**
     * 用于 AJAX API 请求的 OkHttpClient（JSON 响应）
     */
    private val ajaxClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", "zh,en-US;q=0.9,en;q=0.8,ja;q=0.7")
                    .addHeader("Referer", "https://www.pixiv.net/")

                // 添加 Cookie 和 x-user-id
                webCookie?.let { requestBuilder.addHeader("Cookie", it) }
                webUserId?.let { requestBuilder.addHeader("x-user-id", it) }

                chain.proceed(requestBuilder.build())
            }
            .followRedirects(true)
            .build()
    }

    /**
     * 正则表达式匹配 __NEXT_DATA__ script 标签内容
     */
    private val nextDataPattern: Pattern = Pattern.compile(
        "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.+?)</script>",
        Pattern.DOTALL
    )

    // ==================== 网页版搜索 API ====================

    /**
     * 搜索标签顶部数据（插画、漫画、小说、热门作品）
     * API: /ajax/search/top/{tag}
     *
     * @param tag 标签名（会自动 URL 编码）
     * @param lang 语言（zh, ja, en 等）
     * @param forceRefresh 是否强制刷新
     */
    suspend fun searchTagTop(
        tag: String,
        lang: String = "zh",
        forceRefresh: Boolean = false
    ): Result<WebSearchResponse> = withContext(Dispatchers.IO) {
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        val url = "$WEB_BASE_URL/ajax/search/top/$encodedTag?lang=$lang"
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)

        // 1. 检查缓存
        if (!forceRefresh) {
            val cached = ApiCacheManager.get(cacheKey)
            if (cached != null) {
                Log.d(TAG, "✅ Cache hit for search: $tag")
                try {
                    val json = String(cached.responseBody, Charsets.UTF_8)
                    val response = gson.fromJson(json, WebSearchResponse::class.java)
                    if (response.error == false && response.body != null) {
                        return@withContext Result.success(response)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cache parse failed: ${e.message}")
                }
            }
        }

        // 2. 网络请求
        try {
            Log.d(TAG, "Fetching search top: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = ajaxClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Search request failed: ${response.code}")
                return@withContext tryStaleCache(cacheKey, WebSearchResponse::class.java)
                    ?: Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val json = response.body?.string()
            if (json.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            // 3. 解析
            val searchResponse = gson.fromJson(json, WebSearchResponse::class.java)

            if (searchResponse.error == true) {
                Log.e(TAG, "API error: ${searchResponse.message}")
                return@withContext Result.failure(Exception(searchResponse.message ?: "API error"))
            }

            // 4. 缓存
            ApiCacheManager.put(
                key = cacheKey,
                responseBody = json.toByteArray(Charsets.UTF_8),
                contentType = "application/json",
                httpCode = response.code,
                httpMessage = response.message
            )

            Log.d(TAG, "Search success: ${searchResponse.body?.illust?.total ?: 0} illusts")
            Result.success(searchResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search tag", e)
            tryStaleCache(cacheKey, WebSearchResponse::class.java)
                ?: Result.failure(e)
        }
    }

    /**
     * 获取标签详情信息（百科、翻译、相关标签等）
     * API: /ajax/search/tags/{tag}
     *
     * @param tag 标签名
     * @param lang 语言
     * @param forceRefresh 是否强制刷新
     */
    suspend fun getTagInfo(
        tag: String,
        lang: String = "zh",
        forceRefresh: Boolean = false
    ): Result<WebTagInfoResponse> = withContext(Dispatchers.IO) {
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        val url = "$WEB_BASE_URL/ajax/search/tags/$encodedTag?lang=$lang"
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)

        // 1. 检查缓存
        if (!forceRefresh) {
            val cached = ApiCacheManager.get(cacheKey)
            if (cached != null) {
                Log.d(TAG, "✅ Cache hit for tag info: $tag")
                try {
                    val json = String(cached.responseBody, Charsets.UTF_8)
                    val response = gson.fromJson(json, WebTagInfoResponse::class.java)
                    if (response.error == false && response.body != null) {
                        return@withContext Result.success(response)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cache parse failed: ${e.message}")
                }
            }
        }

        // 2. 网络请求
        try {
            Log.d(TAG, "Fetching tag info: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = ajaxClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Tag info request failed: ${response.code}")
                return@withContext tryStaleCache(cacheKey, WebTagInfoResponse::class.java)
                    ?: Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val json = response.body?.string()
            if (json.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            // 3. 解析
            val tagInfoResponse = gson.fromJson(json, WebTagInfoResponse::class.java)

            if (tagInfoResponse.error == true) {
                Log.e(TAG, "API error: ${tagInfoResponse.message}")
                return@withContext Result.failure(Exception(tagInfoResponse.message ?: "API error"))
            }

            // 4. 缓存
            ApiCacheManager.put(
                key = cacheKey,
                responseBody = json.toByteArray(Charsets.UTF_8),
                contentType = "application/json",
                httpCode = response.code,
                httpMessage = response.message
            )

            Log.d(TAG, "Tag info success: ${tagInfoResponse.body?.tag}")
            Result.success(tagInfoResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tag info", e)
            tryStaleCache(cacheKey, WebTagInfoResponse::class.java)
                ?: Result.failure(e)
        }
    }

    /**
     * 尝试从过期缓存获取数据
     */
    private suspend fun <T> tryStaleCache(cacheKey: String, clazz: Class<T>): Result<T>? {
        val staleCache = ApiCacheManager.getStale(cacheKey)
        if (staleCache != null) {
            try {
                Log.d(TAG, "📦 Using stale cache")
                val json = String(staleCache.responseBody, Charsets.UTF_8)
                val response = gson.fromJson(json, clazz)
                return Result.success(response)
            } catch (e: Exception) {
                Log.w(TAG, "Stale cache parse failed: ${e.message}")
            }
        }
        return null
    }

    // ==================== 排行榜 API ====================

    /**
     * 获取动图排行榜
     * @param mode 排行榜模式：daily, weekly, monthly 等
     * @param date 日期，格式 yyyyMMdd，null 表示最新
     * @param page 页码，从 1 开始
     * @param forceRefresh 是否强制刷新（忽略缓存）
     */
    suspend fun getUgoiraRanking(
        mode: String = "daily",
        date: String? = null,
        page: Int = 1,
        forceRefresh: Boolean = false
    ): Result<List<WebRankingItem>> = withContext(Dispatchers.IO) {
        getRanking(
            mode = mode,
            content = ContentType.UGOIRA,
            date = date,
            page = page,
            forceRefresh = forceRefresh
        )
    }

    /**
     * 获取排行榜（通用方法）
     * @param mode 排行榜模式：daily, weekly, monthly, rookie, original, male, female 等
     * @param content 内容类型：illust, manga, ugoira
     * @param date 日期，格式 yyyyMMdd
     * @param page 页码
     * @param forceRefresh 是否强制刷新（忽略缓存）
     */
    suspend fun getRanking(
        mode: String = "daily",
        content: String? = null,
        date: String? = null,
        page: Int = 1,
        forceRefresh: Boolean = false
    ): Result<List<WebRankingItem>> = withContext(Dispatchers.IO) {
        val url = buildRankingUrl(mode, content, date, page)
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)

        // 1. 检查缓存（非强制刷新时）
        if (!forceRefresh) {
            val cached = ApiCacheManager.get(cacheKey)
            if (cached != null) {
                Log.d(TAG, "✅ Cache hit for: $url")
                val html = String(cached.responseBody, Charsets.UTF_8)
                val items = parseRankingHtml(html)
                if (items.isNotEmpty()) {
                    return@withContext Result.success(items)
                }
                Log.w(TAG, "Cache parse failed, fetching from network")
            }
        }

        // 2. 网络请求
        try {
            Log.d(TAG, "Fetching ranking: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = webClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed: ${response.code}")
                val staleCache = ApiCacheManager.getStale(cacheKey)
                if (staleCache != null) {
                    Log.d(TAG, "📦 Using stale cache due to network error")
                    val html = String(staleCache.responseBody, Charsets.UTF_8)
                    val items = parseRankingHtml(html)
                    if (items.isNotEmpty()) {
                        return@withContext Result.success(items)
                    }
                }
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val html = response.body?.string()
            if (html.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            // 3. 解析数据
            val items = parseRankingHtml(html)
            Log.d(TAG, "Parsed ${items.size} items")

            // 4. 缓存响应
            if (items.isNotEmpty()) {
                ApiCacheManager.put(
                    key = cacheKey,
                    responseBody = html.toByteArray(Charsets.UTF_8),
                    contentType = "text/html",
                    httpCode = response.code,
                    httpMessage = response.message
                )
            }

            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ranking", e)
            val staleCache = ApiCacheManager.getStale(cacheKey)
            if (staleCache != null) {
                Log.d(TAG, "📦 Using stale cache due to exception: ${e.message}")
                val html = String(staleCache.responseBody, Charsets.UTF_8)
                val items = parseRankingHtml(html)
                if (items.isNotEmpty()) {
                    return@withContext Result.success(items)
                }
            }
            Result.failure(e)
        }
    }

    /**
     * 构建排行榜 URL
     */
    private fun buildRankingUrl(
        mode: String,
        content: String?,
        date: String?,
        page: Int
    ): String {
        val params = mutableListOf<String>()
        params.add("mode=$mode")

        if (!content.isNullOrEmpty()) {
            params.add("content=$content")
        }

        if (!date.isNullOrEmpty()) {
            params.add("date=$date")
        }

        if (page > 1) {
            params.add("p=$page")
        }

        return "$WEB_BASE_URL/ranking.php?${params.joinToString("&")}"
    }

    /**
     * 解析 HTML 中的 __NEXT_DATA__
     */
    private fun parseRankingHtml(html: String): List<WebRankingItem> {
        val matcher = nextDataPattern.matcher(html)

        if (!matcher.find()) {
            Log.w(TAG, "__NEXT_DATA__ not found in HTML")
            return emptyList()
        }

        val jsonString = matcher.group(1)
        if (jsonString.isNullOrEmpty()) {
            Log.w(TAG, "__NEXT_DATA__ content is empty")
            return emptyList()
        }

        return try {
            val response = gson.fromJson(jsonString, WebRankingResponse::class.java)
            response.props?.pageProps?.assign?.contents ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse __NEXT_DATA__ JSON", e)
            emptyList()
        }
    }

    /**
     * 网页排行榜模式枚举
     */
    object RankingMode {
        const val DAILY = "daily"
        const val WEEKLY = "weekly"
    }

    /**
     * 内容类型枚举
     */
    object ContentType {
        const val ILLUST = "illust"
        const val MANGA = "manga"
        const val UGOIRA = "ugoira"
    }
}
