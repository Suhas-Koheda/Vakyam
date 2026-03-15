package dev.haas.vakya.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import dev.haas.vakya.AppContextHolder
import dev.haas.vakya.data.database.CalendarEventEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class VakyaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppContextHolder.database
        val now = System.currentTimeMillis()
        val endOfDay = LocalDateTime.now().withHour(23).withMinute(59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val todayEvents = db.calendarEventDao().getTodayEventsList(now, endOfDay)
        val upcomingEvents = db.calendarEventDao().getUpcomingEventsList(endOfDay)
            .take(5) // Just show next 5

        provideContent {
            VakyaWidgetContent(todayEvents, upcomingEvents)
        }
    }

    @Composable
    private fun VakyaWidgetContent(today: List<CalendarEventEntity>, upcoming: List<CalendarEventEntity>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(8.dp)
                .appWidgetBackground()
        ) {
            Text(
                text = "Vakya Tasks",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorProvider(Color.Black))
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                item {
                    SectionDivider("TODAY")
                }
                
                if (today.isEmpty()) {
                    item {
                        EmptyItem("No tasks today")
                    }
                } else {
                    items(today) { event ->
                        WidgetEventItem(event)
                    }
                }
                
                item {
                    SectionDivider("UPCOMING")
                }
                
                if (upcoming.isEmpty()) {
                    item {
                        EmptyItem("No upcoming tasks")
                    }
                } else {
                    items(upcoming) { event ->
                        WidgetEventItem(event)
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionDivider(title: String) {
        Column {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ColorProvider(Color.DarkGray))
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
    }

    @Composable
    private fun WidgetEventItem(event: CalendarEventEntity) {
        val timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.startTime), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("hh:mm a"))
            
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = "•", style = TextStyle(color = ColorProvider(Color.Blue)))
            Spacer(modifier = GlanceModifier.width(4.dp))
            Column {
                Text(
                    text = event.title,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ColorProvider(Color.Black))
                )
                Text(
                    text = timeStr,
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray))
                )
            }
        }
    }

    @Composable
    private fun EmptyItem(msg: String) {
        Text(
            text = msg,
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray)),
            modifier = GlanceModifier.padding(start = 8.dp)
        )
    }
}
