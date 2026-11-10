package com.example.myempty.tictactoe

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class NearbyConnectionManager(
    private val context: Context,
    private val onMoveReceived: (Int) -> Unit,
    private val onRestartReceived: () -> Unit,
    private val onConnected: (isHost: Boolean) -> Unit,
    private val onDisconnected: () -> Unit,
    private val onStatusUpdate: (String) -> Unit
) {
    // P2P_STAR is optimal for both Bluetooth and Wi-Fi Direct / Local Hotspot mesh
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.example.myempty.tictactoe.SERVICE_ID"
    private var connectedEndpointId: String? = null
    private var isHost = false

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val code = bytes[0].toInt()
                if (code == 99) {
                    onRestartReceived()
                } else if (code in 0..8) {
                    onMoveReceived(code)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            onStatusUpdate("Connecting to ${connectionInfo.endpointName}...")
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpointId = endpointId
                onStatusUpdate("Connected!")
                onConnected(isHost)
            } else {
                onStatusUpdate("Connection failed: ${result.status.statusMessage}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpointId = null
            onDisconnected()
            onStatusUpdate("Opponent disconnected.")
        }
    }

    fun startAdvertising() {
        isHost = true
        onStatusUpdate("Hosting on Hotspot/BT... Waiting...")
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .build()

        Nearby.getConnectionsClient(context)
            .startAdvertising("Host", serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnFailureListener { onStatusUpdate("Failed to host: ${it.localizedMessage}") }
    }

    fun startDiscovery() {
        isHost = false
        onStatusUpdate("Scanning over Hotspot/BT...")
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .build()

        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                onStatusUpdate("Found host! Connecting...")
                Nearby.getConnectionsClient(context)
                    .requestConnection("Guest", endpointId, connectionLifecycleCallback)
            }

            override fun onEndpointLost(endpointId: String) {
                onStatusUpdate("Host lost.")
            }
        }

        Nearby.getConnectionsClient(context)
            .startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
            .addOnFailureListener { onStatusUpdate("Scan failed: ${it.localizedMessage}") }
    }

    fun sendMove(index: Int) {
        connectedEndpointId?.let { id ->
            val payload = Payload.fromBytes(byteArrayOf(index.toByte()))
            Nearby.getConnectionsClient(context).sendPayload(id, payload)
        }
    }

    fun sendRestart() {
        connectedEndpointId?.let { id ->
            val payload = Payload.fromBytes(byteArrayOf(99.toByte()))
            Nearby.getConnectionsClient(context).sendPayload(id, payload)
        }
    }

    fun stop() {
        Nearby.getConnectionsClient(context).stopAdvertising()
        Nearby.getConnectionsClient(context).stopDiscovery()
        Nearby.getConnectionsClient(context).stopAllEndpoints()
        connectedEndpointId = null
    }
}
