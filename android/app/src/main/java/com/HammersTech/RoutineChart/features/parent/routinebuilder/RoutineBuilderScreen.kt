package com.HammersTech.RoutineChart.features.parent.routinebuilder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.HammersTech.RoutineChart.core.domain.models.Routine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(
    routine: Routine?,
    onDismiss: () -> Unit,
    viewModel: RoutineBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Use a key that includes both routine and a unique identifier to force reset
    LaunchedEffect(routine?.id ?: "new") {
        viewModel.initialize(routine)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (routine == null) "New Routine" else "Edit Routine")
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onSuccess = onDismiss) },
                        enabled = state.canSave && !state.isSaving,
                    ) {
                        Text("Save")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Routine Details Section
            item {
                Text(
                    text = "Routine Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Routine Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }

            item {
                OutlinedTextField(
                    value = state.iconName,
                    onValueChange = viewModel::updateIconName,
                    label = { Text("Icon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Steps Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            itemsIndexed(state.steps) { index, step ->
                StepItem(
                    index = index,
                    step = step,
                    onLabelChange = { label ->
                        viewModel.updateStep(index, label, step.iconName)
                    },
                    onIconChange = { icon ->
                        viewModel.updateStep(index, step.label, icon)
                    },
                    onDelete = {
                        viewModel.removeStep(index)
                    },
                )
            }

            item {
                Button(
                    onClick = viewModel::addStep,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Step")
                }
            }

            // Child Assignment Section
            if (state.children.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Assign to Children",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(state.children.size) { index ->
                    val child = state.children[index]
                    ChildAssignmentItem(
                        childName = child.displayName,
                        childIcon = child.avatarIcon ?: "👤",
                        isSelected = state.selectedChildIds.contains(child.id),
                        onToggle = { viewModel.toggleChildSelection(child.id) },
                    )
                }
            }

            // Error Section
            if (state.error != null) {
                item {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StepItem(
    index: Int,
    step: StepInput,
    onLabelChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Step number
        Text(
            text = "${index + 1}.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(32.dp),
        )

        // Icon
        OutlinedTextField(
            value = step.iconName,
            onValueChange = onIconChange,
            modifier = Modifier.width(60.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
        )

        // Label
        OutlinedTextField(
            value = step.label,
            onValueChange = onLabelChange,
            label = { Text("Step name") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        // Delete button
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Step",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun ChildAssignmentItem(
    childName: String,
    childIcon: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = childIcon,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = childName,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
        )
    }
}
