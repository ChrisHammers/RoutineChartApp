package com.HammersTech.RoutineChart.features.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Test screen for SyncCursor CRUD operations
 * Phase 3.1: Sync Infrastructure
 */
@Composable
fun SyncCursorTestScreen(
    viewModel: SyncCursorTestViewModel = hiltViewModel()
) {
    val testResults by viewModel.testResults.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SyncCursor Test") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SyncCursor CRUD Test",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                )
                Text(
                    text = "Phase 3.1: Sync Infrastructure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Test Results
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(testResults) { result ->
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Run All Tests Button
            Button(
                onClick = { viewModel.runTests() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isRunning) "Running Tests..." else "Run All Tests",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            // Individual Test Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Individual Tests",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TestButton(
                        text = "Create",
                        onClick = { viewModel.testCreate() },
                        modifier = Modifier.weight(1f)
                    )
                    TestButton(
                        text = "Read",
                        onClick = { viewModel.testRead() },
                        modifier = Modifier.weight(1f)
                    )
                    TestButton(
                        text = "Update",
                        onClick = { viewModel.testUpdate() },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TestButton(
                        text = "Get All",
                        onClick = { viewModel.testGetAll() },
                        modifier = Modifier.weight(1f)
                    )
                    TestButton(
                        text = "Delete",
                        onClick = { viewModel.testDelete() },
                        modifier = Modifier.weight(1f)
                    )
                    TestButton(
                        text = "Clear",
                        onClick = { viewModel.clearResults() },
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}
