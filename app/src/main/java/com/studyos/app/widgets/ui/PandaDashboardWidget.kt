package com.studyos.app.widgets.ui

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
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import com.studyos.app.MainActivity
import com.studyos.app.widgets.data.WidgetDataHelper

/**
 * 🐼 Panda Dashboard Widget (2×2)
 * Full dashboard: streak, XP, tasks done, current subject.
 * Dynamically changes background gradient for Morning, Afternoon, Evening.
 * Button removed as per user design specification.
 */
class PandaDashboardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val streak = WidgetDataHelper.getStreak(context)
        val todayXp = WidgetDataHelper.getTodayXp(context)
        val completedTasks = WidgetDataHelper.getCompletedTasks(context)
        val subject = WidgetDataHelper.getCurrentSubject(context)
        val message = WidgetDataHelper.getMotivationalMessage()
        val theme = WidgetDataHelper.getCurrentWidgetTheme()

        provideContent {
            PandaDashboardContent(streak, todayXp, completedTasks, subject, message, theme)
        }
    }
}

@Composable
private fun PandaDashboardContent(
    streak: Int,
    todayXp: Long,
    completedTasks: Int,
    subject: String,
    message: String,
    theme: WidgetDataHelper.WidgetTheme
) {
    val bgImageProvider = ImageProvider(theme.backgroundDrawableResId)
    val primaryTextProvider = ColorProvider(theme.primaryTextColor)
    val secondaryTextProvider = ColorProvider(theme.secondaryTextColor)
    val accentProvider = ColorProvider(theme.accentColor)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgImageProvider)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header (Panda Icon, Klytaq Branding & Greeting)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🐼", style = TextStyle(fontSize = 24.sp))
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column {
                Text(
                    text = "Kletaq",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextProvider
                    )
                )
                Text(
                    text = message,
                    style = TextStyle(fontSize = 10.sp, color = secondaryTextProvider),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Row 1: Streak & Today's XP
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔥 Streak",
                    style = TextStyle(fontSize = 10.sp, color = secondaryTextProvider, textAlign = TextAlign.Center)
                )
                Text(
                    text = "$streak",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextProvider,
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
                    style = TextStyle(fontSize = 10.sp, color = secondaryTextProvider, textAlign = TextAlign.Center)
                )
                Text(
                    text = "${todayXp} XP",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentProvider,
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
                    style = TextStyle(fontSize = 10.sp, color = secondaryTextProvider, textAlign = TextAlign.Center)
                )
                Text(
                    text = "$completedTasks",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextProvider,
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
                    style = TextStyle(fontSize = 10.sp, color = secondaryTextProvider, textAlign = TextAlign.Center)
                )
                Text(
                    text = subject,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextProvider,
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
