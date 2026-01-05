package com.HammersTech.RoutineChart.features.parent.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.features.parent.routinebuilder.RoutineBuilderScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: ParentDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showRoutineBuilder by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }

    Box(modifier = modifier) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Routines") }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingRoutine = null
                        showRoutineBuilder = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Routine")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            when {
                state.isLoading -> {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadData() }) {
                            Text("Retry")
                        }
                    }
                }

                state.routines.isEmpty() -> {
                    EmptyState(
                        onCreateRoutine = {
                            editingRoutine = null
                            showRoutineBuilder = true
                        }
                    )
                }

                else -> {
                    RoutinesList(
                        routines = state.routines,
                        onRoutineClick = { routine ->
                            editingRoutine = routine
                            showRoutineBuilder = true
                        },
                        onDeleteRoutine = { routine ->
                            viewModel.deleteRoutine(routine.id)
                        }
                    )
                }
            }
        }
    }

    // Routine Builder Dialog/Sheet - Outside the Scaffold
    if (showRoutineBuilder) {
        RoutineBuilderScreen(
            routine = editingRoutine,
            onDismiss = {
                showRoutineBuilder = false
                editingRoutine = null
                viewModel.loadData()
            }
        )
    }
    }
}

@Composable
fun RoutinesList(
    routines: List<Routine>,
    onRoutineClick: (Routine) -> Unit,
    onDeleteRoutine: (Routine) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(routines) { routine ->
            RoutineCard(
                routine = routine,
                onClick = { onRoutineClick(routine) },
                onDelete = { onDeleteRoutine(routine) }
            )
        }
    }
}

@Composable
fun RoutineCard(
    routine: Routine,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Text(
                    text = routine.iconName ?: "📋",
                    style = MaterialTheme.typography.headlineMedium
                )

                // Title and version
                Column {
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Version ${routine.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Routine",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyState(onCreateRoutine: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📋",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Routines Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create your first routine to get started!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onCreateRoutine) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Create Routine")
        }
    }
}

