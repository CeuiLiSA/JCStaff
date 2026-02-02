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
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Pixiv 网页抓取服务
 * 用于获取移动 API 不支持的功能（如 ugoira 排行榜）
 * 支持通过 ApiCacheManager 进行缓存
 */
object PixivWebScraper {

    private const val TAG = "PixivWebScraper"
    private const val WEB_BASE_URL = "https://www.pixiv.net"

    private val gson: Gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .create()

    /**
     * 用于网页抓取的 OkHttpClient
     * 需要添加必要的 headers 来模拟浏览器请求
     */
    private val webClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
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
                    .build()
                chain.proceed(request)
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
                // 缓存解析失败，继续网络请求
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
                // 请求失败时尝试返回过期缓存
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

            // 4. 缓存响应（仅当解析成功时）
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
            // 网络异常时尝试返回过期缓存
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
