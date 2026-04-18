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

enum class NodeStatus {
    UNKNOWN,
    ONLINE,
    UNREACHABLE,
    VERIFYING
}

data class WorkerTelemetry(
    val availableRamGb: Double,
    val totalRamGb: Double,
    val cpuCount: Int,
    val threadCount: Int,
    val batteryTempCelsius: Double
)

data class WorkerNode(
    val ip: String,
    var status: NodeStatus = NodeStatus.UNKNOWN,
    var telemetry: WorkerTelemetry? = null
)

object NodeManager {
    val activeNodes = MutableStateFlow<List<WorkerNode>>(emptyList())

    fun addNode(ip: String) {
        val current = activeNodes.value.toMutableList()
        // Avoid duplicates
        if (current.none { it.ip == ip }) {
            current.add(WorkerNode(ip))
            activeNodes.value = current
        }
    }

    fun removeNode(ip: String) {
        activeNodes.value = activeNodes.value.filter { it.ip != ip }
    }

    fun updateNodeStatus(ip: String, status: NodeStatus) {
        activeNodes.value = activeNodes.value.map { 
            if (it.ip == ip) it.copy(status = status) else it 
        }
    }

    fun updateNodeTelemetry(ip: String, telemetry: WorkerTelemetry?) {
        activeNodes.value = activeNodes.value.map {
            if (it.ip == ip) it.copy(telemetry = telemetry) else it
        }
    }
}
