package com.HammersTech.RoutineChart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HammersTech.RoutineChart.app.ui.theme.RoutineChartTheme
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeRoutineRepository
import com.HammersTech.RoutineChart.core.data.remote.firebase.CompositeUserRepository
import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.models.PlanTier
import com.HammersTech.RoutineChart.core.domain.models.Role
import com.HammersTech.RoutineChart.core.domain.models.User
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.ULIDGenerator
import com.HammersTech.RoutineChart.features.auth.AuthFlowScreen
import com.HammersTech.RoutineChart.features.child.today.ChildTodayScreen
import com.HammersTech.RoutineChart.features.parent.dashboard.ParentDashboardScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.TimeZone
import javax.inject.Inject

/**
 * Main entry point for the Routine Chart App
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            android.util.Log.d("MainActivity", "onCreate started")
            setContent {
                RoutineChartTheme {
                    MainScreen()
                }
            }
            android.util.Log.d("MainActivity", "setContent completed")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            throw e
        }
    }
}

/**
 * ViewModel for MainActivity to observe auth state and current user
 * Phase 2.1: Firebase Auth
 * Phase 2.3: Auto-create Family and User for non-anonymous parents
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val familyRepository: FamilyRepository,
        private val routineRepository: RoutineRepository,
    ) : ViewModel() {
        val authState: StateFlow<com.HammersTech.RoutineChart.core.domain.models.AuthUser?> =
            authRepository.authStateFlow.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = authRepository.currentUser,
            )

        val currentUser: StateFlow<User?> =
            authState.flatMapLatest { authUser ->
                if (authUser != null) {
                    userRepository.observeById(authUser.id)
                } else {
                    flowOf(null)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

        init {
            android.util.Log.d("MainViewModel", "Auth state initialized. Current user: ${authRepository.currentUser}")

            // Auto-create Family and User for non-anonymous parents when they sign in
            // Also trigger routine upload for existing users
            viewModelScope.launch {
                authState.collect { authUser ->
                    if (authUser != null && !authUser.isAnonymous) {
                        // Non-anonymous user (parent) - check if User record exists
                        val existingUser = userRepository.getById(authUser.id)
                        if (existingUser != null) {
                            // User exists - ensure user document is in Firestore and trigger routine upload
                            if (userRepository is CompositeUserRepository) {
                                try {
                                    userRepository.syncToFirestore(existingUser)
                                    AppLogger.Database.info("✅ Ensured user document exists in Firestore before routine upload")
                                } catch (e: Exception) {
                                    AppLogger.Database.error("⚠️ Failed to sync user to Firestore before routine upload: ${e.message}", e)
                                }
                            }

                            // Phase 3.2: Upload unsynced routines (early implementation of Phase 3.8 background sync)
                            // Phase 3.3: Pull routines from Firestore (early implementation of Phase 3.8 background sync)
                            if (routineRepository is CompositeRoutineRepository) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        // First, upload any unsynced local changes
                                        val uploaded = routineRepository.uploadUnsynced(existingUser.id, existingUser.familyId)
                                        if (uploaded > 0) {
                                            AppLogger.Database.info("✅ Uploaded $uploaded unsynced routine(s) on app launch")
                                        }

                                        // Then, pull any remote changes (this will also pull steps)
                                        val pulled = routineRepository.pullRoutines(existingUser.id, existingUser.familyId)
                                        if (pulled > 0) {
                                            AppLogger.Database.info("✅ Pulled $pulled routine(s) from Firestore on app launch")
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.Database.error("⚠️ Failed to sync routines: ${e.message}", e)
                                    }
                                }
                            }
                            return@collect
                        }

                        // No User record exists - create Family and User
                        try {
                            val timeZone = TimeZone.getDefault().id
                            val family =
                                Family(
                                    id = ULIDGenerator.generate(),
                                    name = null,
                                    timeZone = timeZone,
                                    weekStartsOn = 0, // Sunday
                                    planTier = PlanTier.FREE,
                                    createdAt = Instant.now(),
                                    updatedAt = Instant.now(),
                                )
                            familyRepository.create(family)

                            val displayName = authUser.email?.substringBefore("@") ?: "Parent"
                            val newUser =
                                User(
                                    id = authUser.id,
                                    familyId = family.id,
                                    role = Role.PARENT,
                                    displayName = displayName,
                                    email = authUser.email,
                                    createdAt = Instant.now(),
                                )
                            userRepository.create(newUser)

                            // CRITICAL: Ensure user document exists in Firestore before uploading routines
                            // This is required for Firestore security rules to allow routine uploads
                            if (userRepository is CompositeUserRepository) {
                                try {
                                    userRepository.syncToFirestore(newUser)
                                    AppLogger.Database.info("✅ Ensured user document exists in Firestore before routine upload")
                                } catch (e: Exception) {
                                    AppLogger.Database.error("⚠️ Failed to sync user to Firestore before routine upload: ${e.message}", e)
                                }
                            }

                            // Phase 3.2: Upload unsynced routines (early implementation of Phase 3.8 background sync)
                            // Phase 3.3: Pull routines from Firestore (early implementation of Phase 3.8 background sync)
                            if (routineRepository is CompositeRoutineRepository) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    try {
                                        // First, upload any unsynced local changes
                                        val uploaded = routineRepository.uploadUnsynced(newUser.id, newUser.familyId)
                                        if (uploaded > 0) {
                                            AppLogger.Database.info("✅ Uploaded $uploaded unsynced routine(s) on app launch")
                                        }

                                        // Then, pull any remote changes (this will also pull steps)
                                        val pulled = routineRepository.pullRoutines(newUser.id, newUser.familyId)
                                        if (pulled > 0) {
                                            AppLogger.Database.info("✅ Pulled $pulled routine(s) from Firestore on app launch")
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.Database.error("⚠️ Failed to sync routines: ${e.message}", e)
                                    }
                                }
                            }

                            AppLogger.Database.info("Created family and user for parent: ${authUser.id}")
                            android.util.Log.d("MainViewModel", "Auto-created family ${family.id} and user ${newUser.id} for parent")
                        } catch (e: Exception) {
                            AppLogger.Database.error("Failed to create family and user for parent", e)
                            android.util.Log.e("MainViewModel", "Failed to create family and user", e)
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val authUser by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Debug logging
    androidx.compose.runtime.LaunchedEffect(authUser, currentUser) {
        android.util.Log.d(
            "MainScreen",
            "Auth user state: ${authUser?.let { "User(id=${it.id}, email=${it.email}, anonymous=${it.isAnonymous})" } ?: "null"}",
        )
        android.util.Log.d("MainScreen", "Current user: ${currentUser?.let { "User(id=${it.id}, role=${it.role})" } ?: "null"}")
    }

    if (authUser != null) {
        // User is authenticated - show main content
        android.util.Log.d("MainScreen", "Showing authenticated content")
        AuthenticatedContent(currentUser = currentUser)
    } else {
        // User is not authenticated - show auth flow
        android.util.Log.d("MainScreen", "Showing auth flow")
        AuthFlowScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedContent(currentUser: User?) {
    // If we have a user, show role-based content
    if (currentUser != null) {
        if (currentUser.role == Role.PARENT) {
            // Parent sees both tabs
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            label = { Text("Parent") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Face, contentDescription = null) },
                            label = { Text("Child") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                        )
                    }
                },
            ) { paddingValues ->
                when (selectedTab) {
                    0 ->
                        ParentDashboardScreen(
                            modifier = Modifier.padding(paddingValues),
                        )
                    1 -> ChildTodayScreen()
                }
            }
        } else {
            // Child only sees child view
            ChildTodayScreen()
        }
    } else {
        // No User record yet - show child view by default
        // This handles anonymous sign-ins who haven't joined a family yet
        // (Anonymous users are treated as children until they join a family)
        ChildTodayScreen()
    }
}
