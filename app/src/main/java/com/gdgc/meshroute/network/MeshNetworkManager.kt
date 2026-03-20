package com.gdgc.meshroute.network

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

class MeshNetworkManager(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.gdgc.meshroute.SERVICE_ID"
    private val strategy = Strategy.P2P_CLUSTER

    private val _receivedHazards = MutableStateFlow<String?>(null)
    val receivedHazards = _receivedHazards.asStateFlow()

    fun startAdvertising(localEndpointName: String) {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startAdvertising(
            localEndpointName,
            serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            // Advertising started successfully
        }.addOnFailureListener { e ->
            // Advertising failed to start
        }
    }

    fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            // Discovery started successfully
        }.addOnFailureListener { e ->
            // Discovery failed to start
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept connection for emergency use-case
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    // Connected! Now you can send/receive payloads
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
                ConnectionsStatusCodes.STATUS_ERROR -> {}
            }
        }

        override fun onDisconnected(endpointId: String) {
            // Handle disconnection
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // Endpoint found, request connection
            connectionsClient.requestConnection(
                "LocalDevice",
                endpointId,
                connectionLifecycleCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {
            // Handle endpoint lost
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes()?.let { String(it, StandardCharsets.UTF_8) }
                _receivedHazards.value = data
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Monitor transfer progress
        }
    }

    fun sendHazardReport(endpointId: String, report: String) {
        val payload = Payload.fromBytes(report.toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }
}
