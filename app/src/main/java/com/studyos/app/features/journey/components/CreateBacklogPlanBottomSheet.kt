package com.studyos.app.features.journey.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyos.app.core.theme.CardSurface
import com.studyos.app.core.theme.InkPaperBorder
import com.studyos.app.core.theme.PurpleAccent
import com.studyos.app.core.theme.TextPrimary
import com.studyos.app.core.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateBacklogPlanBottomSheet(
    subject: SubjectJourney,
    onDismiss: () -> Unit,
    onSavePlan: (studyDaysPerWeek: Int, sessionMinutes: Int, selectedUnitIds: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var studyDays by remember { mutableIntStateOf(3) } // 3 or 5
    var sessionMins by remember { mutableIntStateOf(30) } // 30, 45, 60
    var isWeakUnitsMode by remember { mutableStateOf(false) }

    val allUnitIds = remember(subject) { subject.units.map { it.id } }
    val selectedUnits = remember(subject) { mutableStateListOf<String>().apply { addAll(allUnitIds) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PurpleAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "BACKLOG PLAN",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // 1. Days per week
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Study Days Per Week",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(3 to "3 Days / week", 5 to "5 Days / week").forEach { (days, label) ->
                        val isSelected = studyDays == days
                        OptionPill(
                            label = label,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = { studyDays = days }
                        )
                    }
                }
            }

            // 2. Session minutes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Session Duration",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(30 to "30 Mins", 45 to "45 Mins", 60 to "60 Mins").forEach { (mins, label) ->
                        val isSelected = sessionMins == mins
                        OptionPill(
                            label = label,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = { sessionMins = mins }
                        )
                    }
                }
            }

            // 3. Starting Unit / Scope
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Plan Scope",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OptionPill(
                        label = "Study full syllabus",
                        isSelected = !isWeakUnitsMode,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isWeakUnitsMode = false
                            selectedUnits.clear()
                            selectedUnits.addAll(allUnitIds)
                        }
                    )
                    OptionPill(
                        label = "Select weak units",
                        isSelected = isWeakUnitsMode,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isWeakUnitsMode = true
                        }
                    )
                }

                if (isWeakUnitsMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subject.units.forEach { unit ->
                            val isUnitSelected = selectedUnits.contains(unit.id)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isUnitSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isUnitSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PurpleAccent) else null,
                                modifier = Modifier.clickable {
                                    if (isUnitSelected) {
                                        if (selectedUnits.size > 1) {
                                            selectedUnits.remove(unit.id)
                                        }
                                    } else {
                                        selectedUnits.add(unit.id)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isUnitSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PurpleAccent,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "Unit ${unit.unitNumber}: ${unit.title.take(16)}...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isUnitSelected) PurpleAccent else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val finalUnits = if (!isWeakUnitsMode) allUnitIds else selectedUnits.toList()
                    onSavePlan(studyDays, sessionMins, finalUnits)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleAccent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Activate Backlog Plan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PurpleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PurpleAccent else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PurpleAccent else TextSecondary
        )
    }
}
