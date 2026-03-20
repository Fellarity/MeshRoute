package com.gdgc.meshroute

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gdgc.meshroute.logic.PathfindingEngine
import com.gdgc.meshroute.network.MeshNetworkManager
import com.gdgc.meshroute.sync.FirestoreSyncManager
import com.gdgc.meshroute.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var meshManager: MeshNetworkManager

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            startMeshServices()
        } else {
            Toast.makeText(this, "Permissions required for Mesh Network", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Managers
        meshManager = MeshNetworkManager(applicationContext)
        val pathfindingEngine = PathfindingEngine()
        val firestoreSyncManager = FirestoreSyncManager()

        // Initialize ViewModel (Manually for now, without Hilt)
        viewModel = MainViewModel(meshManager, pathfindingEngine, firestoreSyncManager)

        checkPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startMeshServices()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startMeshServices() {
        meshManager.startAdvertising(Build.MODEL)
        meshManager.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stopAll()
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val hazards by viewModel.hazards.collectAsState()
    val safePath by viewModel.safePath.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "Report Hazard")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "MeshRoute Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Safe Path Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculated Safe Exit Path:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = if (safePath.isNotEmpty()) safePath.joinToString(" -> ") else "NO SAFE PATH FOUND",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Nearby Hazard Reports:", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(hazards) { hazard ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Location: ${hazard.nodeName}", style = MaterialTheme.typography.labelLarge)
                            Text(text = hazard.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (showDialog) {
            HazardReportDialog(
                onDismiss = { showDialog = false },
                onReport = { node, desc ->
                    viewModel.reportHazard(node, desc)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun HazardReportDialog(onDismiss: () -> Unit, onReport: (String, String) -> Unit) {
    var nodeName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Emergency Hazard") },
        text = {
            Column {
                TextField(
                    value = nodeName,
                    onValueChange = { nodeName = it },
                    label = { Text("Location (e.g., Stairwell B)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g., Blocked by smoke)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(nodeName, description) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Report & Broadcast")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
