package com.kletaq.app.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

data class BranchOption(
    val code: String,
    val fullName: String,
    val iconEmoji: String
)

@Composable
fun BranchSelectionScreen(
    viewModel: OnboardingViewModel,
    onBranchCompleted: () -> Unit,
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    BackHandler { onBackClick() }

    val selectedBranch by viewModel.selectedBranch.collectAsState()
    val branchState by viewModel.branchState.collectAsState()

    val branches = listOf(
        BranchOption("CSE", "Computer Science and Engineering (CSE)", "💻"),
        BranchOption("ISE", "Information Science and Engineering (ISE)", "🌐"),
        BranchOption("AIML", "Artificial Intelligence and Machine Learning (AIML)", "🤖"),
        BranchOption("IoT", "CSE (Internet of Things, Cyber Security & Blockchain)", "📡"),
        BranchOption("ECE", "Electronics and Communication Engineering (ECE)", "⚡"),
        BranchOption("EEE", "Electrical and Electronics Engineering (EEE)", "🔌"),
        BranchOption("Mechanical", "Mechanical Engineering", "⚙️")
    )

    LaunchedEffect(branchState) {
        if (branchState is StepUiState.Success) {
            onBranchCompleted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Top Navigation Bar (Back & Switch Account)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Back",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(8.dp)
            )

            Text(
                text = "Switch Account",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier
                    .clickable {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        onSignOut()
                    }
                    .padding(8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(52.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🎓", fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Select your branch",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "We will structure your syllabus and subject roadmap accordingly.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                items(branches, key = { it.code }) { branch ->
                    val isSelected = selectedBranch == branch.code

                    val cardBorderColor by animateColorAsState(
                        targetValue = if (isSelected) TextPrimary else BorderColor,
                        label = "cardBorder"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clickable { viewModel.selectBranch(branch.code) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = branch.iconEmoji, fontSize = 20.sp)
                                if (isSelected) {
                                    Text(text = "✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }

                            Column {
                                Text(
                                    text = branch.code,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = branch.fullName,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Action Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (branchState is StepUiState.Error) {
                    Text(
                        text = (branchState as StepUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = { viewModel.submitBranch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    enabled = selectedBranch.isNotBlank() && branchState !is StepUiState.Loading,
                    elevation = null
                ) {
                    if (branchState is StepUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
