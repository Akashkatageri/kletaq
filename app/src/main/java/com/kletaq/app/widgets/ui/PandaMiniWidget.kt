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

            val mood = WidgetDataHelper.getMood(
                context = context,
                todayXp = todayXp,
                completedTasks = completedTasks,
                streak = streak,
                currentSubject = subject
            )
            val hasStudiedToday = todayXp > 0L || completedTasks > 0

            val messageText = when (mood) {
                PandaMood.BACKLOG_SESSION_NEXT -> if (subject.isNotBlank() && subject != "No subject") "Next: $subject" else mood.text
                PandaMood.STREAK_ACTIVE -> if (streak > 0) "Keep the streak alive!" else "Great job today!"
                else -> mood.text
            }

            // 🎨 Map to 5 approved visual states (hide full-body/poorly-clipping assets until replaced)
            val pandaDrawableRes = when (mood) {
                PandaMood.NO_STUDY_TODAY, PandaMood.NO_CURRENT_PLAN -> com.kletaq.app.R.drawable.panda_ready
                PandaMood.RETURNED_AFTER_DAYS -> com.kletaq.app.R.drawable.panda_welcome
                PandaMood.GOAL_COMPLETED, PandaMood.STREAK_ACTIVE -> com.kletaq.app.R.drawable.panda_complete
                PandaMood.EXAM_REVISION, PandaMood.BACKLOG_SESSION_NEXT -> com.kletaq.app.R.drawable.panda_revision
                PandaMood.EVENING_NOT_STUDIED -> com.kletaq.app.R.drawable.panda_sleepy
                else -> com.kletaq.app.R.drawable.panda_ready
            }

            PandaMiniContent(
                streakCount = streak,
                hasStudiedToday = hasStudiedToday,
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
    hasStudiedToday: Boolean,
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
            // 🔥 Line 1: Dynamic Streak count with Lit/Gray Fire Icon (e.g. 🔥 3 / 🩶 3)
            Row(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(
                        if (hasStudiedToday) com.kletaq.app.R.drawable.ic_widget_fire_lit
                        else com.kletaq.app.R.drawable.ic_widget_fire_gray
                    ),
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
