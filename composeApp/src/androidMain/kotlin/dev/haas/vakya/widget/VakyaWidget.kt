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
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf


import dev.haas.vakya.R

import dev.haas.vakya.data.database.KnowledgeNoteEntity

class VakyaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppContextHolder.database
        val now = System.currentTimeMillis()
        val endOfDay = LocalDateTime.now().withHour(23).withMinute(59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val todayEvents = db.calendarEventDao().getTodayEventsList(now, endOfDay)
        val upcomingEvents = db.calendarEventDao().getUpcomingEventsList(endOfDay)
            .take(5)
            
        val recentNotes = db.knowledgeNoteDao().getRecentNotesList(3)

        provideContent {
            VakyaWidgetContent(todayEvents, upcomingEvents, recentNotes)
        }
    }

    @Composable
    private fun VakyaWidgetContent(
        today: List<CalendarEventEntity>, 
        upcoming: List<CalendarEventEntity>,
        recentNotes: List<KnowledgeNoteEntity>
    ) {
        val backgroundColor = ColorProvider(R.color.widget_bg)
        val onBackgroundColor = ColorProvider(R.color.widget_on_bg)
        val secondaryTextColor = ColorProvider(R.color.widget_secondary_text)
        val accentColor = ColorProvider(R.color.widget_accent)

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
                        color = accentColor
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                
                // Create Option (rounded add button)
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(accentColor)
                        .cornerRadius(18.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ColorProvider(White)
                        ),
                        modifier = GlanceModifier.padding(bottom = 2.dp)
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

                item {
                    SectionDivider("KNOWLEDGE", accentColor)
                }

                if (recentNotes.isEmpty()) {
                    item {
                        EmptyItem("No notes yet", secondaryTextColor)
                    }
                } else {
                    items(recentNotes) { note ->
                        WidgetNoteItem(note, onBackgroundColor, secondaryTextColor)
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetNoteItem(note: KnowledgeNoteEntity, primaryColor: ColorProvider, secondaryColor: ColorProvider) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val noteColor = ColorProvider(R.color.widget_accent)
            // Larger Clickable Square for Checkbox functionality
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(actionRunCallback<ToggleNoteAction>(actionParametersOf(ToggleNoteAction.noteIdKey to note.id))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "□", 
                    style = TextStyle(color = noteColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                )
            }
            
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                Text(
                    text = note.title,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryColor),
                    maxLines = 1
                )
                if (note.summary != null) {
                    Text(
                        text = note.summary,
                        style = TextStyle(fontSize = 11.sp, color = secondaryColor),
                        maxLines = 1
                    )
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
            
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bulletColor = ColorProvider(R.color.widget_bullet)
            
            // Larger Clickable Bullet (behaves like a checkbox to dismiss/complete)
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(actionRunCallback<ToggleEventAction>(actionParametersOf(ToggleEventAction.eventIdKey to event.id))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "○", 
                    style = TextStyle(color = bulletColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                Text(
                    text = event.title,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryColor),
                    maxLines = 1
                )
                Text(
                    text = timeStr,
                    style = TextStyle(fontSize = 11.sp, color = secondaryColor)
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

class ToggleEventAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = parameters[ToggleEventAction.eventIdKey] ?: return
        AppContextHolder.database.calendarEventDao().markAsIgnored(eventId)
        VakyaWidget().update(context, glanceId)
    }
    companion object {
        val eventIdKey = ActionParameters.Key<Long>("eventId")
    }
}

class ToggleNoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val noteId = parameters[ToggleNoteAction.noteIdKey] ?: return
        // For now, checking off a note from the widget will archive it (delete from recent view)
        // In a real app we might update a 'isCompleted' flag.
        val note = AppContextHolder.database.knowledgeNoteDao().getNoteById(noteId)
        if (note != null) {
            AppContextHolder.database.knowledgeNoteDao().deleteNote(note)
        }
        VakyaWidget().update(context, glanceId)
    }
    companion object {
        val noteIdKey = ActionParameters.Key<Long>("noteId")
    }
}
