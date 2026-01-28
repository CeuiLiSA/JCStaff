package ceui.lisa.jcstaff.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface PixivApi {

    @GET("/v1/illust/recommended")
    suspend fun getRecommendedIllusts(
        @Query("content_type") contentType: String = "illust",
        @Query("include_ranking_illusts") includeRankingLabel: Boolean = true,
        @Query("filter") filter: String = "for_ios"
    ): HomeIllustResponse

    @GET("/v1/illust/ranking")
    suspend fun getRankingIllusts(
        @Query("mode") mode: String = "day",
        @Query("filter") filter: String = "for_ios",
        @Query("offset") offset: Int? = null
    ): IllustResponse

    @GET("/v1/trending-tags/illust")
    suspend fun getTrendingTags(
        @Query("filter") filter: String = "for_ios"
    ): TrendingTagsResponse

    @GET("/v1/trending-tags/novel")
    suspend fun getTrendingNovelTags(
        @Query("filter") filter: String = "for_ios"
    ): TrendingTagsResponse

    @GET("/v1/search/illust")
    suspend fun searchIllusts(
        @Query("word") word: String,
        @Query("search_target") searchTarget: String = "partial_match_for_tags",
        @Query("sort") sort: String = "date_desc",
        @Query("filter") filter: String = "for_ios",
        @Query("offset") offset: Int? = null,
        @Query("include_translated_tag_results") includeTranslatedTagResults: Boolean = true,
        @Query("merge_plain_keyword_results") mergePlainKeywordResults: Boolean = true
    ): IllustResponse

    @GET("/v1/illust/detail")
    suspend fun getIllustDetail(
        @Query("illust_id") illustId: Long
    ): SingleIllustResponse

    @GET("/v2/illust/related")
    suspend fun getRelatedIllusts(
        @Query("illust_id") illustId: Long,
        @Query("filter") filter: String = "for_ios"
    ): IllustResponse

    @GET("/v2/illust/follow")
    suspend fun getFollowingIllusts(
        @Query("restrict") restrict: String = "public",
        @Query("offset") offset: Int? = null
    ): IllustResponse

    @GET("/v1/user/illusts")
    suspend fun getUserIllusts(
        @Query("user_id") userId: Long,
        @Query("type") type: String = "illust",
        @Query("filter") filter: String = "for_ios",
        @Query("offset") offset: Int? = null
    ): IllustResponse

    @GET("/v1/user/bookmarks/illust")
    suspend fun getUserBookmarks(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String = "public",
        @Query("filter") filter: String = "for_ios",
        @Query("max_bookmark_id") maxBookmarkId: Long? = null
    ): IllustResponse

    @GET("/v1/user/detail")
    suspend fun getUserDetail(
        @Query("user_id") userId: Long,
        @Query("filter") filter: String = "for_ios"
    ): UserDetailResponse

    @GET("/v1/user/recommended")
    suspend fun getRecommendedUsers(
        @Query("filter") filter: String = "for_ios",
        @Query("offset") offset: Int? = null
    ): UserPreviewResponse

    @FormUrlEncoded
    @POST("/v2/illust/bookmark/add")
    suspend fun addBookmark(
        @Field("illust_id") illustId: Long,
        @Field("restrict") restrict: String = "public"
    ): Unit

    @FormUrlEncoded
    @POST("/v1/illust/bookmark/delete")
    suspend fun deleteBookmark(
        @Field("illust_id") illustId: Long
    ): Unit

    @FormUrlEncoded
    @POST("/v1/user/follow/add")
    suspend fun followUser(
        @Field("user_id") userId: Long,
        @Field("restrict") restrict: String = "public"
    ): Unit

    @FormUrlEncoded
    @POST("/v1/user/follow/delete")
    suspend fun unfollowUser(
        @Field("user_id") userId: Long
    ): Unit

    @GET("/v1/search/popular-preview/illust")
    suspend fun popularPreviewIllusts(
        @Query("word") word: String,
        @Query("search_target") searchTarget: String = "partial_match_for_tags",
        @Query("filter") filter: String = "for_ios",
        @Query("include_translated_tag_results") includeTranslatedTagResults: Boolean = true,
        @Query("merge_plain_keyword_results") mergePlainKeywordResults: Boolean = true
    ): IllustResponse

    @GET("/v1/search/popular-preview/novel")
    suspend fun popularPreviewNovels(
        @Query("word") word: String,
        @Query("search_target") searchTarget: String = "partial_match_for_tags",
        @Query("filter") filter: String = "for_ios",
        @Query("include_translated_tag_results") includeTranslatedTagResults: Boolean = true,
        @Query("merge_plain_keyword_results") mergePlainKeywordResults: Boolean = true
    ): NovelResponse

    @GET("/v1/search/novel")
    suspend fun searchNovels(
        @Query("word") word: String,
        @Query("search_target") searchTarget: String = "partial_match_for_tags",
        @Query("sort") sort: String = "date_desc",
        @Query("filter") filter: String = "for_ios",
        @Query("include_translated_tag_results") includeTranslatedTagResults: Boolean = true,
        @Query("merge_plain_keyword_results") mergePlainKeywordResults: Boolean = true
    ): NovelResponse

    // ===== Novel Endpoints =====

    @GET("/v1/novel/recommended")
    suspend fun getRecommendedNovels(
        @Query("include_ranking_illusts") includeRanking: Boolean = false,
        @Query("filter") filter: String = "for_ios"
    ): NovelResponse

    @GET("/v1/novel/detail")
    suspend fun getNovelDetail(
        @Query("novel_id") novelId: Long
    ): SingleNovelResponse

    @GET("/v1/novel/follow")
    suspend fun getFollowingNovels(
        @Query("restrict") restrict: String = "public"
    ): NovelResponse

    @FormUrlEncoded
    @POST("/v2/novel/bookmark/add")
    suspend fun addNovelBookmark(
        @Field("novel_id") novelId: Long,
        @Field("restrict") restrict: String = "public"
    ): Unit

    @FormUrlEncoded
    @POST("/v1/novel/bookmark/delete")
    suspend fun deleteNovelBookmark(
        @Field("novel_id") novelId: Long
    ): Unit

    /**
     * 通用 GET 请求，用于加载 next_url 分页
     */
    @GET
    suspend fun getNextPageIllusts(@Url nextUrl: String): IllustResponse

    @GET
    suspend fun getNextPageHomeIllusts(@Url nextUrl: String): HomeIllustResponse

    @GET
    suspend fun getNextPageNovels(@Url nextUrl: String): NovelResponse
}