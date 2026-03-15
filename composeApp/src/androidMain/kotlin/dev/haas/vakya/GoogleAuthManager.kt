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

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val userData = UserData(
                userId = googleIdTokenCredential.id,
                email = googleIdTokenCredential.id, // Usually id is the email
                displayName = googleIdTokenCredential.displayName,
                profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString()
            )
            _userState.value = userData
            saveUserState(userData)

            // Save to Room for Agent
            kotlinx.coroutines.MainScope().launch {
                val db = androidx.room.Room.databaseBuilder(
                    context,
                    dev.haas.vakya.data.database.VakyaDatabase::class.java, "vakya-db"
                ).build()
                db.accountDao().insertAccount(
                    dev.haas.vakya.data.database.AccountEntity(
                        email = userData.email,
                        displayName = userData.displayName,
                        isGmailEnabled = true,
                        targetCalendarId = "primary",
                        accessToken = googleIdTokenCredential.idToken // Using ID token as placeholder or if it's actually an access token (usually Credential Manager gives ID token, but we might need to swap for access token later)
                    )
                )
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
