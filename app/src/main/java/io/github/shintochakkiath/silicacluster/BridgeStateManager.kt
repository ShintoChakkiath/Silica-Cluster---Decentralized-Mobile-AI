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

import kotlinx.coroutines.flow.MutableStateFlow

object BridgeStateManager {
    val currentBridgeUrl = MutableStateFlow<String?>(null)
    val bridgeLogs = MutableStateFlow<List<String>>(emptyList())
    val bridgeToken = MutableStateFlow<String>("")
    val threadCount = MutableStateFlow<Int>(4)
    val isLlamaReady = MutableStateFlow<Boolean>(false)
    val rpcPort = MutableStateFlow<Int>(50052)
    val engineCrashed = MutableStateFlow<Boolean>(false)
    val engineCrashReason = MutableStateFlow<String?>(null)
    val lastPeerIp = MutableStateFlow<String?>(null)
    
    // HANDSHAKE STATE
    val incomingRequestIp = MutableStateFlow<String?>(null)
    val trustedMasterIp = MutableStateFlow<String?>(null)
    val workerStatus = MutableStateFlow<String>("Listening") // Listening, Verifying, Connected, Error
    val workerLogs = MutableStateFlow<List<String>>(emptyList())
    
    // LAYER DISTRIBUTION
    val masterComputePercentage = MutableStateFlow<Float?>(null) // No hardcoded default
    val fullWorkerOffload = MutableStateFlow(false) 
    val activeModelTotalLayers = MutableStateFlow<Int?>(null) // Nullable if parsing fails
}
