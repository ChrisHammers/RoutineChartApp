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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.HammersTech.RoutineChart.app.ui.theme.RoutineChartTheme
import com.HammersTech.RoutineChart.features.child.today.ChildTodayScreen
import com.HammersTech.RoutineChart.features.parent.dashboard.ParentDashboardScreen
import dagger.hilt.android.AndroidEntryPoint

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
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

