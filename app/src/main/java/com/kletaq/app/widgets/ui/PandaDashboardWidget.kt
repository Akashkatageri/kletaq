package com.kletaq.app.widgets.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.ContentScale
import androidx.datastore.preferences.core.Preferences
import com.kletaq.app.MainActivity
import com.kletaq.app.widgets.data.WidgetDataHelper
import com.kletaq.app.widgets.data.WidgetDataHelper.PandaMood

/**
 * 🐼 Panda Dashboard Widget (2×2)
 * Full dashboard: streak, XP, tasks done, current subject.
 * Dynamically changes background gradient for Morning, Afternoon, Evening.
 * Button removed as per user design specification.
 */
class PandaDashboardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val streak = prefs[WidgetDataHelper.STREAK_KEY] ?: WidgetDataHelper.getStreak(context)
            val todayXp = prefs[WidgetDataHelper.TODAY_XP_KEY] ?: WidgetDataHelper.getTodayXp(context)
            val completedTasks = prefs[WidgetDataHelper.COMPLETED_TASKS_KEY] ?: WidgetDataHelper.getCompletedTasks(context)
            val subject = prefs[WidgetDataHelper.SUBJECT_KEY] ?: WidgetDataHelper.getCurrentSubject(context)

            val mood = WidgetDataHelper.getMood(
                context = context,
                todayXp = todayXp,
                completedTasks = completedTasks,
                streak = streak,
                currentSubject = subject
            )
            val hasStudiedToday = todayXp > 0L

            val fireIconRes = if (hasStudiedToday) {
                com.kletaq.app.R.drawable.ic_widget_fire_lit
            } else {
                com.kletaq.app.R.drawable.ic_widget_fire_gray
            }

            PandaDashboardContent(
                streak = streak,
                todayXp = todayXp,
                completedTasks = completedTasks,
                subject = subject,
                mood = mood,
                fireIconRes = fireIconRes
            )
        }
    }
}

@Composable
private fun PandaDashboardContent(
    streak: Int,
    todayXp: Long,
    completedTasks: Int,
    subject: String,
    mood: PandaMood,
    fireIconRes: Int
) {
    val bgImageProvider = ImageProvider(mood.backgroundResId)
    val whiteTextProvider = ColorProvider(androidx.compose.ui.graphics.Color.White)
    val mutedTextProvider = ColorProvider(androidx.compose.ui.graphics.Color(0xFFE2E8F0))
    val accentYellowProvider = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFDE047))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgImageProvider)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header (Dynamic Expressive Panda Character & Title)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(mood.pandaDrawableResId),
                contentDescription = "Panda Character",
                modifier = GlanceModifier
                    .width(46.dp)
                    .height(36.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Kletaq",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = whiteTextProvider
                    )
                )
                Text(
                    text = mood.text,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = mutedTextProvider
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        // Row 1: Streak & Today's XP
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(fireIconRes),
                        contentDescription = "Streak Fire",
                        modifier = GlanceModifier
                            .width(13.dp)
                            .height(13.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    Text(
                        text = "Streak",
                        style = TextStyle(fontSize = 10.sp, color = mutedTextProvider, textAlign = TextAlign.Center)
                    )
                }
                Text(
                    text = "$streak",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = whiteTextProvider,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⭐ Today",
                    style = TextStyle(fontSize = 10.sp, color = mutedTextProvider, textAlign = TextAlign.Center)
                )
                Text(
                    text = "${todayXp} XP",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentYellowProvider,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Row 2: Tasks Done & Current Subject
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✅ Tasks",
                    style = TextStyle(fontSize = 10.sp, color = mutedTextProvider, textAlign = TextAlign.Center)
                )
                Text(
                    text = "$completedTasks",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = whiteTextProvider,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📖 Subject",
                    style = TextStyle(fontSize = 10.sp, color = mutedTextProvider, textAlign = TextAlign.Center)
                )
                Text(
                    text = if (subject.isNotBlank() && subject != "No subject") subject else "Study Time",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = whiteTextProvider,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

class PandaDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PandaDashboardWidget()
}
