package com.kletaq.app.core.navigation

import android.util.Log

import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.kletaq.app.core.theme.CardSurface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.kletaq.app.features.focus.TimerScreen
import com.kletaq.app.features.friends.FriendsScreen
import com.kletaq.app.features.home.HomeScreen
import com.kletaq.app.features.journey.JourneyScreen
import com.kletaq.app.features.profile.ProfileScreen

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    val navItemsLeft = listOf(
        NavItem(Screen.Home.route, "Home", Icons.Default.Home),
        NavItem(Screen.Journey.route, "Journey", Icons.AutoMirrored.Filled.MenuBook)
    )

    val navItemsRight = listOf(
        NavItem(Screen.Friends.route, "Friends", Icons.Default.People),
        NavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
    )

    var showBottomSheet by remember { mutableStateOf(false) }
    var activeFocusTopicId by remember { mutableStateOf<String?>(null) }
    var activeFocusTopic by remember { mutableStateOf<String?>(null) }
    var activeFocusSubject by remember { mutableStateOf<String?>(null) }
    var activeFocusSemester by remember { mutableStateOf<String?>(null) }
    var targetJourneySemesterId by remember { mutableStateOf<String?>(null) }
    var targetJourneySubjectId by remember { mutableStateOf<String?>(null) }
    var targetJourneyUnitId by remember { mutableStateOf<String?>(null) }
    var targetJourneyTopicId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.Journey.route,
        Screen.Friends.route,
        Screen.Profile.route,
        Screen.Tasks.route,
        Screen.Focus.route
    )

    fun navigateToRoute(targetRoute: String) {
        Log.d(
            "NAV_DEBUG",
            "Navigating to $targetRoute. Current route before = ${navController.currentDestination?.route}"
        )
        navController.navigate(targetRoute) {
            popUpTo(Screen.Journey.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        Log.d(
            "NAV_DEBUG",
            "Navigated to $targetRoute. Current route after = ${navController.currentDestination?.route}"
        )
    }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    NavigationBar(
                        containerColor = CardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.shadow(elevation = 2.dp, shape = RectangleShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItemsLeft.forEach { item ->
                                val selected = currentRoute == item.route
                                CustomNavItem(
                                    item = item,
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navigateToRoute(item.route)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            navItemsRight.forEach { item ->
                                val selected = currentRoute == item.route
                                CustomNavItem(
                                    item = item,
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navigateToRoute(item.route)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-8).dp)
                            .size(64.dp)
                            .zIndex(1f)
                            .clickable { showBottomSheet = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Focus / Create",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route
            ) {
                composable(Screen.Splash.route) {
                    val splashViewModel: com.kletaq.app.features.splash.SplashViewModel = hiltViewModel()
                    com.kletaq.app.features.splash.SplashScreen(
                        viewModel = splashViewModel,
                        onNavigateToDestination = { dest ->
                            val targetRoute = when (dest) {
                                com.kletaq.app.features.splash.SplashDestination.GOOGLE_SIGN_IN -> Screen.GoogleSignIn.route
                                com.kletaq.app.features.splash.SplashDestination.LEGAL -> Screen.Legal.route
                                com.kletaq.app.features.splash.SplashDestination.UNIVERSITY -> Screen.UniversityOnboarding.route
                                com.kletaq.app.features.splash.SplashDestination.BRANCH -> Screen.BranchOnboarding.route
                                com.kletaq.app.features.splash.SplashDestination.SCHEME -> Screen.SchemeOnboarding.route
                                com.kletaq.app.features.splash.SplashDestination.SEMESTER -> Screen.SemesterOnboarding.route
                                com.kletaq.app.features.splash.SplashDestination.CYCLE -> Screen.CycleOnboarding.route
                                com.kletaq.app.features.splash.SplashDestination.BACKLOG -> Screen.BacklogOnboarding.route
                                com.kletaq.app.features.splash.SplashDestination.USERNAME -> Screen.UsernameOnboarding.route
                                com.kletaq.app.features.splash.SplashDestination.HOME -> Screen.Home.route
                                else -> Screen.GoogleSignIn.route
                            }
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.GoogleSignIn.route) {
                    val authViewModel: com.kletaq.app.features.auth.AuthViewModel = hiltViewModel()
                    val scope = rememberCoroutineScope()

                    com.kletaq.app.features.auth.GoogleSignInScreen(
                        viewModel = authViewModel,
                        onAuthSuccess = { user ->
                            scope.launch {
                                val uid = user.uid
                                android.util.Log.d("AuthFlow", "UID = $uid")

                                val profileResult = authViewModel.userRepository.getUserProfile(uid)
                                val profile = profileResult.getOrNull()

                                val docExists = profile != null
                                android.util.Log.d("AuthFlow", "Document exists = $docExists")
                                android.util.Log.d("AuthFlow", "Profile = $profile")
                                android.util.Log.d("AuthFlow", "hasCompletedOnboarding = ${profile?.hasCompletedOnboarding}")

                                if (!docExists) {
                                    authViewModel.userRepository.saveInitialUser(
                                        uid = uid,
                                        email = user.email ?: "",
                                        displayName = user.displayName ?: "",
                                        photoUrl = user.photoUrl?.toString() ?: ""
                                    )
                                }

                                if (profile != null && profile.hasCompletedOnboarding) {
                                    android.util.Log.d("AuthFlow", "Destination = Home")
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.GoogleSignIn.route) { inclusive = true }
                                    }
                                } else {
                                    android.util.Log.d("AuthFlow", "Destination = Onboarding")
                                    navController.navigate(Screen.Legal.route) {
                                        popUpTo(Screen.GoogleSignIn.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Legal.route) {
                    val legalViewModel: com.kletaq.app.features.legal.LegalViewModel = hiltViewModel()
                    com.kletaq.app.features.legal.LegalScreen(
                        viewModel = legalViewModel,
                        onViewDocument = { docType ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docType))
                        },
                        onLegalAccepted = {
                            navController.navigate(Screen.UniversityOnboarding.route) {
                                popUpTo(Screen.Legal.route) { inclusive = true }
                            }
                        },
                        onSignOut = {
                            navController.navigate(Screen.GoogleSignIn.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.DocumentViewer.route) { backStackEntry ->
                    val docType = backStackEntry.arguments?.getString("docType") ?: "terms"
                    val legalViewModel: com.kletaq.app.features.legal.LegalViewModel = hiltViewModel()
                    val title = if (docType == "terms") "Terms and Conditions" else "Privacy Policy"
                    val content = if (docType == "terms") legalViewModel.getTermsText() else legalViewModel.getPrivacyText()
                    com.kletaq.app.features.legal.DocumentViewerScreen(
                        title = title,
                        contentMarkdown = content,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Nested navigation graph for onboarding — all screens share one OnboardingViewModel
                navigation(
                    startDestination = Screen.UniversityOnboarding.route,
                    route = "onboarding_graph"
                ) {
                    composable(Screen.UniversityOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.UniversitySelectionScreen(
                            viewModel = onboardingViewModel,
                            onUniversityCompleted = {
                                navController.navigate(Screen.BranchOnboarding.route)
                            },
                            onSignOut = {
                                navController.navigate(Screen.GoogleSignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.BranchOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.BranchSelectionScreen(
                            viewModel = onboardingViewModel,
                            onBranchCompleted = {
                                onboardingViewModel.resetStepStates()
                                navController.navigate(Screen.SchemeOnboarding.route)
                            },
                            onBackClick = {
                                onboardingViewModel.resetStepStates()
                                navController.popBackStack()
                            },
                            onSignOut = {
                                navController.navigate(Screen.GoogleSignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.SchemeOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.SchemeSelectionScreen(
                            viewModel = onboardingViewModel,
                            onSchemeCompleted = {
                                onboardingViewModel.resetStepStates()
                                navController.navigate(Screen.SemesterOnboarding.route)
                            },
                            onBackClick = {
                                onboardingViewModel.resetStepStates()
                                navController.popBackStack()
                            },
                            onSignOut = {
                                navController.navigate(Screen.GoogleSignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.SemesterOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        val sem by onboardingViewModel.selectedSemester.collectAsState()

                        com.kletaq.app.features.onboarding.SemesterSelectionScreen(
                            viewModel = onboardingViewModel,
                            onSemesterCompleted = {
                                val nextRoute = when {
                                    onboardingViewModel.shouldShowCycleStep() -> Screen.CycleOnboarding.route
                                    sem > 1 -> Screen.BacklogOnboarding.route
                                    else -> Screen.UsernameOnboarding.route
                                }
                                onboardingViewModel.resetStepStates()
                                navController.navigate(nextRoute)
                            },
                            onBackClick = {
                                onboardingViewModel.resetStepStates()
                                navController.popBackStack()
                            },
                            onSignOut = {
                                navController.navigate(Screen.GoogleSignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.CycleOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.CycleSelectionScreen(
                            viewModel = onboardingViewModel,
                            onCycleCompleted = {
                                onboardingViewModel.resetStepStates()
                                val sem = onboardingViewModel.selectedSemester.value
                                val nextRoute = if (sem > 1) Screen.BacklogOnboarding.route else Screen.UsernameOnboarding.route
                                navController.navigate(nextRoute)
                            },
                            onBackClick = {
                                onboardingViewModel.resetStepStates()
                                navController.popBackStack()
                            },
                            onSignOut = {
                                navController.navigate(Screen.GoogleSignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.BacklogOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.BacklogSelectionScreen(
                            viewModel = onboardingViewModel,
                            onBacklogsCompleted = {
                                onboardingViewModel.resetStepStates()
                                navController.navigate(Screen.UsernameOnboarding.route)
                            },
                            onBackClick = {
                                onboardingViewModel.resetStepStates()
                                navController.popBackStack()
                            },
                            onSignOut = {
                                navController.navigate(Screen.GoogleSignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.UsernameOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.UsernameScreen(
                            viewModel = onboardingViewModel,
                            onUsernameCompleted = {
                                onboardingViewModel.resetStepStates()
                                navController.navigate(Screen.ImportSyllabusOnboarding.route)
                            },
                            onBackClick = {
                                onboardingViewModel.resetStepStates()
                                navController.popBackStack()
                            },
                            onSignOut = {
                                navController.navigate(Screen.GoogleSignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.ImportSyllabusOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.ImportSyllabusScreen(
                            viewModel = onboardingViewModel,
                            onImportCompleted = {
                                navController.navigate(Screen.ConfigureCalendarOnboarding.route) {
                                    popUpTo(Screen.ImportSyllabusOnboarding.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.ConfigureCalendarOnboarding.route) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("onboarding_graph") }
                        val onboardingViewModel: com.kletaq.app.features.onboarding.OnboardingViewModel = hiltViewModel(parentEntry)
                        com.kletaq.app.features.onboarding.ConfigureCalendarScreen(
                            viewModel = onboardingViewModel,
                            onCalendarStepCompleted = {
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.ConfigureCalendarOnboarding.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Welcome.route) {
                        com.kletaq.app.features.onboarding.WelcomeScreen(
                            onStartLearningClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToJourney = { semId, subId, uId, tId ->
                            targetJourneySemesterId = semId
                            targetJourneySubjectId = subId
                            targetJourneyUnitId = uId
                            targetJourneyTopicId = tId
                            navigateToRoute(Screen.Journey.route)
                        },
                        onNavigateToCreateTask = {
                            navController.navigate(Screen.CreateTask.route)
                        },
                        onNavigateToSearch = {
                            navController.navigate(Screen.Search.route)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        }
                    )
                }

                composable(Screen.Journey.route) {
                    JourneyScreen(
                        targetSemesterId = targetJourneySemesterId,
                        targetSubjectId = targetJourneySubjectId,
                        targetUnitId = targetJourneyUnitId,
                        targetTopicId = targetJourneyTopicId,
                        onNavigateToLesson = { lessonNode, subjectName, semesterName ->
                            activeFocusTopicId = lessonNode.id
                            activeFocusTopic = lessonNode.title
                            activeFocusSubject = subjectName
                            activeFocusSemester = semesterName
                            navController.navigate(Screen.Lesson.route)
                        },
                        onNavigateToFocus = { lesson, subject, sem ->
                            activeFocusTopicId = lesson.id
                            activeFocusTopic = lesson.title
                            activeFocusSubject = subject
                            activeFocusSemester = sem
                            navController.navigate(Screen.Focus.route)
                        }
                    )
                }

                composable(Screen.Lesson.route) {
                    com.kletaq.app.features.lesson.LessonScreen(
                        lessonId = activeFocusTopicId ?: "pd_01",
                        lessonTitle = activeFocusTopic ?: "Partial Differentiation",
                        subjectName = activeFocusSubject ?: "Engineering Mathematics II",
                        semesterName = activeFocusSemester ?: "Semester 2",
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.MonthlyHabitTracker.route) {
                    com.kletaq.app.features.tasks.MonthlyHabitTrackerScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.ArchivedHabitMonths.route) {
                    com.kletaq.app.features.settings.ArchivedHabitMonthsScreen(
                        onBackClick = { navController.popBackStack() },
                        onSelectMonth = { yearMonth ->
                            navController.navigate(Screen.MonthlyHabitTracker.route)
                        }
                    )
                }

                composable(Screen.Friends.route) {
                    FriendsScreen(
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateToCreateNote = { navController.navigate(Screen.CreateNote.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Settings.route) {
                    com.kletaq.app.features.settings.SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                        onSignOut = {
                            navController.navigate(Screen.GoogleSignIn.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToBacklogSubjects = { navController.navigate(Screen.ActiveBacklog.route) },
                        onNavigateToStudyCalendar = { navController.navigate(Screen.StudyCalendar.route) },
                        onNavigateToArchivedSemesters = { navController.navigate(Screen.ArchivedSemesters.route) },
                        onNavigateToMonthlyHabitTracker = { navController.navigate(Screen.MonthlyHabitTracker.route) },
                        onNavigateToArchivedHabitMonths = { navController.navigate(Screen.ArchivedHabitMonths.route) }
                    )
                }

                composable(Screen.Notifications.route) {
                    com.kletaq.app.features.notifications.NotificationsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Tasks.route) {
                    com.kletaq.app.features.tasks.TasksScreen(
                        onNavigateToCreateTask = { navController.navigate(Screen.CreateTask.route) },
                        onNavigateToHabitTracker = { navController.navigate(Screen.MonthlyHabitTracker.route) }
                    )
                }

                composable(Screen.Focus.route) {
                    TimerScreen(
                        topicName = activeFocusTopic,
                        subjectName = activeFocusSubject,
                        semesterName = activeFocusSemester
                    )
                }

                composable(Screen.CreateTask.route) {
                    com.kletaq.app.features.tasks.CreateTaskScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.CreateNote.route) {
                    com.kletaq.app.features.notes.CreateNoteScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Search.route) {
                    com.kletaq.app.features.search.SearchScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToTopic = { _, _, _, _ ->
                            navController.navigate(Screen.Journey.route)
                        }
                    )
                }

                composable(Screen.ActiveBacklog.route) {
                    com.kletaq.app.features.settings.ActiveBacklogScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.StudyCalendar.route) {
                    com.kletaq.app.features.settings.StudyCalendarScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.ArchivedSemesters.route) {
                    com.kletaq.app.features.settings.ArchivedSemestersScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        if (showBottomSheet) {
            com.kletaq.app.core.ui.QuickActionMenuSheet(
                onDismiss = { showBottomSheet = false },
                onNavigateToFocus = { navController.navigate(Screen.Focus.route) },
                onNavigateToCreateTask = { navController.navigate(Screen.CreateTask.route) }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CustomNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
