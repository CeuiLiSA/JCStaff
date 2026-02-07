package ceui.lisa.jcstaff.network

import ceui.lisa.jcstaff.core.LanguageManager
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HeaderInterceptor(
    private val tokenProvider: (() -> String?)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestNonce = RequestNonce.build()
        val originalRequest = chain.request()

        val newRequest = originalRequest.newBuilder().apply {
            // Add auth token if available
            tokenProvider?.invoke()?.let { token ->
                addHeader("authorization", "Bearer $token")
            }

            val headers = LanguageManager.currentHeaders
            addHeader("accept", "*/*")
            addHeader("accept-language", headers.acceptLanguage)
            addHeader("app-accept-language", headers.appAcceptLanguage)
            addHeader("app-os", "ios")
            addHeader("app-os-version", "26.2")
            addHeader("app-version", "8.4.4")
            addHeader("x-client-time", requestNonce.xClientTime)
            addHeader("x-client-hash", requestNonce.xClientHash)
            addHeader("user-agent", "PixivIOSApp/8.4.4 (iOS 26.2; iPhone18,1)")
            addHeader("priority", "u=3")
        }.build()

        return chain.proceed(newRequest)
    }
}

data class RequestNonce(
    val xClientTime: String,
    val xClientHash: String,
) {
    companion object {
        private val format: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX", Locale.US)
        private const val HASH_SECRET = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

        fun build(): RequestNonce {
            val time = ZonedDateTime.now().format(format)
            val hash = md5("$time$HASH_SECRET")
            return RequestNonce(time, hash)
        }

        private fun md5(plainText: String): String {
            val md = MessageDigest.getInstance("MD5")
            md.update(plainText.toByteArray())
            val digest = md.digest()
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}