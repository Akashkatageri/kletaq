package com.kletaq.app.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onStartLearningClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Main Graphic & Heading
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(88.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🎉", fontSize = 40.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Welcome to Kletaq",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your learning journey is ready.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Start Learning Button
            Button(
                onClick = onStartLearningClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                elevation = null
            ) {
                Text(
                    text = "Start Learning",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
