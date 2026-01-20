package ceui.lisa.jcstaff.auth

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ceui.lisa.jcstaff.network.AccountResponse
import ceui.lisa.jcstaff.network.PixivClient
import ceui.lisa.jcstaff.network.User
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class AuthRepository(private val context: Context) {

    companion object {
        private val ACCOUNT_JSON = stringPreferencesKey("account_json")
    }

    private val gson = Gson()

    val accountFlow: Flow<AccountResponse?> = context.dataStore.data.map { preferences ->
        preferences[ACCOUNT_JSON]?.let { json ->
            try {
                gson.fromJson(json, AccountResponse::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    val isLoggedIn: Flow<Boolean> = accountFlow.map { it?.access_token != null }

    val currentUser: Flow<User?> = accountFlow.map { it?.user }

    suspend fun getAccessToken(): String? {
        return accountFlow.first()?.access_token
    }

    suspend fun getRefreshToken(): String? {
        return accountFlow.first()?.refresh_token
    }

    suspend fun loginWithCode(uri: Uri): Result<AccountResponse> = withContext(Dispatchers.IO) {
        try {
            val code = uri.getQueryParameter("code")
                ?: return@withContext Result.failure(Exception("No code in callback URL"))

            val pkce = PixivClient.getPkce()

            val response = PixivClient.oAuthApi.login(
                clientId = PixivClient.CLIENT_ID,
                clientSecret = PixivClient.CLIENT_SECRET,
                grantType = PixivClient.GRANT_TYPE_AUTH_CODE,
                code = code,
                codeVerifier = pkce.verifier,
                redirectUri = PixivClient.CALLBACK_URL
            )

            saveAccount(response)
            PixivClient.resetPkce()
            PixivClient.setTokenProvider { response.access_token }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAccessToken(): Result<AccountResponse> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = getRefreshToken()
                ?: return@withContext Result.failure(Exception("No refresh token"))

            val response = PixivClient.oAuthApi.refreshToken(
                clientId = PixivClient.CLIENT_ID,
                clientSecret = PixivClient.CLIENT_SECRET,
                grantType = PixivClient.GRANT_TYPE_REFRESH_TOKEN,
                refreshToken = refreshToken
            )

            saveAccount(response)
            PixivClient.setTokenProvider { response.access_token }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveAccount(account: AccountResponse) {
        context.dataStore.edit { preferences ->
            preferences[ACCOUNT_JSON] = gson.toJson(account)
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCOUNT_JSON)
        }
        PixivClient.setTokenProvider { null }
        PixivClient.resetClient()
    }

    suspend fun initialize() {
        val token = getAccessToken()
        if (token != null) {
            PixivClient.setTokenProvider { token }
        }
    }
}