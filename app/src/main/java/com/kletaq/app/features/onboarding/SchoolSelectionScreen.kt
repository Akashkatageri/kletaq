package com.kletaq.app.features.onboarding

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

data class SchoolBoardOption(
    val code: String,
    val title: String,
    val description: String,
    val emoji: String
)

data class SchoolStreamOption(
    val id: String,
    val title: String,
    val subjects: String,
    val emoji: String
)

@Composable
fun SchoolSelectionScreen(
    viewModel: OnboardingViewModel,
    onSchoolCompleted: () -> Unit,
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    BackHandler {
        viewModel.resetStepStates()
        onBackClick()
    }

    val selectedBoard by viewModel.selectedSchoolBoard.collectAsState()
    val selectedGrade by viewModel.selectedSchoolGrade.collectAsState()
    val selectedStream by viewModel.selectedSchoolStream.collectAsState()
    val schoolState by viewModel.schoolState.collectAsState()

    LaunchedEffect(schoolState) {
        if (schoolState is StepUiState.Success) {
            viewModel.resetSchoolState()
            onSchoolCompleted()
        }
    }

    val boards = listOf(
        SchoolBoardOption("CBSE", "CBSE", "Central Board of Secondary Education", "🏛️"),
        SchoolBoardOption("ICSE", "ICSE / ISC", "Council for Indian School Certificate", "📘"),
        SchoolBoardOption("Karnataka State Board", "Karnataka Board", "KSEAB / SSLC & PUC", "🟡"),
        SchoolBoardOption("Other State / International", "Other Board", "State Boards, Cambridge, IB", "🌐")
    )

    val streams = listOf(
        SchoolStreamOption("Science (PCMB)", "Science (PCMB)", "Physics, Chemistry, Maths, Biology", "🔬"),
        SchoolStreamOption("Science (PCMC)", "Science (PCMC)", "Physics, Chemistry, Maths, Computer Science", "💻"),
        SchoolStreamOption("Commerce", "Commerce", "Accountancy, Business Studies, Economics, Maths", "📊"),
        SchoolStreamOption("Arts / Humanities", "Arts / Humanities", "History, Political Science, Sociology, Psychology", "🎨")
    )

    val isSeniorClass = selectedGrade.contains("11") || selectedGrade.contains("12") || selectedGrade.contains("PUC")

    val isContinueEnabled = if (isSeniorClass) {
        selectedBoard.isNotBlank() && selectedGrade.isNotBlank() && selectedStream.isNotBlank()
    } else {
        selectedBoard.isNotBlank() && selectedGrade.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Navigation Bar (Back & Switch Account)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.resetStepStates()
                        onBackClick()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "←",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Back",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

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

        Spacer(modifier = Modifier.height(10.dp))

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
                        Text(text = "🎒", fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "School & Pre-University",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Configure your school board and grade to personalize your syllabus & roadmap.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: School Board Selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. SELECT EDUCATION BOARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )

                boards.forEach { board ->
                    val isSelected = selectedBoard.equals(board.code, ignoreCase = true)
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) TextPrimary else BorderColor,
                        label = "boardBorder_${board.code}"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.selectSchoolBoard(board.code)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = board.emoji, fontSize = 22.sp)
                                Column {
                                    Text(
                                        text = board.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = board.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✓",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.background
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: Class / Grade Selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "2. SELECT CLASS / GRADE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )

                // Sub-heading: Primary & High School (Classes 1-10)
                Text(
                    text = "Classes 1 to 10 (School)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                // 2 Rows of 5 numeric chips for Class 1 to 10
                val primaryClassesRow1 = listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5")
                val primaryClassesRow2 = listOf("Class 6", "Class 7", "Class 8", "Class 9", "Class 10")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    primaryClassesRow1.forEach { grade ->
                        val isSelected = selectedGrade.equals(grade, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TextPrimary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) TextPrimary else BorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.selectSchoolGrade(grade)
                                    viewModel.selectSchoolStream("")
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = grade.replace("Class ", "Cl "),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.background else TextPrimary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    primaryClassesRow2.forEach { grade ->
                        val isSelected = selectedGrade.equals(grade, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TextPrimary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) TextPrimary else BorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.selectSchoolGrade(grade)
                                    viewModel.selectSchoolStream("")
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = grade.replace("Class ", "Cl "),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.background else TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sub-heading: Pre-University & Senior Secondary
                Text(
                    text = "Pre-University & Senior Secondary (11th & 12th)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val seniorClasses = listOf(
                    "Class 11 / 1st PUC",
                    "Class 12 / 2nd PUC"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    seniorClasses.forEach { grade ->
                        val isSelected = selectedGrade.equals(grade, ignoreCase = true)
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) TextPrimary else BorderColor,
                            label = "seniorGradeBorder_$grade"
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectSchoolGrade(grade)
                                    if (selectedStream.isBlank()) {
                                        viewModel.selectSchoolStream("Science (PCMB)")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = grade,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Stream required",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "✓",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Stream Selection (Animated visibility for Class 11, 12, PUC)
            AnimatedVisibility(
                visible = isSeniorClass,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. SELECT ACADEMIC STREAM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )

                    streams.forEach { stream ->
                        val isSelected = selectedStream.equals(stream.id, ignoreCase = true)
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) TextPrimary else BorderColor,
                            label = "streamBorder_${stream.id}"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.selectSchoolStream(stream.id)
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = stream.emoji, fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = stream.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = stream.subjects,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "✓",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.background
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (schoolState is StepUiState.Error) {
                Text(
                    text = (schoolState as StepUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    viewModel.submitSchoolProfile()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                enabled = isContinueEnabled && schoolState !is StepUiState.Loading,
                elevation = null
            ) {
                if (schoolState is StepUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.background,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue to Profile",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
