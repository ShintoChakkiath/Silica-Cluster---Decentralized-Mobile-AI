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

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class BinaryRunner(val context: Context) {

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private var currentProcess: Process? = null

    // We assume the binaries are packaged as .so files in jniLibs so Android extracts them.
    private val nativeLibDir = context.applicationInfo.nativeLibraryDir

    suspend fun startLlamaServer(modelPath: String, isWorker: Boolean = false, workerIp: String? = null, threadCount: Int = 4, offloadedLayers: Int = -1, masterPct: Float = 0f, extraRpcNodes: String = "") {
        withContext(Dispatchers.IO) {
            try {
                // Force close any previously orphaned process managed by this runner
                currentProcess?.destroy()
                currentProcess = null

                // ROBUST ZOMBIE KILLER: Android OS updates or swiping the app away leaves the native C++ binary running
                // holding the 50052/8080 port hostage. We must aggressively hunt it down and kill it.
                try {
                    val ps = Runtime.getRuntime().exec("ps")
                    val reader = BufferedReader(InputStreamReader(ps.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line!!.contains("librpc.so") || line!!.contains("libllama.so")) {
                            val parts = line!!.trim().split(Regex("\\s+"))
                            if (parts.size > 1) {
                                val pid = parts[1]
                                Runtime.getRuntime().exec(arrayOf("kill", "-9", pid)).waitFor()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to killall if ps parsing fails
                    try { Runtime.getRuntime().exec(arrayOf("killall", "-9", "librpc.so")).waitFor() } catch (e2: Exception) {}
                    try { Runtime.getRuntime().exec(arrayOf("killall", "-9", "libllama.so")).waitFor() } catch (e2: Exception) {}
                }

                val binaryName = if (isWorker) "librpc.so" else "libllama.so"
                val executable = File(nativeLibDir, binaryName)
                
                if (!executable.exists()) {
                    appendLog("Error: Native binary $binaryName not found in $nativeLibDir. Please compile and package it.")
                    return@withContext
                }

                // Make sure it's executable
                executable.setExecutable(true)

                val cmdArgs = mutableListOf(executable.absolutePath)
                if (isWorker) {
                    var portToUse = 50052
                    for (p in 50052..50100) {
                        try {
                            val socket = java.net.ServerSocket(p)
                            socket.close()
                            portToUse = p
                            break
                        } catch (e: Exception) {}
                    }
                    BridgeStateManager.rpcPort.value = portToUse

                    cmdArgs.add("--host")
                    cmdArgs.add("0.0.0.0")
                    cmdArgs.add("--port")
                    cmdArgs.add(portToUse.toString())
                } else {
                    cmdArgs.add("-m")
                    cmdArgs.add(modelPath)
                    cmdArgs.add("--host")
                    cmdArgs.add("0.0.0.0") // Listen on all interfaces to bypass IPv4/IPv6 loopback mismatches
                    cmdArgs.add("--port")
                    cmdArgs.add("8080")
                    cmdArgs.add("-c")
                    cmdArgs.add("4096") // explicitly set context size to prevent prompt overflow socket drops
                    cmdArgs.add("-t")
                    cmdArgs.add(threadCount.toString())
                    
                    val initialNodes = NodeManager.activeNodes.value.filter { it.status == NodeStatus.ONLINE }
                    val validatedNodes = mutableListOf<String>()
                    
                    if (extraRpcNodes.isNotEmpty()) {
                        validatedNodes.addAll(extraRpcNodes.split(","))
                        ActivityLogger.logSystemMessage("Distributed Sub-Master: Targeting ${validatedNodes.size} secondary workers...")
                    } else if (initialNodes.isNotEmpty()) {
                        ActivityLogger.logSystemMessage("Distributed Pre-Flight: Verifying ${initialNodes.size} workers...")
                        initialNodes.forEach { node ->
                            val targetPort = node.telemetry?.rpcPort ?: 50052
                            val isAlive = NetworkManager.pingWorkerNode(node.ip, port = targetPort, timeoutMs = 1500)
                            if (isAlive) {
                                validatedNodes.add("${node.ip}:$targetPort")
                                ActivityLogger.logSystemMessage("Verified Node: ${node.ip}:$targetPort [READY]")
                            } else {
                                NodeManager.updateNodeStatus(node.ip, NodeStatus.UNREACHABLE)
                                ActivityLogger.logSystemMessage("Skipping Node: ${node.ip} [UNREACHABLE]")
                            }
                        }
                    }

                    if (validatedNodes.isNotEmpty()) {
                        val rpcString = validatedNodes.joinToString(",")
                        cmdArgs.add("--rpc")
                        cmdArgs.add(rpcString)
                        
                        // STABILITY OFF-LOADING: Dynamic Distribution
                        if (offloadedLayers == -1) {
                            // Fallback: Proportional tensor split
                            cmdArgs.add("-ngl")
                            cmdArgs.add("999")
                            cmdArgs.add("--tensor-split")
                            cmdArgs.add("${masterPct.toInt()},${(100f - masterPct).toInt()}")
                        } else {
                            // Precise layer split
                            cmdArgs.add("-ngl")
                            cmdArgs.add(offloadedLayers.toString())
                        }
                        
                        appendLog("Worker Cluster: Multi-node RPC Handshake Initiated...")
                    } else if (initialNodes.isNotEmpty()) {
                        ActivityLogger.logSystemMessage("Warning: No workers responded. Reverting to LOCAL inference.")
                    }
                    
                    // Enforce no-mmap globally on Android to prevent SIGABRT 134 chunk fault assertions
                    // Android's virtual memory mapper often denies contiguous 3GB+ allocations.
                    cmdArgs.add("--no-mmap")
                }

                ActivityLogger.logSystemMessage("Kernel Boot: ${cmdArgs.joinToString(" ")}")
                val pb = ProcessBuilder(cmdArgs)
                pb.redirectErrorStream(true)
                pb.environment()["LD_LIBRARY_PATH"] = nativeLibDir
                
                val process = pb.start()
                currentProcess = process

                BridgeStateManager.isLlamaReady.value = false
                BridgeStateManager.engineCrashed.value = false
                BridgeStateManager.engineCrashReason.value = null
                BridgeStateManager.bridgeLogs.value = emptyList()
                BridgeStateManager.workerLogs.value = emptyList()
                
                var monitorJob: kotlinx.coroutines.Job? = null
                if (isWorker) {
                    val uid = android.os.Process.myUid()
                    monitorJob = CoroutineScope(Dispatchers.IO).launch {
                        var lastRx = android.net.TrafficStats.getUidRxBytes(uid)
                        var lastTx = android.net.TrafficStats.getUidTxBytes(uid)
                        while(isActive) {
                            delay(1500)
                            val currentRx = android.net.TrafficStats.getUidRxBytes(uid)
                            val currentTx = android.net.TrafficStats.getUidTxBytes(uid)
                            val rxDelta = currentRx - lastRx
                            val txDelta = currentTx - lastTx
                            
                            // Filter ambient WebSocket/HTTP pings (under 5KB), but catch all tensor math
                            if (rxDelta > 5_000 || txDelta > 5_000) {
                                val rxStr = if (rxDelta > 1048576) String.format("%.2f MB", rxDelta / 1048576.0) else String.format("%.1f KB", rxDelta / 1024.0)
                                val txStr = if (txDelta > 1048576) String.format("%.2f MB", txDelta / 1048576.0) else String.format("%.1f KB", txDelta / 1024.0)
                                val current = BridgeStateManager.workerLogs.value.toMutableList()
                                current.add("[NETWORK ACTIVITY] ⚡ Tensor Stream -> Rcvd: $rxStr | Sent: $txStr")
                                if (current.size > 500) current.removeAt(0)
                                BridgeStateManager.workerLogs.value = current
                            }
                            
                            lastRx = currentRx
                            lastTx = currentTx
                        }
                    }
                }

                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val msg = line ?: ""
                        
                        // Aggressive Universal Ready Detection (Master or Worker)
                        if (msg.contains("listening", ignoreCase = true) && 
                           (msg.contains("server", ignoreCase = true) || 
                            msg.contains("http", ignoreCase = true) || 
                            msg.contains("rpc", ignoreCase = true) || 
                            msg.contains("port", ignoreCase = true))) {
                            BridgeStateManager.isLlamaReady.value = true
                        }

                        if (isWorker) {
                            val current = BridgeStateManager.workerLogs.value.toMutableList()
                            current.add(msg)
                            if (current.size > 500) current.removeAt(0)
                            BridgeStateManager.workerLogs.value = current
                            
                            // Mirror critical worker events to bridgeLogs for status UI
                            if (msg.contains("listening", ignoreCase = true) || 
                                msg.contains("accepted", ignoreCase = true)) {
                                appendLog("[WORKER] $msg")
                            }
                        } else {
                            ActivityLogger.logSystemMessage(msg)
                            appendLog("[LLM] $msg")
                        }
                        
                        // Parse Peer IP for Workers
                        if (msg.contains("accepted connection from", ignoreCase = true)) {
                            val ipMatch = Regex("accepted connection from ([0-9.]+):", RegexOption.IGNORE_CASE).find(msg)
                            val ip = ipMatch?.groupValues?.get(1) ?: "Unknown"
                            BridgeStateManager.lastPeerIp.value = ip
                            
                            if (isWorker) {
                                val current = BridgeStateManager.workerLogs.value.toMutableList()
                                current.add("[RPC KERNEL] Socket linked -> $ip (Streaming Payload Data)")
                                if (current.size > 500) current.removeAt(0)
                                BridgeStateManager.workerLogs.value = current
                            }
                        } else if (isWorker && msg.contains("connection closed", ignoreCase = true)) {
                            val current = BridgeStateManager.workerLogs.value.toMutableList()
                            current.add("[RPC KERNEL] Transceiver socket closed. Payload executed.")
                            if (current.size > 500) current.removeAt(0)
                            BridgeStateManager.workerLogs.value = current
                        }

                        // Silent tracking of last known error reason
                        if (msg.contains("Error", ignoreCase = true) || 
                            (msg.contains("error", ignoreCase = true) && !msg.contains("error_count: 0")) ||
                            msg.contains("assert", ignoreCase = true) ||
                            msg.contains("Exception", ignoreCase = true) ||
                            msg.contains("CANNOT LINK", ignoreCase = true)) {
                            val cleanMsg = msg.replace(Regex("^\\[(LLM|WORKER)\\] "), "").trim()
                            BridgeStateManager.engineCrashReason.value = cleanMsg
                        }
                    }
                }
                
                monitorJob?.cancel()
                process.waitFor()
                BridgeStateManager.isLlamaReady.value = false
                val exitCode = process.exitValue()
                ActivityLogger.logSystemMessage("Kernel Exit: $exitCode")
                if (exitCode != 0) {
                    BridgeStateManager.engineCrashed.value = true
                    if (BridgeStateManager.engineCrashReason.value == null) {
                        BridgeStateManager.engineCrashReason.value = "Abnormal Exit Code: $exitCode"
                    }
                    appendLog("Error: LLM Engine unexpectedly crashed with exit code $exitCode")
                } else {
                    BridgeStateManager.engineCrashReason.value = null
                    appendLog("System Offline: LLM Engine shutdown cleanly.")
                }
            } catch (e: Exception) {
                BridgeStateManager.isLlamaReady.value = false
                BridgeStateManager.engineCrashed.value = true
                BridgeStateManager.engineCrashReason.value = e.message ?: "Unknown Process Exception"
                appendLog("Error: LLM Engine crash - ${e.message}")
            } finally {
                currentProcess = null
            }
        }
    }

    suspend fun startBridge(bridge: InternetBridge, localPort: Int, bridgeToken: String = "") {
        withContext(Dispatchers.IO) {
            try {
                val binaryName = when(bridge) {
                    InternetBridge.Cloudflare_Free, InternetBridge.Cloudflare_Token -> "libcloudflared.so"
                    InternetBridge.Tailscale -> "libtailscaled.so"
                    InternetBridge.Ngrok -> "libngrok.so"
                }

                val executable = File(nativeLibDir, binaryName)
                if (!executable.exists()) {
                    val errorMsg = "Error: Tunnel binary $binaryName not found."
                    appendLog(errorMsg)
                    BridgeStateManager.currentBridgeUrl.value = errorMsg
                    return@withContext
                }

                executable.setExecutable(true)

                val cmdArgs = mutableListOf(executable.absolutePath)
                when (bridge) {
                    InternetBridge.Cloudflare_Free -> {
                        cmdArgs.add("tunnel")
                        cmdArgs.add("--url")
                        cmdArgs.add("http://127.0.0.1:$localPort")
                        cmdArgs.add("--no-autoupdate")
                    }
                    InternetBridge.Cloudflare_Token -> {
                        if (bridgeToken.isBlank()) {
                            appendLog("Error: Cloudflare Token is required but missing.")
                            BridgeStateManager.currentBridgeUrl.value = "Error: Missing Token"
                            return@withContext
                        }
                        cmdArgs.add("tunnel")
                        cmdArgs.add("--no-autoupdate")
                        cmdArgs.add("run")
                        cmdArgs.add("--token")
                        cmdArgs.add(bridgeToken)
                    }
                    InternetBridge.Ngrok -> {
                        if (bridgeToken.isBlank()) {
                            appendLog("Error: Ngrok Auth Token is required but missing.")
                            BridgeStateManager.currentBridgeUrl.value = "Error: Missing Token"
                            return@withContext
                        }
                        // ngrok authtoken
                        val authPb = ProcessBuilder(executable.absolutePath, "config", "add-authtoken", bridgeToken)
                        authPb.environment()["LD_LIBRARY_PATH"] = nativeLibDir
                        authPb.start().waitFor()
                        
                        cmdArgs.add("http")
                        cmdArgs.add(localPort.toString())
                    }
                    InternetBridge.Tailscale -> {
                        cmdArgs.add("--tun=userspace-networking")
                        cmdArgs.add("--socks5-server=localhost:1055")
                        cmdArgs.add("--outbound-http-proxy-listen=localhost:1055")
                    }
                }

                BridgeStateManager.currentBridgeUrl.value = "Initializing Tunnel..."
                appendLog("Starting Bridge: ${cmdArgs.joinToString(" ")}")
                val pb = ProcessBuilder(cmdArgs)
                pb.redirectErrorStream(true)
                
                val process = pb.start()
                
                val cloudflareRegex = Regex("https://[a-zA-Z0-9.-]+\\.trycloudflare\\.com")
                val ngrokRegex = Regex("https://[a-zA-Z0-9.-]+\\.ngrok-free\\.app")
                
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLog("[$bridge] $line")
                        
                        if (bridge == InternetBridge.Cloudflare_Free) {
                            cloudflareRegex.find(line ?: "")?.let { match ->
                                val url = match.value
                                if (!url.contains("api.trycloudflare") && !url.contains("update.trycloudflare")) {
                                    BridgeStateManager.currentBridgeUrl.value = url
                                }
                            }
                        } else if (bridge == InternetBridge.Cloudflare_Token) {
                            if (line!!.contains("Registered tunnel connection")) {
                                BridgeStateManager.currentBridgeUrl.value = "Active (Registered Named Tunnel)"
                            }
                        } else if (bridge == InternetBridge.Ngrok) {
                            if (line!!.contains("url=https")) {
                                ngrokRegex.find(line ?: "")?.let { match ->
                                    BridgeStateManager.currentBridgeUrl.value = match.value
                                } ?: run {
                                    BridgeStateManager.currentBridgeUrl.value = "Active (Ngrok Tunnel Attached)"
                                }
                            }
                        } else if (bridge == InternetBridge.Tailscale) {
                            if (line!!.contains("tailscale up")) {
                                BridgeStateManager.currentBridgeUrl.value = "Tailscaled Running (Please Auth)"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Bridge Exception: ${e.message}"
                appendLog(errorMsg)
                BridgeStateManager.currentBridgeUrl.value = errorMsg
            }
        }
    }

    fun stop() {
        currentProcess?.destroy()
        currentProcess = null
        appendLog("Process Terminated.")
    }

    private fun appendLog(msg: String) {
        val maxLines = 100
        val temp = _logs.value.toMutableList()
        temp.add(msg)
        if (temp.size > maxLines) {
            temp.removeAt(0)
        }
        _logs.value = temp
        BridgeStateManager.bridgeLogs.value = temp
        Log.d("BinaryRunner", msg)
    }
}
