package com.HammersTech.RoutineChart.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.HammersTech.RoutineChart.core.domain.models.Role
import com.HammersTech.RoutineChart.features.familyinvite.JoinFamilyOptionsScreen
import com.HammersTech.RoutineChart.features.familyinvite.JoinWithCodeScreen
import com.HammersTech.RoutineChart.features.familyinvite.ScanInviteScreen

/**
 * Settings screen with family management options
 * Phase 2.3: User Linking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showJoinFamilyOptions by remember { mutableStateOf(false) }
    var showScanQR by remember { mutableStateOf(false) }
    var showEnterCode by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Done")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            // Family Section
            Text(
                text = "Family",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Join a Family (available for both children and parents)
            // Parents can join other families (e.g., co-parenting scenarios)
            ListItem(
                headlineContent = { Text("Join a Family") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showJoinFamilyOptions = true }
            )
            Divider()
            
            // Testing Section (DEBUG only)
            android.util.Log.d("SettingsScreen", "BuildConfig.DEBUG=${com.HammersTech.RoutineChart.BuildConfig.DEBUG}")
            if (com.HammersTech.RoutineChart.BuildConfig.DEBUG && state.currentUser?.role == Role.PARENT) {
                Text(
                    text = "Testing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                
                ListItem(
                    headlineContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Test Children")
                            if (state.isCreatingTestData) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isCreatingTestData) {
                            viewModel.createTestChildren()
                        }
                )
                
                if (state.testDataMessage != null) {
                    Text(
                        text = state.testDataMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                
                Divider()
            }
            
            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.0.0") }
            )
        }
    }
    
    // Join Family Options Dialog
    if (showJoinFamilyOptions) {
        JoinFamilyOptionsScreen(
            onDismiss = { showJoinFamilyOptions = false },
            onScanQR = {
                showJoinFamilyOptions = false
                showScanQR = true
            },
            onEnterCode = {
                showJoinFamilyOptions = false
                showEnterCode = true
            }
        )
    }
    
    // Scan QR Screen
    if (showScanQR) {
        ScanInviteScreen(
            onDismiss = { showScanQR = false },
            onJoinSuccess = {
                showScanQR = false
                onDismiss()
            }
        )
    }
    
    // Enter Code Screen
    if (showEnterCode) {
        JoinWithCodeScreen(
            onDismiss = { showEnterCode = false },
            onJoinSuccess = {
                showEnterCode = false
                onDismiss()
            }
        )
    }
}
