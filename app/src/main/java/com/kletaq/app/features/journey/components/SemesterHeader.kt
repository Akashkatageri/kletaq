package com.kletaq.app.features.journey.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.PrimaryAccentColor
import com.kletaq.app.core.theme.SuccessGreen
import com.kletaq.app.core.theme.TextSecondary
import com.kletaq.app.features.home.components.ProgressBar

import androidx.compose.ui.text.style.TextOverflow

@Composable
fun SemesterHeader(
    semesters: List<SemesterJourney>,
    selectedSemesterId: String,
    onSemesterSelect: (String) -> Unit,
    userBranch: String = "CSE",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedSemester = semesters.find { it.id == selectedSemesterId }
        ?: semesters.lastOrNull { !it.isLocked }
        ?: semesters.first()

    val completedSubjects = selectedSemester.completedSubjectsCount
    val totalSubjects = selectedSemester.subjectCount
    val isAllCleared = completedSubjects >= totalSubjects && totalSubjects > 0

    val cleanBranch = userBranch.trim()
    val branchLabel = when {
        cleanBranch.isBlank() -> "CSE"
        cleanBranch.equals("Computer Science & Engineering", ignoreCase = true) || cleanBranch.equals("Computer Science", ignoreCase = true) -> "CSE"
        cleanBranch.equals("Information Science & Engineering", ignoreCase = true) || cleanBranch.equals("Information Science", ignoreCase = true) -> "ISE"
        cleanBranch.equals("Electronics & Communication", ignoreCase = true) || cleanBranch.equals("Electronics & Communication Engineering", ignoreCase = true) -> "ECE"
        cleanBranch.equals("Electrical & Electronics", ignoreCase = true) || cleanBranch.equals("Electrical & Electronics Engineering", ignoreCase = true) -> "EEE"
        cleanBranch.equals("Mechanical Engineering", ignoreCase = true) || cleanBranch.equals("Mechanical", ignoreCase = true) -> "ME"
        cleanBranch.equals("Civil Engineering", ignoreCase = true) || cleanBranch.equals("Civil", ignoreCase = true) -> "CIV"
        cleanBranch.equals("Artificial Intelligence & Machine Learning", ignoreCase = true) || cleanBranch.equals("AI & ML", ignoreCase = true) -> "AIML"
        cleanBranch.length <= 6 -> cleanBranch.uppercase()
        else -> cleanBranch.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    }

    val headerTitle = "Semester ${selectedSemester.semesterNumber} · $branchLabel"

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Title & Subtitle
        Text(
            text = headerTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        val subtitleText = when {
            selectedSemester.isLocked -> "🔒 Locked • Complete prior semesters to unlock"
            isAllCleared -> "$completedSubjects/$totalSubjects subjects completed ✓"
            else -> "$completedSubjects/$totalSubjects subjects completed"
        }

        Text(
            text = subtitleText,
            fontSize = 12.sp,
            fontWeight = if (isAllCleared) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isAllCleared) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Clean Horizontal Progress Bar
        ProgressBar(
            progress = if (selectedSemester.isLocked) 0f else selectedSemester.progress,
            height = 6.dp,
            progressColor = if (isAllCleared) SuccessGreen else PrimaryAccentColor,
            backgroundColor = (if (isAllCleared) SuccessGreen else PrimaryAccentColor).copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Compact Semester Pill Chips: [S1 ✓] [S2] [S3 🔒]
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(semesters) { semester ->
                val isSelected = semester.id == selectedSemesterId
                val isLocked = semester.isLocked
                val semCleared = semester.completedSubjectsCount >= semester.subjectCount && semester.subjectCount > 0

                val chipLabel = when {
                    isLocked -> "S${semester.semesterNumber} 🔒"
                    semCleared -> "S${semester.semesterNumber} ✓"
                    else -> "S${semester.semesterNumber}"
                }

                Surface(
                    shape = CircleShape,
                    color = when {
                        isSelected -> PrimaryAccentColor
                        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
                        semCleared -> SuccessGreen.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                    },
                    contentColor = when {
                        isSelected -> Color.White
                        isLocked -> TextSecondary.copy(alpha = 0.5f)
                        semCleared -> SuccessGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    border = when {
                        isSelected -> null
                        semCleared -> BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                        else -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    },
                    modifier = Modifier.clickable {
                        if (!isLocked) {
                            onSemesterSelect(semester.id)
                        } else {
                            Toast.makeText(
                                context,
                                "Complete Semester ${semester.semesterNumber - 1} to unlock Semester ${semester.semesterNumber}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(
                        text = chipLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || semCleared) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
