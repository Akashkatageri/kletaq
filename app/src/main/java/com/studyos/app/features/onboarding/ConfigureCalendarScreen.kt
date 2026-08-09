package com.studyos.app.features.onboarding

import androidx.compose.runtime.Composable

@Composable
fun ConfigureCalendarScreen(
    viewModel: OnboardingViewModel,
    onCalendarStepCompleted: () -> Unit
) {
    com.studyos.app.features.settings.StudyCalendarScreen(
        onBackClick = onCalendarStepCompleted,
        isOnboarding = true,
        onCalendarCompleted = onCalendarStepCompleted
    )
}
