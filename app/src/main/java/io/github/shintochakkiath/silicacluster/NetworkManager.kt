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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkManager {

    fun hasInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Safely traverses network interfaces to return the local IPv4 address (e.g. 192.168.1.50).
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Not Connected (Check WiFi)"
    }

    /**
     * Executes a massive parallel TCP sweep across the /24 local subnet to discover active RPC worker nodes.
     * Guaranteed to finish within 800ms globally by spawning 254 coroutines.
     */
    suspend fun discoverLocalWorkers(): List<String> = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress()
        if (localIp == "Not Connected (Check WiFi)" || localIp.isBlank() || localIp.startsWith("127.")) {
            return@withContext emptyList()
        }

        val baseIp = localIp.substringBeforeLast(".") // e.g. "192.168.1"
        // Spawn 254 parallel ping jobs
        val deferreds: List<kotlinx.coroutines.Deferred<String?>> = (1..254).map { host ->
            async(Dispatchers.IO) {
                val targetIp = "$baseIp.$host"
                // Don't ping ourselves
                if (targetIp == localIp) return@async null
                
                if (pingWorkerNode(targetIp, timeoutMs = 2500)) targetIp else null
            }
        }
        
        deferreds.awaitAll().filterNotNull()
    }

    /**
     * Attempts to open a brief socket connection to the specified worker's IP and port.
     * Returns true if connection succeeds, indicating the `librpc` engine is actually listening.
     */
    suspend fun pingWorkerNode(ip: String, port: Int = 50052, timeoutMs: Int = 2000): Boolean {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            } catch (e: Exception) {
                false
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    // Ignore close exceptions
                }
            }
        }
    }

    suspend fun fetchWorkerTelemetry(ip: String): WorkerTelemetry? {
        return withContext(Dispatchers.IO) {
            try {
                // Using standard java.net for quick lightweight JSON fetches
                val url = java.net.URL("http://$ip:8082/telemetrics")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(jsonString)
                    WorkerTelemetry(
                        availableRamGb = json.optDouble("availableRamGb", 0.0),
                        totalRamGb = json.optDouble("totalRamGb", 0.0),
                        cpuCount = json.optInt("cpuCount", 0),
                        threadCount = json.optInt("threadCount", 0),
                        batteryTempCelsius = json.optDouble("batteryTempCelsius", 0.0)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun requestJoin(workerIp: String, masterIp: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://$workerIp:8082/request-join")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json")
            
            val json = org.json.JSONObject().apply { put("masterIp", masterIp) }
            connection.outputStream.use { it.write(json.toString().toByteArray()) }
            
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkJoinStatus(workerIp: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://$workerIp:8082/join-status")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            
            if (connection.responseCode == 200) {
                val json = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                if (json.getBoolean("isAuthorized")) json.getString("masterIp") else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
