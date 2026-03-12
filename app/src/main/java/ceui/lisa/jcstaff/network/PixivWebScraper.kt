package ceui.lisa.jcstaff.network

import android.util.Log
import ceui.lisa.jcstaff.cache.ApiCacheManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
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

    // Web Cookie 存储（可选，用于 R18 内容和个人收藏状态）
    private var webCookie: String? = null
    private var webUserId: String? = null

    /**
     * 设置 Web Cookie（可选，用于访问 R18 内容）
     * @param cookie 完整的 Cookie 字符串（包含 PHPSESSID）
     * @param userId 用户 ID
     */
    fun setWebCredentials(cookie: String, userId: String) {
        webCookie = cookie
        webUserId = userId
        Log.d(TAG, "✅ Web credentials set for user: $userId")
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
     * 检查是否有 Web 认证（可选，大部分功能不需要）
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

    // ==================== 珍藏册 API ====================

    /**
     * 获取珍藏册详情
     * API: /ajax/collection/{collectionId}
     *
     * @param collectionId 珍藏册 ID
     * @param lang 语言
     * @param forceRefresh 是否强制刷新
     */
    suspend fun getCollection(
        collectionId: String,
        lang: String = "zh",
        forceRefresh: Boolean = false
    ): Result<CollectionResponse> = withContext(Dispatchers.IO) {
        val url = "$WEB_BASE_URL/ajax/collection/$collectionId?lang=$lang"
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)

        if (!forceRefresh) {
            val cached = ApiCacheManager.get(cacheKey)
            if (cached != null) {
                try {
                    val json = String(cached.responseBody, Charsets.UTF_8)
                    val response = gson.fromJson(json, CollectionResponse::class.java)
                    if (response.error == false && response.body != null) {
                        return@withContext Result.success(response)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cache parse failed: ${e.message}")
                }
            }
        }

        try {
            Log.d(TAG, "Fetching collection: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = ajaxClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Collection request failed: ${response.code}")
                return@withContext tryStaleCache(cacheKey, CollectionResponse::class.java)
                    ?: Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val json = response.body?.string()
            if (json.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val collectionResponse = gson.fromJson(json, CollectionResponse::class.java)

            if (collectionResponse.error == true) {
                Log.e(TAG, "API error: ${collectionResponse.message}")
                return@withContext Result.failure(Exception(collectionResponse.message ?: "API error"))
            }

            ApiCacheManager.put(
                key = cacheKey,
                responseBody = json.toByteArray(Charsets.UTF_8),
                contentType = "application/json",
                httpCode = response.code,
                httpMessage = response.message
            )

            Log.d(TAG, "Collection success: ${collectionResponse.body?.data?.detail?.tiles?.size ?: 0} tiles")
            Result.success(collectionResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get collection", e)
            tryStaleCache(cacheKey, CollectionResponse::class.java)
                ?: Result.failure(e)
        }
    }

    /**
     * 获取用户的珍藏册 ID 列表
     * API: /ajax/user/{userId}/profile/all
     *
     * @param userId 用户 ID
     * @param lang 语言
     */
    suspend fun getUserCollectionIds(
        userId: String,
        lang: String = "zh"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val url = "$WEB_BASE_URL/ajax/user/$userId/profile/all?lang=$lang"
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)

        val cached = ApiCacheManager.get(cacheKey)
        if (cached != null) {
            try {
                val json = String(cached.responseBody, Charsets.UTF_8)
                val response = gson.fromJson(json, UserProfileAllResponse::class.java)
                if (response.error == false) {
                    val ids = response.body?.collectionIds ?: emptyList()
                    return@withContext Result.success(ids)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cache parse failed: ${e.message}")
            }
        }

        try {
            Log.d(TAG, "Fetching user collection ids: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = ajaxClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val json = response.body?.string()
            if (json.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val profileResponse = gson.fromJson(json, UserProfileAllResponse::class.java)

            if (profileResponse.error == true) {
                return@withContext Result.failure(Exception(profileResponse.message ?: "API error"))
            }

            ApiCacheManager.put(
                key = cacheKey,
                responseBody = json.toByteArray(Charsets.UTF_8),
                contentType = "application/json",
                httpCode = response.code,
                httpMessage = response.message
            )

            val ids = profileResponse.body?.collectionIds ?: emptyList()
            Log.d(TAG, "User has ${ids.size} collections")
            Result.success(ids)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user collection ids", e)
            Result.failure(e)
        }
    }

    /**
     * 获取珍藏册推荐标签列表
     * API: /ajax/collections/search/recommended_tags
     */
    suspend fun getCollectionRecommendedTags(
        lang: String = "zh"
    ): Result<CollectionRecommendedTagsResponse> = withContext(Dispatchers.IO) {
        val url = "$WEB_BASE_URL/ajax/collections/search/recommended_tags?lang=$lang"
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)

        val cached = ApiCacheManager.get(cacheKey)
        if (cached != null) {
            try {
                val json = String(cached.responseBody, Charsets.UTF_8)
                val response = gson.fromJson(json, CollectionRecommendedTagsResponse::class.java)
                if (response.error == false && response.body != null) {
                    return@withContext Result.success(response)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cache parse failed: ${e.message}")
            }
        }

        try {
            Log.d(TAG, "Fetching collection recommended tags: $url")
            val request = Request.Builder().url(url).get().build()
            val response = ajaxClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext tryStaleCache(cacheKey, CollectionRecommendedTagsResponse::class.java)
                    ?: Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val json = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val result = gson.fromJson(json, CollectionRecommendedTagsResponse::class.java)

            if (result.error == true) {
                return@withContext Result.failure(Exception(result.message ?: "API error"))
            }

            ApiCacheManager.put(
                key = cacheKey,
                responseBody = json.toByteArray(Charsets.UTF_8),
                contentType = "application/json",
                httpCode = response.code,
                httpMessage = response.message
            )

            Log.d(TAG, "Recommended tags: ${result.body?.recommendedTags?.size ?: 0}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recommended tags", e)
            tryStaleCache(cacheKey, CollectionRecommendedTagsResponse::class.java)
                ?: Result.failure(e)
        }
    }

    /**
     * 搜索珍藏册
     * API: /ajax/collections/search
     *
     * @param word 搜索关键词（标签名）
     * @param order 排序：date_d（最新）, date_a（最早）
     * @param mode 模式：all, safe, r18
     * @param sMode 搜索模式：s_tag（标签匹配）
     * @param page 页码，从 1 开始
     * @param lang 语言
     */
    suspend fun searchCollections(
        word: String = "",
        order: String = "date_d",
        mode: String = "all",
        sMode: String = "s_tag",
        page: Int = 1,
        lang: String = "zh",
        forceRefresh: Boolean = false
    ): Result<CollectionSearchResponse> = withContext(Dispatchers.IO) {
        val encodedWord = URLEncoder.encode(word, "UTF-8")
        val url = "$WEB_BASE_URL/ajax/collections/search?word=$encodedWord&order=$order&mode=$mode&sMode=$sMode&lang=$lang&p=$page"
        val cacheKey = ApiCacheManager.buildCacheKey("GET", url)

        if (!forceRefresh) {
            val cached = ApiCacheManager.get(cacheKey)
            if (cached != null) {
                try {
                    val json = String(cached.responseBody, Charsets.UTF_8)
                    val response = gson.fromJson(json, CollectionSearchResponse::class.java)
                    if (response.error == false && response.body != null) {
                        return@withContext Result.success(response)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cache parse failed: ${e.message}")
                }
            }
        }

        try {
            Log.d(TAG, "Searching collections: $url")
            val request = Request.Builder().url(url).get().build()
            val response = ajaxClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext tryStaleCache(cacheKey, CollectionSearchResponse::class.java)
                    ?: Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val json = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val result = gson.fromJson(json, CollectionSearchResponse::class.java)

            if (result.error == true) {
                return@withContext Result.failure(Exception(result.message ?: "API error"))
            }

            ApiCacheManager.put(
                key = cacheKey,
                responseBody = json.toByteArray(Charsets.UTF_8),
                contentType = "application/json",
                httpCode = response.code,
                httpMessage = response.message
            )

            val collections = result.body?.thumbnails?.collection ?: emptyList()
            val total = result.body?.data?.total ?: 0
            Log.d(TAG, "Collection search: ${collections.size} results, $total total")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search collections", e)
            tryStaleCache(cacheKey, CollectionSearchResponse::class.java)
                ?: Result.failure(e)
        }
    }

    /**
     * 获取推荐珍藏册（编辑精选）
     * 从 /collection 页面的 __NEXT_DATA__ 中提取 recommendCollectionIds
     * 及 serverSerializedPreloadedState 中的珍藏册元数据
     */
    suspend fun getRecommendedCollections(
        forceRefresh: Boolean = false
    ): Result<List<CollectionSummary>> = withContext(Dispatchers.IO) {
        val url = "$WEB_BASE_URL/collection"
        val cacheKey = ApiCacheManager.buildCacheKey("GET", "$url#recommended")

        if (!forceRefresh) {
            val cached = ApiCacheManager.get(cacheKey)
            if (cached != null) {
                try {
                    val html = String(cached.responseBody, Charsets.UTF_8)
                    val result = parseRecommendedCollections(html)
                    if (result.isNotEmpty()) {
                        return@withContext Result.success(result)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cache parse failed: ${e.message}")
                }
            }
        }

        try {
            Log.d(TAG, "Fetching recommended collections from: $url")
            val request = Request.Builder().url(url).get().build()
            val response = webClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val staleCache = ApiCacheManager.getStale(cacheKey)
                if (staleCache != null) {
                    val html = String(staleCache.responseBody, Charsets.UTF_8)
                    val result = parseRecommendedCollections(html)
                    if (result.isNotEmpty()) return@withContext Result.success(result)
                }
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val html = response.body?.string()
            if (html.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Empty response"))
            }

            val result = parseRecommendedCollections(html)
            if (result.isNotEmpty()) {
                ApiCacheManager.put(
                    key = cacheKey,
                    responseBody = html.toByteArray(Charsets.UTF_8),
                    contentType = "text/html",
                    httpCode = response.code,
                    httpMessage = response.message
                )
            }
            Log.d(TAG, "Recommended collections: ${result.size}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recommended collections", e)
            val staleCache = ApiCacheManager.getStale(cacheKey)
            if (staleCache != null) {
                val html = String(staleCache.responseBody, Charsets.UTF_8)
                val result = parseRecommendedCollections(html)
                if (result.isNotEmpty()) return@withContext Result.success(result)
            }
            Result.failure(e)
        }
    }

    private fun parseRecommendedCollections(html: String): List<CollectionSummary> {
        val matcher = nextDataPattern.matcher(html)
        if (!matcher.find()) return emptyList()

        val jsonString = matcher.group(1) ?: return emptyList()

        return try {
            val root = gson.fromJson(jsonString, JsonObject::class.java)
            val pageProps = root
                .getAsJsonObject("props")
                ?.getAsJsonObject("pageProps") ?: return emptyList()

            val page = pageProps.getAsJsonObject("page")
            val recommendIds = page?.getAsJsonArray("recommendCollectionIds")
                ?.map { it.asString } ?: emptyList()

            if (recommendIds.isEmpty()) return emptyList()

            // Parse serverSerializedPreloadedState for collection metadata
            val preloadedStateStr = pageProps.get("serverSerializedPreloadedState")?.asString
                ?: return emptyList()
            val preloadedState = gson.fromJson(preloadedStateStr, JsonObject::class.java)
            val thumbnailCollection = preloadedState
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonObject("collection") ?: return emptyList()

            recommendIds.mapNotNull { id ->
                thumbnailCollection.get(id)?.let { element ->
                    try {
                        gson.fromJson(element, CollectionSummary::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recommended collections", e)
            emptyList()
        }
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
