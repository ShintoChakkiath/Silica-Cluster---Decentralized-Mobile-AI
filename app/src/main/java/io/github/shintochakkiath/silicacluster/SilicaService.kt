/*
 * Silica Cluster - Decentralized Mobile AI
 * Copyright (C) 2026 Shinto Chakkiath
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package io.github.shintochakkiath.silicacluster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SilicaService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var binaryRunner: BinaryRunner
    private lateinit var apiGatewayServer: ApiGatewayServer
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        binaryRunner = BinaryRunner(this)
        apiGatewayServer = ApiGatewayServer()
        createNotificationChannel()

        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "SilicaCluster::LlamaWakeLock")
        wakeLock?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when(action) {
            "START" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(1, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(1, buildNotification())
                }
                
                val modelPath = intent.getStringExtra("MODEL_PATH") ?: ""
                val bridgeName = intent.getStringExtra("BRIDGE") ?: "Cloudflare_Free"
                val isWorker = intent.getBooleanExtra("IS_WORKER", false)
                val workerIp = intent.getStringExtra("WORKER_IP")
                val isOffline = intent.getBooleanExtra("IS_OFFLINE", false)
                val bridgeToken = intent.getStringExtra("BRIDGE_TOKEN") ?: ""
                val threadCount = intent.getIntExtra("THREAD_COUNT", 4)
                val extraRpcNodes = intent.getStringExtra("EXTRA_RPC_NODES") ?: ""
                
                // OFF-LOADING LOGIC
                val totalLayers = BridgeStateManager.activeModelTotalLayers.value
                val isFullOffload = BridgeStateManager.fullWorkerOffload.value
                val masterPct = BridgeStateManager.masterComputePercentage.value ?: 0f
                
                val offloadedLayers = if (isFullOffload) {
                    999
                } else if (totalLayers != null) {
                    val masterLayers = (totalLayers * (masterPct / 100f)).toInt()
                    totalLayers - masterLayers
                } else {
                    -1 // Signals to use --tensor-split proportional instead of exact layers
                }

                // Start API Gateway on 8081
                if (!isWorker) {
                    val activeNodes = NodeManager.activeNodes.value.filter { it.status == NodeStatus.ONLINE }
                    if (isFullOffload && activeNodes.isNotEmpty()) {
                        val primaryWorkerIp = activeNodes.first().ip
                        apiGatewayServer.startServer(port = 8081, upstreamIp = primaryWorkerIp)
                        // Trigger worker to start full engine!
                        serviceScope.launch {
                            try {
                                val modelFile = java.io.File(modelPath)
                                val client = io.ktor.client.HttpClient(io.ktor.client.engine.cio.CIO) {
                                    install(io.ktor.client.plugins.HttpTimeout) {
                                        requestTimeoutMillis = 3600_000L // 1 hour for large model transfer
                                        connectTimeoutMillis = 30_000L
                                        socketTimeoutMillis = 3600_000L
                                    }
                                }
                                
                                val checkRes = client.request("http://$primaryWorkerIp:8082/check-model?name=${modelFile.name}") {
                                    method = io.ktor.http.HttpMethod.Get
                                }
                                val checkStr = checkRes.bodyAsText()
                                val exists = org.json.JSONObject(checkStr).optBoolean("exists", false)
                                
                                if (!exists) {
                                    ActivityLogger.logSystemMessage("Syncing LLM to worker (${modelFile.length() / 1024 / 1024} MB)...")
                                    client.request("http://$primaryWorkerIp:8082/upload-model?name=${modelFile.name}") {
                                        method = io.ktor.http.HttpMethod.Post
                                        setBody(object : io.ktor.http.content.OutgoingContent.WriteChannelContent() {
                                            override val contentLength: Long = modelFile.length()
                                            override suspend fun writeTo(channel: io.ktor.utils.io.ByteWriteChannel) {
                                                modelFile.inputStream().use { input ->
                                                    val buffer = ByteArray(16384)
                                                    var bytesRead = input.read(buffer)
                                                    while (bytesRead != -1) {
                                                        channel.writeFully(buffer, 0, bytesRead)
                                                        bytesRead = input.read(buffer)
                                                    }
                                                }
                                            }
                                        })
                                    }
                                    ActivityLogger.logSystemMessage("LLM transfer complete!")
                                }
                                
                                val secondaryWorkers = activeNodes.drop(1).joinToString(",") { "${it.ip}:${it.telemetry?.rpcPort ?: 50052}" }
                                val req = org.json.JSONObject().apply { 
                                    put("modelName", modelFile.name)
                                    put("rpcNodes", secondaryWorkers)
                                }
                                client.request("http://$primaryWorkerIp:8082/start-full-engine") {
                                    method = io.ktor.http.HttpMethod.Post
                                    setBody(req.toString())
                                }
                                ActivityLogger.logSystemMessage("Sub-Master booting. Remote worker is loading model into RAM...")
                                
                                var isWorkerReady = false
                                while (!isWorkerReady && isActive) {
                                    kotlinx.coroutines.delay(2000)
                                    try {
                                        val healthRes = client.request("http://$primaryWorkerIp:8080/health") {
                                            method = io.ktor.http.HttpMethod.Get
                                        }
                                        if (healthRes.status.value in 200..299) {
                                            isWorkerReady = true
                                            BridgeStateManager.isLlamaReady.value = true
                                            ActivityLogger.logSystemMessage("Sub-Master Inference Engine is ONLINE.")
                                        }
                                    } catch (e: Exception) {
                                        // Still loading
                                    }
                                }
                            } catch (e: Exception) {
                                ActivityLogger.logSystemMessage("Failed to start full engine on worker: ${e.message}")
                            }
                        }
                    } else {
                        apiGatewayServer.startServer(port = 8081)
                        // Start Llama Locally
                        serviceScope.launch {
                            binaryRunner.startLlamaServer(modelPath, isWorker, workerIp, threadCount, offloadedLayers, masterPct, extraRpcNodes)
                        }
                    }
                } else {
                    serviceScope.launch {
                        binaryRunner.startLlamaServer(modelPath, isWorker, workerIp, threadCount, offloadedLayers, masterPct, extraRpcNodes)
                    }
                }

                // Start Telemetry Broadcaster (Port 8082)
                TelemetryServer.startServer(this)

                // Start Bridge
                if (!isWorker && !isOffline) {
                    serviceScope.launch {
                        val bridge = try { InternetBridge.valueOf(bridgeName) } catch(e: Exception) { InternetBridge.Cloudflare_Free }
                        binaryRunner.startBridge(bridge, 8081, bridgeToken)
                    }
                }
            }
            "STOP" -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        binaryRunner.stop()
        apiGatewayServer.stopServer()
        TelemetryServer.stopServer()
        serviceJob.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "SilicaServiceChannel",
                "Silica Engine Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "SilicaServiceChannel")
            .setContentTitle("Silica Cluster is Running")
            .setContentText("Your local LLM is active.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}
