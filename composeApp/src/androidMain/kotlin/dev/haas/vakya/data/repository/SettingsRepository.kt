package dev.haas.vakya.data.repository

import dev.haas.vakya.data.database.AccountDao
import dev.haas.vakya.data.database.AccountEntity
import dev.haas.vakya.data.database.AppSettingDao
import dev.haas.vakya.data.database.AppSettingEntity
import android.util.Log
import dev.haas.vakya.data.database.AiActionLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val accountDao: AccountDao,
    private val appSettingDao: AppSettingDao,
    private val aiActionLogDao: dev.haas.vakya.data.database.AiActionLogDao,
    private val calendarApi: dev.haas.vakya.data.google.CalendarApi
) {
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)

    suspend fun removeAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    suspend fun getCalendars(accessToken: String): List<dev.haas.vakya.data.google.CalendarEntry> {
        Log.d("SettingsRepository", "getCalendars called with token: ${accessToken.take(15)}...")
        return try {
            val response = calendarApi.listCalendarList("Bearer $accessToken")
            val items = response.items ?: emptyList()
            Log.d("SettingsRepository", "Successfully fetched ${items.size} calendars")
            items
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("SettingsRepository", "CRITICAL: HTTP ${e.code()} - Failed to fetch calendars")
            Log.e("SettingsRepository", "Error Body: $errorBody")
            
            aiActionLogDao.insertLog(
                AiActionLogEntity(
                    logType = "ERROR",
                    subject = "Calendar 403 Forbidden",
                    actionSummary = "Access denied for calendar list. Possible insufficient scopes. Details: $errorBody"
                )
            )
            emptyList()
        } catch (e: Exception) {
            Log.e("SettingsRepository", "CRITICAL: Failed to fetch calendars", e)
            Log.e("SettingsRepository", "Error details: ${e.message}")
            aiActionLogDao.insertLog(
                AiActionLogEntity(
                    logType = "ERROR",
                    subject = "Calendar Fetch Error",
                    actionSummary = "Failed to fetch calendars: ${e.message}."
                )
            )
            emptyList()
        }
    }

    suspend fun createCalendar(accessToken: String, summary: String): dev.haas.vakya.data.google.CalendarEntry {
        return calendarApi.createCalendar(dev.haas.vakya.data.google.CalendarRequest(summary), "Bearer $accessToken")
    }

    fun getSetting(key: String): Flow<String?> = appSettingDao.getAllSettings().map { list ->
        list.find { it.key == key }?.value
    }

    suspend fun setSetting(key: String, value: String) {
        appSettingDao.setSetting(AppSettingEntity(key, value))
    }
    
    fun getAllSettings() = appSettingDao.getAllSettings()
}
