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
            }
        }
        server?.start(wait = false)
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
}
