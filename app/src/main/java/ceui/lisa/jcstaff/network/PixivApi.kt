package ceui.lisa.jcstaff.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface PixivApi {

    @GET("/v1/{type}/recommended")
    suspend fun getRecommendedContent(
        @Path("type") type: String,
        @Query("include_ranking_illusts") includeRanking: Boolean = true,
        @Query("filter") filter: String = "for_ios"
    ): HomeIllustResponse

    @GET("/v1/illust/ranking")
    suspend fun getRankingIllusts(
        @Query("mode") mode: String = "day",
        @Query("date") date: String? = null,
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

    @GET("/v2/search/autocomplete")
    suspend fun searchAutocomplete(
        @Query("word") word: String,
        @Query("merge_plain_keyword_results") mergePlainKeywordResults: Boolean = true
    ): AutocompleteResponse

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

    @GET("/v1/search/user")
    suspend fun searchUsers(
        @Query("word") word: String,
        @Query("filter") filter: String = "for_ios",
        @Query("offset") offset: Int? = null
    ): UserPreviewResponse

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
        @Query("max_bookmark_id") maxBookmarkId: Long? = null,
        @Query("tag") tag: String? = null
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
    @POST("/v2/illust/bookmark/add")
    suspend fun addBookmarkWithTags(
        @Field("illust_id") illustId: Long,
        @Field("restrict") restrict: String,
        @Field("tags[]") tags: List<String>
    )

    @GET("/v1/user/bookmark-tags/illust")
    suspend fun getUserBookmarkTags(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String = "public"
    ): BookmarkTagsResponse

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

    // ===== User Content Endpoints =====

    @GET("/v1/user/novels")
    suspend fun getUserNovels(
        @Query("user_id") userId: Long,
        @Query("filter") filter: String = "for_ios"
    ): NovelResponse

    @GET("/v1/user/bookmarks/novel")
    suspend fun getUserBookmarkNovels(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String = "public",
        @Query("filter") filter: String = "for_ios"
    ): NovelResponse

    @GET("/v1/user/following")
    suspend fun getUserFollowing(
        @Query("user_id") userId: Long,
        @Query("restrict") restrict: String = "public",
        @Query("filter") filter: String = "for_ios"
    ): UserPreviewResponse

    // ===== Comment Endpoints =====

    @GET("/v3/illust/comments")
    suspend fun getIllustComments(
        @Query("illust_id") illustId: Long
    ): CommentResponse

    @GET("/v3/novel/comments")
    suspend fun getNovelComments(
        @Query("novel_id") novelId: Long
    ): CommentResponse

    @GET("/v2/{type}/comment/replies")
    suspend fun getCommentReplies(
        @Path("type") type: String,
        @Query("comment_id") commentId: Long
    ): CommentResponse

    @FormUrlEncoded
    @POST("/v1/illust/comment/add")
    suspend fun postIllustComment(
        @Field("illust_id") illustId: Long,
        @Field("comment") comment: String,
        @Field("parent_comment_id") parentCommentId: Long? = null
    ): PostCommentResponse

    @FormUrlEncoded
    @POST("/v1/novel/comment/add")
    suspend fun postNovelComment(
        @Field("novel_id") novelId: Long,
        @Field("comment") comment: String,
        @Field("parent_comment_id") parentCommentId: Long? = null
    ): PostCommentResponse

    @FormUrlEncoded
    @POST("/v1/{type}/comment/delete")
    suspend fun deleteComment(
        @Path("type") type: String,
        @Field("comment_id") commentId: Long
    ): Unit

    /**
     * 通用 GET 请求，用于加载 next_url 分页
     * 返回原始 ResponseBody，由调用方用 Gson 解析成具体类型
     */
    @GET
    suspend fun getNextPage(@Url nextUrl: String): okhttp3.ResponseBody

    // ===== Latest Content Endpoints =====

    @GET("/v1/illust/new")
    suspend fun getLatestIllusts(
        @Query("content_type") contentType: String = "illust",
        @Query("filter") filter: String = "for_ios"
    ): IllustResponse

    @GET("/v1/novel/new")
    suspend fun getLatestNovels(): NovelResponse

    // ===== Spotlight Endpoints =====

    @GET("/v1/spotlight/articles")
    suspend fun getSpotlightArticles(
        @Query("category") category: String = "all",
        @Query("filter") filter: String = "for_android"
    ): SpotlightResponse

    // ===== Ugoira Endpoints =====

    @GET("/v1/ugoira/metadata")
    suspend fun getUgoiraMetadata(
        @Query("illust_id") illustId: Long
    ): UgoiraResponse
}