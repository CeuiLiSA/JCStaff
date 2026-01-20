package ceui.lisa.jcstaff.network

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object PixivClient {

    const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    const val GRANT_TYPE_AUTH_CODE = "authorization_code"
    const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    const val CALLBACK_URL = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"

    private const val APP_API_HOST = "https://app-api.pixiv.net"
    private const val OAUTH_HOST = "https://oauth.secure.pixiv.net"
    private const val REQUEST_TIMEOUT = 15L

    private var tokenProvider: (() -> String?)? = null
    private var _pixivApi: PixivApi? = null
    private var currentPkce: PKCEItem? = null

    val oAuthApi: OAuthApi by lazy {
        createOAuthClient().create(OAuthApi::class.java)
    }

    val pixivApi: PixivApi
        get() {
            return _pixivApi ?: createAppClient().create(PixivApi::class.java).also {
                _pixivApi = it
            }
        }

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
        _pixivApi = null
    }

    fun resetClient() {
        _pixivApi = null
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
            .addInterceptor(HeaderInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(OAUTH_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private fun createAppClient(): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor(HeaderInterceptor(tokenProvider))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(APP_API_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }
}