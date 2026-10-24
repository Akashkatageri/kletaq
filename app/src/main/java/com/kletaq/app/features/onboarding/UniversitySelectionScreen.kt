package com.kletaq.app.features.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

enum class UniversityChoice {
    VTU,
    OTHER,
    SCHOOL
}

@Composable
fun UniversitySelectionScreen(
    viewModel: OnboardingViewModel,
    onUniversityCompleted: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val selectedUniversity by viewModel.selectedUniversity.collectAsState()
    val universityState by viewModel.universityState.collectAsState()
    val educationLevel by viewModel.educationLevel.collectAsState()

    var selectedChoice by remember {
        mutableStateOf(
            when {
                educationLevel == "SCHOOL" -> UniversityChoice.SCHOOL
                selectedUniversity.isNotBlank() && !selectedUniversity.equals("VTU", ignoreCase = true) -> UniversityChoice.OTHER
                else -> UniversityChoice.VTU
            }
        )
    }

    var customUniversityName by remember {
        mutableStateOf(
            if (selectedChoice == UniversityChoice.OTHER && selectedUniversity.isNotBlank()) selectedUniversity else ""
        )
    }

    LaunchedEffect(universityState) {
        if (universityState is StepUiState.Success) {
            viewModel.resetUniversityState()
            onUniversityCompleted()
        }
    }

    val isContinueEnabled = when (selectedChoice) {
        UniversityChoice.VTU -> true
        UniversityChoice.OTHER -> customUniversityName.trim().length >= 2
        UniversityChoice.SCHOOL -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Right Log Out / Switch Account Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        onSignOut()
                    }
            ) {
                Text(
                    text = "Switch Account",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(52.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (selectedChoice) {
                                UniversityChoice.SCHOOL -> "🎒"
                                UniversityChoice.OTHER -> "🎓"
                                UniversityChoice.VTU -> "🏛️"
                            },
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Select University or School",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Choose your institution or school board to configure your learning roadmap.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Option 1: VTU
            val isVtuSelected = selectedChoice == UniversityChoice.VTU
            val vtuBorderColor by animateColorAsState(
                targetValue = if (isVtuSelected) TextPrimary else BorderColor,
                label = "vtuBorder"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        selectedChoice = UniversityChoice.VTU
                        viewModel.selectEducationLevel("COLLEGE")
                        viewModel.selectUniversity("VTU")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(if (isVtuSelected) 2.dp else 1.dp, vtuBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🏛️", fontSize = 26.sp)
                            Column {
                                Text(
                                    text = "VTU",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Visvesvaraya Technological University",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (isVtuSelected) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "✓",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.background
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "⚡", fontSize = 12.sp)
                            Text(
                                text = "Preloaded 2025/2022 NEP Syllabus & Question Bank",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Other University / Autonomous College
            val isOtherSelected = selectedChoice == UniversityChoice.OTHER
            val otherBorderColor by animateColorAsState(
                targetValue = if (isOtherSelected) TextPrimary else BorderColor,
                label = "otherBorder"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        selectedChoice = UniversityChoice.OTHER
                        viewModel.selectEducationLevel("COLLEGE")
                        if (customUniversityName.isNotBlank()) {
                            viewModel.selectUniversity(customUniversityName.trim())
                        } else {
                            viewModel.selectUniversity("")
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(if (isOtherSelected) 2.dp else 1.dp, otherBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🎓", fontSize = 26.sp)
                            Column {
                                Text(
                                    text = "Other University",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Autonomous College or Other Board",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (isOtherSelected) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "✓",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.background
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "✨", fontSize = 12.sp)
                            Text(
                                text = "Custom AI Roadmap Importer Support",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isOtherSelected,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "ENTER YOUR UNIVERSITY OR COLLEGE NAME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customUniversityName,
                                onValueChange = { input ->
                                    customUniversityName = input
                                    viewModel.selectUniversity(input.trim())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "e.g., Pune University, PES, RVCE, DTU...",
                                        fontSize = 13.sp,
                                        color = TextSecondary.copy(alpha = 0.6f)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TextPrimary,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                trailingIcon = {
                                    if (customUniversityName.isNotBlank()) {
                                        IconButton(onClick = {
                                            customUniversityName = ""
                                            viewModel.selectUniversity("")
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Kletaq will personalize your semester subjects using the AI Syllabus Importer.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 3: School / Pre-University (PUC)
            val isSchoolChoice = selectedChoice == UniversityChoice.SCHOOL
            val schoolBorderColor by animateColorAsState(
                targetValue = if (isSchoolChoice) TextPrimary else BorderColor,
                label = "schoolBorder"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        selectedChoice = UniversityChoice.SCHOOL
                        viewModel.selectEducationLevel("SCHOOL")
                        viewModel.selectUniversity("School")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(if (isSchoolChoice) 2.dp else 1.dp, schoolBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🎒", fontSize = 26.sp)
                            Column {
                                Text(
                                    text = "School / Pre-University",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Class 1–10 & 11–12th / 1st & 2nd PUC",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (isSchoolChoice) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "✓",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.background
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "📚", fontSize = 12.sp)
                            Text(
                                text = "CBSE, ICSE & State Boards • Custom Roadmap",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Button
        Button(
            onClick = {
                when (selectedChoice) {
                    UniversityChoice.VTU -> {
                        viewModel.selectEducationLevel("COLLEGE")
                        viewModel.selectUniversity("VTU")
                        viewModel.submitUniversity()
                    }
                    UniversityChoice.OTHER -> {
                        viewModel.selectEducationLevel("COLLEGE")
                        viewModel.selectUniversity(customUniversityName.trim())
                        viewModel.submitUniversity()
                    }
                    UniversityChoice.SCHOOL -> {
                        viewModel.selectEducationLevel("SCHOOL")
                        viewModel.selectUniversity("School")
                        viewModel.submitUniversity()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TextPrimary,
                contentColor = MaterialTheme.colorScheme.background
            ),
            enabled = isContinueEnabled && universityState !is StepUiState.Loading,
            elevation = null
        ) {
            if (universityState is StepUiState.Loading) {
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
