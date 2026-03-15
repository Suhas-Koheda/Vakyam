package dev.haas.vakya

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.haas.vakya.data.database.AccountEntity
import com.google.android.gms.auth.GoogleAuthUtil
import android.util.Log
import dev.haas.vakya.data.database.AiActionLogEntity


class GoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    private val _userState = MutableStateFlow<UserData?>(null)
    val userState = _userState.asStateFlow()

    init {
        loadUserState()
    }

    companion object {
        const val WEB_CLIENT_ID = "1037052378949-787c48qhctetmtlcr9g41ppmqc93g87i.apps.googleusercontent.com"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PROFILE_PIC = "profile_pic"
    }

    private fun loadUserState() {
        val userId = prefs.getString(KEY_USER_ID, null)
        if (userId != null) {
            _userState.value = UserData(
                userId = userId,
                email = prefs.getString(KEY_EMAIL, "") ?: "",
                displayName = prefs.getString(KEY_DISPLAY_NAME, null),
                profilePictureUrl = prefs.getString(KEY_PROFILE_PIC, null)
            )
        }
    }

    private fun saveUserState(userData: UserData) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userData.userId)
            putString(KEY_EMAIL, userData.email)
            putString(KEY_DISPLAY_NAME, userData.displayName)
            putString(KEY_PROFILE_PIC, userData.profilePictureUrl)
            apply()
        }
    }

    private fun clearUserState() {
        prefs.edit().clear().apply()
    }

    suspend fun signIn() {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()
        
        // Note: For real Gmail/Calendar access, you would also need to request 
        // access tokens with scopes like:
        // "https://www.googleapis.com/auth/gmail.readonly"
        // "https://www.googleapis.com/auth/calendar.events"
        // Credential Manager primarily provides ID tokens. 
        // For broad API access, you might need a separate Auth flow or 
        // exchange the code for tokens on a backend.


        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            e.printStackTrace()
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val email = googleIdTokenCredential.id // Note: For Credential Manager, 'id' is often the email
            
            val userData = UserData(
                userId = googleIdTokenCredential.id,
                email = email,
                displayName = googleIdTokenCredential.displayName,
                profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString()
            )
            _userState.value = userData
            saveUserState(userData)

            // Attempt to get a real access token for APIs
            val accessToken = withContext(Dispatchers.IO) {
                try {
                    val scopes = "oauth2:https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/calendar.readonly https://www.googleapis.com/auth/calendar.events"
                    val token = GoogleAuthUtil.getToken(context, email, scopes)
                    Log.d("GoogleAuthManager", "Token retrieved successfully: ${token.take(10)}...")
                    token
                } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                    Log.e("GoogleAuthManager", "User consent required (NEED_REMOTE_CONSENT) for $email", e)
                    
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        Log.d("GoogleAuthManager", "Launching recovery intent for $email")
                        activity.startActivity(e.intent)
                    }
                    null
                } catch (e: Exception) {
                    val message = e.message ?: "Unknown error"
                    println("DEBUG_AUTH_ERROR: Failed to get token for $email: $message")
                    Log.e("GoogleAuthManager", "Failed to get token for $email: $message", e)
                    
                    if (message.contains("RECOVERABLE", ignoreCase = true)) {
                        Log.d("GoogleAuthManager", "Recoverable error detected via message for $email")
                    }

                    val db = AppContextHolder.database
                    db.aiActionLogDao().insertLog(
                        AiActionLogEntity(
                            logType = "ERROR",
                            subject = "Auth Token Error",
                            actionSummary = "Failed to get token for $email: $message"
                        )
                    )
                    null
                }
            }

            // Save to Room using the singleton database
            withContext(Dispatchers.IO) {
                val db = AppContextHolder.database
                db.accountDao().insertAccount(
                    AccountEntity(
                        email = email,
                        displayName = userData.displayName,
                        isGmailEnabled = true,
                        targetCalendarId = "primary",
                        targetAccountEmail = email,
                        accessToken = if (accessToken?.startsWith("ya29") == true) accessToken else null
                    )
                )
                db.aiActionLogDao().insertLog(
                    AiActionLogEntity(
                        logType = "AUTH",
                        subject = "Account Added",
                        actionSummary = "Added $email. Access Token retrieved: ${accessToken != null}"
                    )
                )
                Log.d("GoogleAuthManager", "Account added: $email, Token success: ${accessToken != null}")
            }
        }
    }


    fun signOut() {
        _userState.value = null
        clearUserState()
    }
}

data class UserData(
    val userId: String,
    val email: String,
    val displayName: String?,
    val profilePictureUrl: String?
)
