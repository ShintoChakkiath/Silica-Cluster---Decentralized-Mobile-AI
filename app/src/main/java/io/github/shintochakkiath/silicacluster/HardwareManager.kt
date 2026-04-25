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

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

object HardwareManager {

    data class DeviceInfo(
        val availableRamGb: Double,
        val totalRamGb: Double,
        val cpuCount: Int,
        val freeStorageGb: Double,
        val batteryTempCelsius: Double,
        val measuredTops: Double
    )

    private var cachedTops: Double? = null

    private fun measureSyntheticTops(): Double {
        if (cachedTops != null) return cachedTops!!
        
        val cores = maxOf(1, Runtime.getRuntime().availableProcessors())
        val opsPerThread = 20_000_000L
        val start = System.nanoTime()
        
        val threads = (0 until cores).map {
            Thread {
                var sum = 0L
                for (i in 0 until opsPerThread) {
                    sum += (i * 3) xor (i shr 2)
                }
            }.apply { start() }
        }
        threads.forEach { it.join() }
        
        val durationSec = maxOf(0.001, (System.nanoTime() - start) / 1_000_000_000.0)
        val totalOps = cores * opsPerThread * 3.0
        
        // Mathematically accurate CPU throughput (Giga-Operations Per Second)
        // We no longer scale this with fake multipliers for NPU assumptions.
        val gops = (totalOps / durationSec) / 1_000_000_000.0
        
        cachedTops = maxOf(0.1, gops)
        return cachedTops!!
    }

    fun getDeviceInfo(context: Context): DeviceInfo {
        return try {
            // RAM
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)
            
            val totalMem = memoryInfo.totalMem.takeIf { it > 0 } ?: 1L
            val availableRam = memoryInfo.availMem.toDouble() / (1024 * 1024 * 1024)
            val totalRam = totalMem.toDouble() / (1024 * 1024 * 1024)

            // Cores
            val cores = maxOf(1, Runtime.getRuntime().availableProcessors())

            // Storage
            val storageDir = Environment.getDataDirectory()
            val freeStorage = try {
                val stat = StatFs(storageDir.path)
                (stat.availableBlocksLong * stat.blockSizeLong).toDouble() / (1024 * 1024 * 1024)
            } catch (e: Exception) { 0.0 }

            // Temp
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter)
            val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempCelsius = tempRaw / 10.0

            val tops = measureSyntheticTops()

            DeviceInfo(availableRam, totalRam, cores, freeStorage, tempCelsius, tops)
        } catch (e: Exception) {
            // Absolute fail-safe default values
            DeviceInfo(0.0, 1.0, 1, 0.0, 0.0, 1.0)
        }
    }
}
