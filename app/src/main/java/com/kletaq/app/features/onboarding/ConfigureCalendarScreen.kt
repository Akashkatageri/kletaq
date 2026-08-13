package com.kletaq.app.features.onboarding

import androidx.compose.runtime.Composable

@Composable
fun ConfigureCalendarScreen(
    viewModel: OnboardingViewModel,
    onCalendarStepCompleted: () -> Unit
) {
    com.kletaq.app.features.settings.StudyCalendarScreen(
        onBackClick = onCalendarStepCompleted,
        isOnboarding = true,
        onCalendarCompleted = onCalendarStepCompleted
    )
}
