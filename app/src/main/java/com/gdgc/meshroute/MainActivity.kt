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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gdgc.meshroute.logic.PathfindingEngine
import com.gdgc.meshroute.network.MeshNetworkManager
import com.gdgc.meshroute.sync.FirestoreSyncManager

class MainActivity : ComponentActivity() {

    private lateinit var meshManager: MeshNetworkManager
    private lateinit var pathfindingEngine: PathfindingEngine
    private lateinit var firestoreSyncManager: FirestoreSyncManager

    // Required permissions for Nearby Connections on Android 12+
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
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startMeshServices()
        } else {
            Toast.makeText(this, "Permissions required for Mesh Network", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Managers
        meshManager = MeshNetworkManager(applicationContext)
        pathfindingEngine = PathfindingEngine()
        firestoreSyncManager = FirestoreSyncManager()

        checkPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(meshManager)
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
        // Start advertising and discovery on launch for an emergency tool
        meshManager.startAdvertising(Build.MODEL)
        meshManager.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stopAll()
    }
}

@Composable
fun MainScreen(meshManager: MeshNetworkManager) {
    val receivedHazard by meshManager.receivedHazards.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MeshRoute Status: Active",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Latest Hazard Data:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = receivedHazard ?: "No reports received yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
