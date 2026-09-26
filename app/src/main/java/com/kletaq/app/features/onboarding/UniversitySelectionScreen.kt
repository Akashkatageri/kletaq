package com.kletaq.app.features.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kletaq.app.core.theme.BorderColor
import com.kletaq.app.core.theme.TextPrimary
import com.kletaq.app.core.theme.TextSecondary

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip

data class UniversityOption(
    val code: String,
    val fullName: String,
    val category: String, // "IITs", "NITs", "Premier", "State Tech", "Private"
    val badge: String = category
)

@Composable
fun UniversitySelectionScreen(
    viewModel: OnboardingViewModel,
    onUniversityCompleted: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val selectedUniversity by viewModel.selectedUniversity.collectAsState()
    val universityState by viewModel.universityState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf("All") }

    val categories = listOf("All", "IITs", "NITs", "Premier", "State Tech", "Private")

    val allUniversities = remember {
        listOf(
            // State Technical Universities
            UniversityOption("VTU", "Visvesvaraya Technological University, Belagavi", "State Tech", "State Tech"),
            UniversityOption("AKTU", "Dr. A.P.J. Abdul Kalam Technical University, UP", "State Tech", "State Tech"),
            UniversityOption("Anna University", "Anna University, Chennai, Tamil Nadu", "State Tech", "State Tech"),
            UniversityOption("MAKAUT", "Maulana Abul Kalam Azad University of Tech, WB", "State Tech", "State Tech"),
            UniversityOption("JNTU Hyderabad", "Jawaharlal Nehru Technological University, Hyd", "State Tech", "State Tech"),
            UniversityOption("JNTU Kakinada", "JNTU Kakinada, Andhra Pradesh", "State Tech", "State Tech"),
            UniversityOption("SPPU Pune", "Savitribai Phule Pune University, Maharashtra", "State Tech", "State Tech"),
            UniversityOption("GTU", "Gujarat Technological University, Ahmedabad", "State Tech", "State Tech"),
            UniversityOption("KTU", "APJ Abdul Kalam Technological University, Kerala", "State Tech", "State Tech"),

            // IITs (Indian Institutes of Technology)
            UniversityOption("IIT Bombay", "Indian Institute of Technology Bombay", "IITs", "IIT"),
            UniversityOption("IIT Delhi", "Indian Institute of Technology Delhi", "IITs", "IIT"),
            UniversityOption("IIT Madras", "Indian Institute of Technology Madras", "IITs", "IIT"),
            UniversityOption("IIT Kanpur", "Indian Institute of Technology Kanpur", "IITs", "IIT"),
            UniversityOption("IIT Kharagpur", "Indian Institute of Technology Kharagpur", "IITs", "IIT"),
            UniversityOption("IIT Roorkee", "Indian Institute of Technology Roorkee", "IITs", "IIT"),
            UniversityOption("IIT Guwahati", "Indian Institute of Technology Guwahati", "IITs", "IIT"),
            UniversityOption("IIT Hyderabad", "Indian Institute of Technology Hyderabad", "IITs", "IIT"),
            UniversityOption("IIT (BHU) Varanasi", "Indian Institute of Technology (BHU) Varanasi", "IITs", "IIT"),
            UniversityOption("IIT Indore", "Indian Institute of Technology Indore", "IITs", "IIT"),

            // NITs (National Institutes of Technology)
            UniversityOption("NITK Surathkal", "National Institute of Technology Karnataka", "NITs", "NIT"),
            UniversityOption("NIT Trichy", "National Institute of Technology Tiruchirappalli", "NITs", "NIT"),
            UniversityOption("NIT Warangal", "National Institute of Technology Warangal", "NITs", "NIT"),
            UniversityOption("VNIT Nagpur", "Visvesvaraya National Institute of Tech, Nagpur", "NITs", "NIT"),
            UniversityOption("NIT Rourkela", "National Institute of Technology Rourkela", "NITs", "NIT"),
            UniversityOption("NIT Calicut", "National Institute of Technology Calicut", "NITs", "NIT"),
            UniversityOption("MNIT Jaipur", "Malaviya National Institute of Technology Jaipur", "NITs", "NIT"),
            UniversityOption("MNNIT Allahabad", "Motilal Nehru National Institute of Technology", "NITs", "NIT"),
            UniversityOption("SVNIT Surat", "Sardar Vallabhbhai National Institute of Tech", "NITs", "NIT"),
            UniversityOption("NIT Silchar", "National Institute of Technology Silchar", "NITs", "NIT"),

            // Premier Technical Institutes & IIITs
            UniversityOption("IIIT Hyderabad", "International Institute of Information Tech, Hyd", "Premier", "IIIT"),
            UniversityOption("IIIT Bangalore", "International Institute of Information Tech, Blr", "Premier", "IIIT"),
            UniversityOption("IIIT Delhi", "Indraprastha Institute of Information Tech Delhi", "Premier", "IIIT"),
            UniversityOption("BITS Pilani", "Birla Institute of Technology and Science, Pilani", "Premier", "Premier"),
            UniversityOption("DTU", "Delhi Technological University, New Delhi", "Premier", "Premier"),
            UniversityOption("NSUT Delhi", "Netaji Subhas University of Technology, Delhi", "Premier", "Premier"),
            UniversityOption("COEP Pune", "COEP Technological University, Pune", "Premier", "Premier"),
            UniversityOption("VJTI Mumbai", "Veermata Jijabai Technological Institute, Mumbai", "Premier", "Premier"),

            // Deemed & Private Universities
            UniversityOption("MAHE (Manipal)", "Manipal Academy of Higher Education", "Private", "Deemed"),
            UniversityOption("VIT Vellore", "Vellore Institute of Technology, Vellore", "Private", "Private"),
            UniversityOption("SRM IST", "SRM Institute of Science and Technology, Chennai", "Private", "Private"),
            UniversityOption("Thapar University", "Thapar Institute of Engineering and Technology", "Private", "Deemed"),
            UniversityOption("Amrita University", "Amrita Vishwa Vidyapeetham, Coimbatore", "Private", "Deemed")
        )
    }

    val filteredUniversities = remember(searchQuery, selectedCategoryTab, allUniversities) {
        allUniversities.filter { uni ->
            val matchesCategory = selectedCategoryTab == "All" || uni.category.equals(selectedCategoryTab, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    uni.code.contains(searchQuery, ignoreCase = true) ||
                    uni.fullName.contains(searchQuery, ignoreCase = true) ||
                    uni.badge.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    LaunchedEffect(universityState) {
        if (universityState is StepUiState.Success) {
            viewModel.resetUniversityState()
            onUniversityCompleted()
        }
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

        Spacer(modifier = Modifier.height(4.dp))

        // Header Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(46.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🏛️", fontSize = 22.sp)
                }
            }

            Text(
                text = "Select your university",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Choose your affiliation or institute to tailor your curriculum.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search university, IIT, NIT, state...", fontSize = 12.sp, color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isTabSelected = selectedCategoryTab == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isTabSelected) TextPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isTabSelected) TextPrimary else BorderColor),
                    modifier = Modifier
                        .clickable { selectedCategoryTab = cat }
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTabSelected) MaterialTheme.colorScheme.background else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredUniversities.size} universities available",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            if (searchQuery.isNotBlank() || selectedCategoryTab != "All") {
                Text(
                    text = "Reset",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier
                        .clickable {
                            searchQuery = ""
                            selectedCategoryTab = "All"
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Universities List
        if (filteredUniversities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🔍", fontSize = 28.sp)
                    Text(
                        text = "No universities found matching \"$searchQuery\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedCategoryTab = "All"
                        }
                    ) {
                        Text(text = "Clear search", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredUniversities, key = { it.code }) { uni ->
                    val isSelected = selectedUniversity == uni.code

                    val cardBorderColor by animateColorAsState(
                        targetValue = if (isSelected) TextPrimary else BorderColor,
                        label = "uniBorder"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectUniversity(uni.code) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = uni.code,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = uni.badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = uni.fullName,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Button
        Button(
            onClick = { viewModel.submitUniversity() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TextPrimary,
                contentColor = MaterialTheme.colorScheme.background
            ),
            enabled = selectedUniversity.isNotBlank() && universityState !is StepUiState.Loading,
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

