package com.kletaq.app.widgets.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import androidx.glance.unit.ColorProvider
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import com.kletaq.app.MainActivity
import com.kletaq.app.widgets.data.WidgetDataHelper
import com.kletaq.app.widgets.data.WidgetDataHelper.PandaMood

/**
 * 🐼 Panda Mini Widget (2×1)
 *
 * Strict Layout Specifications:
 * - Top Area: Streak header on line 1, message on line 2 (never overlaps panda area).
 * - Reserved Bottom Area: Fixed 42.dp high illustration area.
 * - Panda Image: Bottom-centered inside 42.dp area with width(68.dp).height(42.dp) and ContentScale.Fit.
 * - 5 Approved Visual States: Ready/Default, Welcome Back, Goal Complete, Revision Time, Gentle Reminder.
 */
class PandaMiniWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val streak = prefs[WidgetDataHelper.STREAK_KEY] ?: WidgetDataHelper.getStreak(context)
            val todayXp = prefs[WidgetDataHelper.TODAY_XP_KEY] ?: WidgetDataHelper.getTodayXp(context)
            val completedTasks = prefs[WidgetDataHelper.COMPLETED_TASKS_KEY] ?: WidgetDataHelper.getCompletedTasks(context)
            val subject = prefs[WidgetDataHelper.SUBJECT_KEY] ?: WidgetDataHelper.getCurrentSubject(context)
            val manualMoodId = prefs[WidgetDataHelper.SELECTED_MOOD_KEY] ?: WidgetDataHelper.getManualMoodId(context)

            val mood = WidgetDataHelper.getMood(
                context = context,
                todayXp = todayXp,
                completedTasks = completedTasks,
                streak = streak,
                currentSubject = subject
            )
            // A completed to-do is useful progress, but it is not evidence of a study session.
            // Keep the streak state tied to actual study XP only.
            val hasStudiedToday = todayXp > 0L

            val messageText = when (mood) {
                PandaMood.BACKLOG_SESSION_NEXT -> if (subject.isNotBlank() && subject != "No subject") "Next: $subject" else mood.text
                PandaMood.STREAK_ACTIVE, PandaMood.GOAL_COMPLETED -> if (streak > 0) "Streak secured today! 🎉" else "Great job today!"
                PandaMood.EVENING_DANGER -> if (streak > 0) "Save your streak! 🚨" else "Time to practice! ⚡"
                PandaMood.SUNSET_NUDGE -> if (streak > 0) "Don't let it break! 🔥" else "Time to practice! ⚡"
                PandaMood.NIGHT_LAST_CALL -> if (streak > 0) "Last chance! 🌙" else "Ready to learn? 🌙"
                PandaMood.MORNING_FRESH -> "Ready to learn? ⚡"
                PandaMood.AFTERNOON_FOCUS -> "Time to practice! 📚"
                else -> mood.text
            }

            // 🎨 Dynamic expressive panda illustrations directly from the active mood
            val pandaDrawableRes = mood.pandaDrawableResId

            val fireIconRes = if (hasStudiedToday) {
                com.kletaq.app.R.drawable.ic_widget_fire_lit
            } else {
                com.kletaq.app.R.drawable.ic_widget_fire_gray
            }

            PandaMiniContent(
                streakCount = streak,
                fireIconRes = fireIconRes,
                messageText = messageText,
                bgDrawableRes = mood.backgroundResId,
                pandaDrawableRes = pandaDrawableRes
            )
        }
    }
}

@Composable
private fun PandaMiniContent(
    streakCount: Int,
    fireIconRes: Int,
    messageText: String,
    bgDrawableRes: Int,
    pandaDrawableRes: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(bgDrawableRes))
            .clickable(actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.Top
    ) {
        // 📝 1. Top Text Area (Header line 1 + Message line 2, strictly separated from 42.dp bottom area)
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Top
        ) {
            // 🔥 Line 1: Dynamic Streak count with Lit/Alert/Gray Fire Icon (e.g. 🔥 3 / 🔥(!) 3 / 🩶 0)
            Row(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
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
                    text = "$streakCount",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White),
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            // 💬 Line 2: Short Context Message
            Text(
                text = messageText,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = ColorProvider(Color.White),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.defaultWeight())
        }

        // 🐼 2. Reserved Bottom Illustration Area (42.dp high, bottom-centered)
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(42.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Image(
                provider = ImageProvider(pandaDrawableRes),
                contentDescription = messageText,
                modifier = GlanceModifier
                    .width(68.dp)
                    .height(42.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

class PandaMiniWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PandaMiniWidget()
}
