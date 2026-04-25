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
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import org.json.JSONObject

object TelemetryServer {
    private var server: ApplicationEngine? = null

    fun startServer(context: Context) {
        if (server != null) return
        
        server = embeddedServer(CIO, port = 8082, host = "0.0.0.0") {
            install(CORS) {
                anyHost()
                allowHeader(io.ktor.http.HttpHeaders.ContentType)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
            }
            routing {
                get("/telemetrics") {
                    val info = HardwareManager.getDeviceInfo(context)
                    val json = JSONObject().apply {
                        put("availableRamGb", info.availableRamGb)
                        put("totalRamGb", info.totalRamGb)
                        put("cpuCount", info.cpuCount)
                        put("threadCount", BridgeStateManager.threadCount.value)
                        put("batteryTempCelsius", info.batteryTempCelsius)
                        put("measuredTops", info.measuredTops)
                        put("rpcPort", BridgeStateManager.rpcPort.value)
                    }
                    call.respondText(json.toString(), io.ktor.http.ContentType.Application.Json)
                }

                post("/request-join") {
                    val body = call.receiveText()
                    val json = JSONObject(body)
                    val masterIp = json.optString("masterIp", "")
                    if (masterIp.isNotBlank()) {
                        BridgeStateManager.incomingRequestIp.value = masterIp
                        BridgeStateManager.workerStatus.value = "Verifying"
                        call.respond(HttpStatusCode.OK, "Request Received")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Missing IP")
                    }
                }

                get("/join-status") {
                    val json = JSONObject().apply {
                        put("isAuthorized", BridgeStateManager.trustedMasterIp.value != null)
                        put("masterIp", BridgeStateManager.trustedMasterIp.value ?: "")
                    }
                    call.respondText(json.toString(), io.ktor.http.ContentType.Application.Json)
                }

                get("/check-model") {
                    val modelName = call.request.queryParameters["name"] ?: ""
                    val storageDir = try { context.getExternalFilesDir(null) ?: context.filesDir } catch (e: Exception) { context.filesDir }
                    val dir = java.io.File(storageDir, "models").apply { mkdirs() }
                    val exists = java.io.File(dir, modelName).exists()
                    call.respondText(org.json.JSONObject().apply { put("exists", exists) }.toString(), io.ktor.http.ContentType.Application.Json)
                }

                post("/upload-model") {
                    val modelName = call.request.queryParameters["name"] ?: "uploaded_model.gguf"
                    val storageDir = try { context.getExternalFilesDir(null) ?: context.filesDir } catch (e: Exception) { context.filesDir }
                    val dir = java.io.File(storageDir, "models").apply { mkdirs() }
                    val destFile = java.io.File(dir, modelName)
                    try {
                        val channel = call.receiveChannel()
                        val fos = destFile.outputStream()
                        val buffer = java.nio.ByteBuffer.allocate(8192)
                        while (!channel.isClosedForRead) {
                            val read = channel.readAvailable(buffer)
                            if (read > 0) {
                                buffer.flip()
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                fos.write(bytes)
                                buffer.clear()
                            }
                        }
                        fos.close()
                        call.respond(HttpStatusCode.OK, "Uploaded")
                    } catch (e: Exception) {
                        destFile.delete()
                        call.respond(HttpStatusCode.InternalServerError, "Failed to upload")
                    }
                }

                post("/start-full-engine") {
                    val body = call.receiveText()
                    val json = JSONObject(body)
                    val modelName = json.optString("modelName", "")
                    val rpcNodes = json.optString("rpcNodes", "")
                    val models = ModelDirectory.getModels(context)
                    val targetModel = models.find { it.name.replace(" ", "_").lowercase() + ".gguf" == modelName || it.name == modelName }
                    if (targetModel != null) {
                        val storageDir = try { context.getExternalFilesDir(null) ?: context.filesDir } catch (e: Exception) { context.filesDir }
                        val dir = java.io.File(storageDir, "models").apply { mkdirs() }
                        val modelPath = java.io.File(dir, modelName).absolutePath
                        val intent = android.content.Intent(context, SilicaService::class.java).apply {
                            action = "START"
                            putExtra("MODEL_PATH", modelPath)
                            putExtra("BRIDGE", "Cloudflare_Free")
                            putExtra("IS_WORKER", false) // run standard libllama.so
                            putExtra("IS_OFFLINE", true) // skip cloudflare tunnel on worker
                            putExtra("THREAD_COUNT", BridgeStateManager.threadCount.value)
                            putExtra("EXTRA_RPC_NODES", rpcNodes)
                        }
                        context.startService(intent)
                        call.respond(HttpStatusCode.OK, "Engine starting on worker")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Model not found")
                    }
                }
            }
        }
        server?.start(wait = false)
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
}
