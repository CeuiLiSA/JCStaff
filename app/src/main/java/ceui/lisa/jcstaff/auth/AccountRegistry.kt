package ceui.lisa.jcstaff.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.Serializable

data class AccountEntry(
    val userId: Long,
    val userName: String,
    val userAccount: String,
    val avatarUrl: String?,
    val addedAt: Long = System.currentTimeMillis()
) : Serializable

private val Context.accountRegistryDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "account_registry")

object AccountRegistry {

    private val ACCOUNTS_JSON = stringPreferencesKey("accounts_json")
    private val ACTIVE_USER_ID = longPreferencesKey("active_user_id")
    private val MIGRATION_DONE = stringPreferencesKey("migration_done")

    private val gson = Gson()
    private var dataStore: DataStore<Preferences>? = null

    fun initialize(context: Context) {
        dataStore = context.accountRegistryDataStore
    }

    val allAccounts: Flow<List<AccountEntry>>
        get() = dataStore!!.data.map { prefs ->
            val json = prefs[ACCOUNTS_JSON]
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<AccountEntry>>() {}.type
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    val activeUserId: Flow<Long?>
        get() = dataStore!!.data.map { prefs ->
            prefs[ACTIVE_USER_ID]
        }

    suspend fun getAll(): List<AccountEntry> = allAccounts.first()

    suspend fun getActiveUserId(): Long? = activeUserId.first()

    suspend fun addAccount(entry: AccountEntry) {
        dataStore!!.edit { prefs ->
            val current = getAccountsFromPrefs(prefs).toMutableList()
            current.removeAll { it.userId == entry.userId }
            current.add(entry)
            prefs[ACCOUNTS_JSON] = gson.toJson(current)
        }
    }

    suspend fun updateAccount(entry: AccountEntry) {
        dataStore!!.edit { prefs ->
            val current = getAccountsFromPrefs(prefs).toMutableList()
            val index = current.indexOfFirst { it.userId == entry.userId }
            if (index >= 0) {
                current[index] = entry
                prefs[ACCOUNTS_JSON] = gson.toJson(current)
            }
        }
    }

    suspend fun setActiveAccount(userId: Long) {
        dataStore!!.edit { prefs ->
            prefs[ACTIVE_USER_ID] = userId
        }
    }

    suspend fun removeAccount(userId: Long) {
        dataStore!!.edit { prefs ->
            val current = getAccountsFromPrefs(prefs).toMutableList()
            current.removeAll { it.userId == userId }
            prefs[ACCOUNTS_JSON] = gson.toJson(current)
            if (prefs[ACTIVE_USER_ID] == userId) {
                val next = current.firstOrNull()
                if (next != null) {
                    prefs[ACTIVE_USER_ID] = next.userId
                } else {
                    prefs.remove(ACTIVE_USER_ID)
                }
            }
        }
    }

    suspend fun isMigrationDone(): Boolean {
        return dataStore!!.data.first()[MIGRATION_DONE] != null
    }

    suspend fun setMigrationDone() {
        dataStore!!.edit { prefs ->
            prefs[MIGRATION_DONE] = "true"
        }
    }

    private fun getAccountsFromPrefs(prefs: Preferences): List<AccountEntry> {
        val json = prefs[ACCOUNTS_JSON]
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<AccountEntry>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
