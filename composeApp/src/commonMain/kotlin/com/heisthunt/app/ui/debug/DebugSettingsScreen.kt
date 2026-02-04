package com.heisthunt.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heisthunt.app.location.MockLocationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mockEnabled by remember { mutableStateOf(MockLocationService.isEnabled) }
    var selectedScenario by remember { mutableStateOf<MockLocationService.TestScenario>(
        MockLocationService.TestScenario.SEOUL_CITY_HALL
    ) }
    var showCustomDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🧪 Debug Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF020617),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mock Location Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Mock Location Service",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Use simulated GPS locations for testing",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Switch(
                                checked = mockEnabled,
                                onCheckedChange = { enabled ->
                                    mockEnabled = enabled
                                    if (enabled) {
                                        MockLocationService.enable()
                                        MockLocationService.setScenario(selectedScenario)
                                    } else {
                                        MockLocationService.disable()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF22C55E),
                                    checkedTrackColor = Color(0xFF22C55E).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // Location Scenarios
            if (mockEnabled) {
                item {
                    Text(
                        "Test Scenarios",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(MockLocationService.TestScenario.values()) { scenario ->
                    LocationScenarioCard(
                        scenario = scenario,
                        isSelected = selectedScenario == scenario,
                        onClick = {
                            selectedScenario = scenario
                            MockLocationService.setScenario(scenario)
                        }
                    )
                }

                // Custom Location
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedScenario is MockLocationService.TestScenario.Custom) {
                                Color(0xFF3B82F6).copy(alpha = 0.2f)
                            } else {
                                Color(0xFF1E293B)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Custom Location",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                if (selectedScenario is MockLocationService.TestScenario.Custom) {
                                    val custom = selectedScenario as MockLocationService.TestScenario.Custom
                                    Text(
                                        "(${custom.latitude}, ${custom.longitude})",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Text(
                                "SET",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomLocationDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { lat, lng ->
                MockLocationService.setCustomLocation(lat, lng)
                selectedScenario = MockLocationService.TestScenario.Custom(lat, lng)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun LocationScenarioCard(
    scenario: MockLocationService.TestScenario,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF22C55E).copy(alpha = 0.2f)
            } else {
                Color(0xFF1E293B)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                scenario.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (isSelected) {
                Text(
                    "✓",
                    fontSize = 20.sp,
                    color = Color(0xFF22C55E)
                )
            }
        }
    }
}

@Composable
private fun CustomLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var latText by remember { mutableStateOf("37.5665") }
    var lngText by remember { mutableStateOf("126.9780") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Custom Location", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        onConfirm(lat, lng)
                    }
                }
            ) {
                Text("SET")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
