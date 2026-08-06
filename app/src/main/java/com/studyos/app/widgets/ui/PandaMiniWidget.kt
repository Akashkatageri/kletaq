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
import androidx.glance.text.TextStyle
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import com.studyos.app.MainActivity
import com.studyos.app.widgets.data.WidgetDataHelper

/**
 * 🐼 Panda Mini Widget (2×1)
 * Displays ONLY streak count, panda avatar, and daily motivation.
 * Dynamically changes background gradient for Morning, Afternoon, Evening.
 * XP metric removed as per user design specification.
 */
class PandaMiniWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val streak = WidgetDataHelper.getStreak(context)
        val message = WidgetDataHelper.getMotivationalMessage()
        val theme = WidgetDataHelper.getCurrentWidgetTheme()

        provideContent {
            PandaMiniContent(streak, message, theme)
        }
    }
}

@Composable
private fun PandaMiniContent(
    streak: Int,
    message: String,
    theme: WidgetDataHelper.WidgetTheme
) {
    val bgImageProvider = ImageProvider(theme.backgroundDrawableResId)
    val primaryTextProvider = ColorProvider(theme.primaryTextColor)
    val secondaryTextProvider = ColorProvider(theme.secondaryTextColor)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgImageProvider)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🐼",
                style = TextStyle(fontSize = 26.sp)
            )

            Spacer(modifier = GlanceModifier.width(10.dp))

            Column {
                Text(
                    text = "🔥 $streak ${if (streak == 1) "day streak" else "days streak"}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextProvider
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = secondaryTextProvider
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

class PandaMiniWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PandaMiniWidget()
}
