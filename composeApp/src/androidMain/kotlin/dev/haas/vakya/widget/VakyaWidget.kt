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
import androidx.glance.action.actionStartActivity
import dev.haas.vakya.MainActivity
import dev.haas.vakya.ui.theme.*
import androidx.glance.action.clickable


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
        val backgroundColor = ColorProvider(White)
        val onBackgroundColor = ColorProvider(Color.Black)
        val secondaryTextColor = ColorProvider(Color.Gray)
        val accentColor = ColorProvider(Crimson)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(8.dp)
                .appWidgetBackground()
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "Vakya Tasks",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        color = ColorProvider(Crimson)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                
                // Create Option (rounded add button)
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(ColorProvider(Crimson))
                        .cornerRadius(18)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ColorProvider(White)
                        )
                    )
                }
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                item {
                    SectionDivider("TODAY", accentColor)
                }
                
                if (today.isEmpty()) {
                    item {
                        EmptyItem("No tasks today", secondaryTextColor)
                    }
                } else {
                    items(today) { event ->
                        WidgetEventItem(event, onBackgroundColor, secondaryTextColor)
                    }
                }
                
                item {
                    SectionDivider("UPCOMING", accentColor)
                }
                
                if (upcoming.isEmpty()) {
                    item {
                        EmptyItem("No upcoming tasks", secondaryTextColor)
                    }
                } else {
                    items(upcoming) { event ->
                        WidgetEventItem(event, onBackgroundColor, secondaryTextColor)
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionDivider(title: String, color: ColorProvider) {
        Column {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
        }
    }

    @Composable
    private fun WidgetEventItem(event: CalendarEventEntity, primaryColor: ColorProvider, secondaryColor: ColorProvider) {
        val timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.startTime), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("hh:mm a"))
            
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = "•", style = TextStyle(color = ColorProvider(Violet)))
            Spacer(modifier = GlanceModifier.width(4.dp))
            Column {
                Text(
                    text = event.title,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = primaryColor),
                    maxLines = 1
                )
                Text(
                    text = timeStr,
                    style = TextStyle(fontSize = 10.sp, color = secondaryColor)
                )
            }
        }
    }

    @Composable
    private fun EmptyItem(msg: String, color: ColorProvider) {
        Text(
            text = msg,
            style = TextStyle(fontSize = 11.sp, color = color),
            modifier = GlanceModifier.padding(start = 8.dp)
        )
    }
}
