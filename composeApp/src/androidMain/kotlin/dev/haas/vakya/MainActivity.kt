package dev.haas.vakya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import androidx.work.*
import dev.haas.vakya.workers.EmailSyncWorker
import dev.haas.vakya.workers.SyncScheduler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        MainScope().launch {
            val db = AppContextHolder.database
            val intervalPref = db.appSettingDao().getAllSettings().first()
                .find { it.key == dev.haas.vakya.ui.viewmodel.SettingsViewModel.KEY_SCAN_INTERVAL }
                ?.value ?: "30 minutes"
            
            val intervalMinutes = when {
                intervalPref.contains("15") -> 15L
                intervalPref.contains("30") -> 30L
                intervalPref.contains("4") -> 240L
                intervalPref.contains("1") -> 60L
                else -> 30L
            }
            
            SyncScheduler.scheduleSync(androidx.work.WorkManager.getInstance(this@MainActivity), intervalMinutes)
        }
        
        dev.haas.vakya.widget.VakyaWidgetWorker.enqueue(this)

        setContent {
            App()
        }
    }

}


@Preview
@Composable
fun AppAndroidPreview() {
    App()
}