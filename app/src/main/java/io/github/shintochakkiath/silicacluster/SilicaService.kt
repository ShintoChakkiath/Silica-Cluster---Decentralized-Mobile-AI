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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
                val bridgeToken = intent.getStringExtra("BRIDGE_TOKEN") ?: ""
                val threadCount = intent.getIntExtra("THREAD_COUNT", 4)

                // Start API Gateway on 8081
                if (!isWorker) {
                    apiGatewayServer.startServer(port = 8081)
                }

                // Start Llama
                serviceScope.launch {
                    binaryRunner.startLlamaServer(modelPath, isWorker, workerIp, threadCount)
                }

                // Start Telemetry Broadcaster (Port 8082)
                TelemetryServer.startServer(this)

                // Start Bridge
                if (!isWorker) {
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
