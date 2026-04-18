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

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.contentLength
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import io.ktor.http.HttpStatusCode
import io.ktor.client.plugins.*

data class DownloadState(val progress: Int, val speedMbps: String = "", val modelName: String = "")

object ModelDownloader {
    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                connectTimeoutMillis = 15000 // 15s connection timeout
                socketTimeoutMillis = 30000 // 30s socket inactivity timeout
            }
            engine {
                requestTimeout = 0 
            }
        }
    }

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()

    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    fun startDownload(modelName: String, url: String, file: File) {
        if (downloadJobs.containsKey(modelName)) return

        _activeDownloads.update { current ->
            current.toMutableMap().apply { put(modelName, DownloadState(0, "Starting...", modelName)) }
        }

        val job = downloadScope.launch {
            downloadModel(url, file).collect { state ->
                val stateWithName = state.copy(modelName = modelName)
                _activeDownloads.update { current ->
                    current.toMutableMap().apply { put(modelName, stateWithName) }
                }
                if (state.progress == 100 || state.progress == -1) {
                    kotlinx.coroutines.delay(2000)
                    downloadJobs.remove(modelName)
                    _activeDownloads.update { current ->
                        current.toMutableMap().apply { remove(modelName) }
                    }
                }
            }
        }
        downloadJobs[modelName] = job
    }

    fun stopDownload(modelName: String) {
        downloadJobs[modelName]?.cancel()
        downloadJobs.remove(modelName)
        _activeDownloads.update { current ->
            current.toMutableMap().apply { remove(modelName) }
        }
    }

    fun downloadModel(url: String, file: File): Flow<DownloadState> = flow {
        if (url.isBlank()) {
            Log.w("ModelDownloader", "Download URL is completely blank! Simulating 100% since no URL provided.")
            emit(DownloadState(100))
            return@flow
        }
        
        val startBytes = if (file.exists()) file.length() else 0L
        Log.d("ModelDownloader", "Starting strict stream download from: $url. Resuming from byte: $startBytes")
        
        try {
            client.prepareGet(url) {
                header("User-Agent", USER_AGENT)
                header("Accept", "*/*")
                if (startBytes > 0) {
                    header("Range", "bytes=$startBytes-")
                }
            }.execute { response ->
                
                if (response.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
                    Log.w("ModelDownloader", "Range not satisfiable, file might already be complete or corrupted. Deleting and restarting.")
                    file.delete()
                    throw Exception("Range not satisfiable, restarting download.")
                }

                if (response.status.value !in 200..299) {
                    Log.e("ModelDownloader", "Download failed with HTTP ${response.status.value}")
                    throw Exception("HTTP Error: ${response.status.value}")
                }

                val isResuming = response.status == HttpStatusCode.PartialContent
                var bytesCopied: Long = if (isResuming) startBytes else { 
                    if (file.exists()) file.delete() 
                    0L 
                }
                
                val remoteSize = response.contentLength() ?: 0L
                val totalBytes = remoteSize + if (isResuming) startBytes else 0L
                
                Log.d("ModelDownloader", "Confirmed stream: totalSize=$totalBytes, resuming=$isResuming, bytesStart=$bytesCopied")
                
                file.parentFile?.mkdirs() // CRITICAL: Ensure the directory actually exists before writing!
                
                val data = ByteArray(1024 * 128) // 128KB Buffer for speed
                val channel: ByteReadChannel = response.bodyAsChannel()
                
                var lastTime = System.currentTimeMillis()
                var bytesSinceLastTime = 0L

                java.io.FileOutputStream(file, isResuming).use { outputStream ->
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(data, 0, data.size)
                        if (read == -1) break
                        
                        if (read > 0) {
                            outputStream.write(data, 0, read)
                            bytesCopied += read
                            bytesSinceLastTime += read
                            
                            val currentTime = System.currentTimeMillis()
                            val timeDiff = currentTime - lastTime
                            
                            // Emit updates every 500ms to stay responsive but avoid spamming StateFlow
                            if (timeDiff > 500) {
                                var speed = ""
                                if (timeDiff > 0) {
                                    val mbps = (bytesSinceLastTime * 1000.0) / timeDiff / (1024.0 * 1024.0)
                                    speed = String.format("%.2f MB/s", mbps)
                                }
                                
                                val progress = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                                emit(DownloadState(progress, speed))
                                
                                lastTime = currentTime
                                bytesSinceLastTime = 0
                            }
                        }
                    }
                    outputStream.flush()
                }
                
                if (totalBytes > 0 && bytesCopied < totalBytes) {
                    throw Exception("Stream dropped prematurely! Got $bytesCopied of $totalBytes bytes.")
                }
            }
            Log.d("ModelDownloader", "Download completely written to disk: ${file.absolutePath}")
            try {
                File(file.absolutePath + ".completed").createNewFile()
            } catch (e: Exception) {
                Log.e("ModelDownloader", "Failed to create completion marker: ${e.message}")
            }
            emit(DownloadState(100, "Syncing..."))
            kotlinx.coroutines.delay(500)
            emit(DownloadState(100, "Complete"))
        } catch (e: Exception) {
            Log.e("ModelDownloader", "Download crash or timeout: ${e.message}")
            // DO NOT delete the file on error! This allows the user to resume the download later.
            emit(DownloadState(-1)) // notify UI of failure
        }
    }.flowOn(Dispatchers.IO)

    suspend fun isUpdateAvailable(url: String, localFile: File): Boolean {
        if (!localFile.exists() || url.isBlank()) return false // Not downloaded or no URL
        return try {
            val response: HttpResponse = client.head(url)
            val remoteSize = response.contentLength() ?: 0L
            val localSize = localFile.length()
            // If remote size differs from local size by more than 1MB (allowing for HTTP boundary quirks), offer update
            if (remoteSize > 0 && Math.abs(remoteSize - localSize) > 1024 * 1024) {
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ModelDownloader", "Failed to check for updates: ${e.message}")
            false
        }
    }
}
