package com.HammersTech.RoutineChart.features.child.today

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildTodayScreen(viewModel: ChildTodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Routines") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                state.isLoading -> {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                state.error != null -> {
                    Text(
                        text = "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    ChildTodayContent(
                        state = state,
                        onChildSelected = viewModel::selectChild,
                        onStepToggled = viewModel::toggleStep,
                    )
                }
            }
        }
    }
}

@Composable
fun ChildTodayContent(
    state: ChildTodayState,
    onChildSelected: (ChildProfile) -> Unit,
    onStepToggled: (String, String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Child selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.children.forEach { child ->
                FilterChip(
                    selected = state.selectedChild?.id == child.id,
                    onClick = { onChildSelected(child) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(child.avatarIcon ?: "")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(child.displayName)
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Routines list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.routines) { routineWithSteps ->
                RoutineCard(
                    routineWithSteps = routineWithSteps,
                    onStepToggled = onStepToggled,
                )
            }
        }
    }
}

@Composable
fun RoutineCard(
    routineWithSteps: RoutineWithSteps,
    onStepToggled: (String, String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Routine header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = routineWithSteps.routine.iconName ?: "📋",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = routineWithSteps.routine.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Completion counter
                val completedCount = routineWithSteps.stepCompletionStates.count { it.value }
                val totalCount = routineWithSteps.steps.size
                Text(
                    text = "$completedCount/$totalCount",
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (routineWithSteps.isRoutineComplete) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Steps
            routineWithSteps.steps.forEach { step ->
                val isComplete = routineWithSteps.stepCompletionStates[step.id] ?: false
                StepRow(
                    step = step,
                    isComplete = isComplete,
                    onClick = {
                        onStepToggled(routineWithSteps.routine.id, step.id)
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StepRow(
    step: com.HammersTech.RoutineChart.core.domain.models.RoutineStep,
    isComplete: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkbox
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isComplete) Color(0xFF4CAF50) else Color.LightGray,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Complete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Step icon and label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = step.iconName ?: "⚪",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = step.label ?: "Step ${step.orderIndex + 1}",
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (isComplete) TextDecoration.LineThrough else null,
                color =
                    if (isComplete) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}
