package dev.haas.vakya.data.auth

import android.content.Context
import dev.haas.vakya.data.database.AccountDao
import dev.haas.vakya.data.database.AccountEntity
import dev.haas.vakya.data.database.VakyaDatabase
import kotlinx.coroutines.flow.Flow

class GoogleAccountManager(private val accountDao: AccountDao) {
    
    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun addAccount(email: String, displayName: String?, accessToken: String?) {
        val account = AccountEntity(
            email = email,
            displayName = displayName,
            isGmailEnabled = true,
            targetCalendarId = "primary",
            targetAccountEmail = email,
            accessToken = accessToken
        )
        accountDao.insertAccount(account)
    }

    suspend fun updateAccountSettings(email: String, isGmailEnabled: Boolean, calendarId: String) {
        val accounts = accountDao.getAllAccountsList()
        val account = accounts.find { it.email == email } ?: return
        accountDao.insertAccount(account.copy(
            isGmailEnabled = isGmailEnabled,
            targetCalendarId = calendarId
        ))
    }

    suspend fun removeAccount(email: String) {
        val accounts = accountDao.getAllAccountsList()
        val account = accounts.find { it.email == email } ?: return
        accountDao.deleteAccount(account)
    }
}
