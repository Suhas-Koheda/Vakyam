package dev.haas.vakya.data.repository

import dev.haas.vakya.data.database.AccountDao
import dev.haas.vakya.data.database.AccountEntity
import dev.haas.vakya.data.database.AppSettingDao
import dev.haas.vakya.data.database.AppSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val accountDao: AccountDao,
    private val appSettingDao: AppSettingDao,
    private val calendarApi: dev.haas.vakya.data.google.CalendarApi
) {
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)

    suspend fun removeAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    suspend fun getCalendars(accessToken: String): List<dev.haas.vakya.data.google.CalendarEntry> {
        return try {
            calendarApi.listCalendarList("Bearer $accessToken").items ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSetting(key: String): Flow<String?> = appSettingDao.getAllSettings().map { list ->
        list.find { it.key == key }?.value
    }

    suspend fun setSetting(key: String, value: String) {
        appSettingDao.setSetting(AppSettingEntity(key, value))
    }
    
    fun getAllSettings() = appSettingDao.getAllSettings()
}
