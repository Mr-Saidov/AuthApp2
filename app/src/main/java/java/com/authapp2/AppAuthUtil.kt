package java.com.authapp2

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.auth0.android.jwt.JWT
import net.openid.appauth.*
import org.greenrobot.eventbus.EventBus


class AppAuthUtil(private val context: Context) {

    private val TAG = "##AppAuthUtil"
    val AUTH_REQUEST = 112
    private val USED_INTENT = "USED_INTENT"
    private lateinit var authorizationService: AuthorizationService

    val SELECTED_ACCOUNT = "selected_account"
    val PREFS_FILENAME = "PREFS"

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true)
            .setUserAuthenticationRequired(true)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun encryptPrefsString(value: String) {
        encryptedPrefs.edit {
            putString(SELECTED_ACCOUNT, value)
            apply()
        }
    }

    fun decryptPrefsString(): String? = encryptedPrefs.getString(SELECTED_ACCOUNT, null)

    fun checkAccounts(): MutableList<Account> {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType(context.getString(R.string.account_type))
        val result: MutableList<Account> = arrayListOf()
        accounts.forEach { result.add(it) }
        return result
    }

    fun getAuthIntent(): Intent {
        val serviceConfiguration =
            AuthorizationServiceConfiguration(getAuthorizeApi(), getTokenApi())
        val builder = AuthorizationRequest.Builder(
            serviceConfiguration,
            context.getString(R.string.client_id),
            ResponseTypeValues.CODE,
            getRedirectUri()
        )
        builder.setScopes(context.getString(R.string.scopes))
            .setPrompt("login")
        val request = builder.build()
        val authorizationService = AuthorizationService(context)
        return authorizationService.getAuthorizationRequestIntent(request)
    }

    fun checkIntent(intent: Intent?) {
        if (intent != null) {
            if (!intent.hasExtra(USED_INTENT)) {
                handleAuthorizationResponse(intent)
                intent.putExtra(USED_INTENT, true)
            }
        }
    }

    private fun handleAuthorizationResponse(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        val authState = AuthState(response, error)
        if (response != null) {
            authorizationService = AuthorizationService(context)
            authorizationService.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { tokenResponse, exception ->
                if (exception != null) {
                    Log.w(TAG, "Token Exchange failed", exception)
                } else {
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, exception)
                        Log.e(TAG, tokenResponse.accessToken!!)
                        createAccount(authState)
                    }
                }
            }
        }
    }

    private fun createAccount(authState: AuthState) {
        val bundle = Bundle()
        bundle.putString("authState", authState.jsonSerializeString())
        val jwt = JWT(authState.accessToken!!)
        val accountManager = AccountManager.get(context)
        val account = Account(jwt.subject, context.getString(R.string.account_type))
        val result = accountManager.addAccountExplicitly(account, "password", null)
        accountManager.setAuthToken(
            account,
            AccountManager.KEY_AUTH_TOKEN_LABEL,
            authState.jsonSerializeString()
        )
        EventBus.getDefault().post(if (result) "${jwt.subject} created" else "fail")
    }

    fun authorizeWithSavedAccount(account: Account) {
        authorizationService = AuthorizationService(context)
        try {
            Log.e(TAG, "${account.name} clicked")
            val accountManager = AccountManager.get(context)
            accountManager.getAuthToken(
                account,
                AccountManager.KEY_AUTH_TOKEN_LABEL,
                false,
                OnTokenAcquired(authorizationService),
                null
            )
        } catch (e: Exception) {

        }
    }


    private class OnTokenAcquired(private val authorizationService: AuthorizationService) :
        AccountManagerCallback<Bundle> {
        private val TAG = "##OnTokenAcquired"
        override fun run(future: AccountManagerFuture<Bundle>?) {
            val bundle = future?.result
            val token = bundle?.getString(AccountManager.KEY_AUTHTOKEN)
            val authState = AuthState.jsonDeserialize(token!!)
            authState.performActionWithFreshTokens(authorizationService)
            { accessToken, idToken, ex ->
                run {
                    if (ex == null) {
                        Log.e(TAG, accessToken!!)
                        Log.e(TAG, idToken!!)
                    } else {
                        Log.e(TAG, ex.errorDescription!!)
                    }
                }
            }
        }
    }

    private fun getAuthorizeApi(): Uri {
        val str =
            "${context.getString(R.string.base_iam_api)}${context.getString(R.string.authorize_api)}"
        return Uri.parse(str)
    }

    private fun getTokenApi(): Uri {
        val str =
            "${context.getString(R.string.base_iam_api)}${context.getString(R.string.token_api)}"
        return Uri.parse(str)
    }

    private fun getUserInfoApi(): Uri {
        val str =
            "${context.getString(R.string.base_iam_api)}${context.getString(R.string.user_info_api)}"
        return Uri.parse(str)
    }

    private fun getRedirectUri(): Uri {
        val str =
            "${context.getString(R.string.account_type)}${context.getString(R.string.callback_api)}"
        return Uri.parse(str)
    }


    companion object {

    }
}