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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.HammersTech.RoutineChart.app.ui.theme.RoutineChartTheme
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.features.auth.AuthFlowScreen
import com.HammersTech.RoutineChart.features.child.today.ChildTodayScreen
import com.HammersTech.RoutineChart.features.parent.dashboard.ParentDashboardScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
 * ViewModel for MainActivity to observe auth state
 * Phase 2.1: Firebase Auth
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    val authState: StateFlow<com.HammersTech.RoutineChart.core.domain.models.AuthUser?> = 
        authRepository.authStateFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = authRepository.currentUser
        )
    
    init {
        android.util.Log.d("MainViewModel", "Auth state initialized. Current user: ${authRepository.currentUser}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val authUser by viewModel.authState.collectAsState()
    
    // Debug logging
    androidx.compose.runtime.LaunchedEffect(authUser) {
        android.util.Log.d("MainScreen", "Auth user state: ${authUser?.let { "User(id=${it.id}, email=${it.email}, anonymous=${it.isAnonymous})" } ?: "null"}")
    }
    
    if (authUser != null) {
        // User is authenticated - show main content
        android.util.Log.d("MainScreen", "Showing authenticated content")
        AuthenticatedContent()
    } else {
        // User is not authenticated - show auth flow
        android.util.Log.d("MainScreen", "Showing auth flow")
        AuthFlowScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedContent() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text("Parent") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Face, contentDescription = null) },
                    label = { Text("Child") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> ParentDashboardScreen(
                modifier = Modifier.padding(paddingValues)
            )
            1 -> ChildTodayScreen()
        }
    }
}

