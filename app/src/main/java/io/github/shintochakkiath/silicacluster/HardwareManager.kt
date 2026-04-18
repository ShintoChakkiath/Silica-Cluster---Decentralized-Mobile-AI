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
        val batteryTempCelsius: Double
    )

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

            DeviceInfo(availableRam, totalRam, cores, freeStorage, tempCelsius)
        } catch (e: Exception) {
            // Absolute fail-safe default values
            DeviceInfo(0.0, 1.0, 1, 0.0, 0.0)
        }
    }
}
