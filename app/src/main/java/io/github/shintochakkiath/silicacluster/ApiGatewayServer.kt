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

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiGatewayServer {
    private var server: ApplicationEngine? = null
    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 0 // Disable engine-level timeout for slow RPC streaming
        }
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000L // 10 minutes timeout for heavy distributed distribution
            connectTimeoutMillis = 30_000L
            socketTimeoutMillis = 600_000L
        }
    }

    private var targetLlamaUrl = "http://127.0.0.1:8080"

    fun startServer(port: Int = 8081, upstreamIp: String? = null) {
        if (upstreamIp != null) targetLlamaUrl = "http://$upstreamIp:8080"
        if (server != null) return

        server = embeddedServer(io.ktor.server.cio.CIO, port = port, host = "0.0.0.0") {
            routing {
                route("/{...}") {
                    handle {
                        val authHeader = call.request.header("Authorization")
                        val token = authHeader?.replace("Bearer ", "")?.trim() ?: ""

                        println("ApiGatewayServer: Received auth header '$authHeader', parsed token: '$token'")

                        if (!ApiKeyManager.validateKey(token)) {
                            println("ApiGatewayServer: Authorization failed for token: '$token'")
                            call.respond(HttpStatusCode.Unauthorized, "Invalid or missing API Key")
                            return@handle
                        }

                        val targetUrl = "$targetLlamaUrl${call.request.uri}"
                        proxyRequest(call, targetUrl)
                    }
                }
            }
        }.start(wait = false)
    }

    private suspend fun proxyRequest(call: ApplicationCall, targetUrl: String) {
        val maxRetries = 15
        val isRetriable = call.request.httpMethod == HttpMethod.Get || call.request.httpMethod == HttpMethod.Head

        for (attempt in 1..maxRetries) {
            try {
                val bodyBytes = if (!isRetriable) {
                    try {
                        val bytes = call.receive<ByteArray>()
                        val text = String(bytes, Charsets.UTF_8)
                        if (text.isNotBlank()) ActivityLogger.logUserRequest(text)
                        bytes
                    } catch(e: Exception) { ByteArray(0) }
                } else ByteArray(0)

                val proxyResponse = httpClient.request(targetUrl) {
                    method = call.request.httpMethod
                    headers {
                        // Forward essential headers
                        call.request.headers.forEach { key, values ->
                            if (!key.equals("Host", ignoreCase = true) &&
                                !key.equals("Content-Length", ignoreCase = true) &&
                                !key.equals("Authorization", ignoreCase = true)) {
                                values.forEach { append(key, it) }
                            }
                        }
                    }
                    
                    if (!isRetriable && bodyBytes.isNotEmpty()) {
                        setBody(bodyBytes)
                    }
                }

                // Copy response back to the client
                call.respondBytesWriter(contentType = proxyResponse.contentType(), status = proxyResponse.status) {
                    val src = proxyResponse.bodyAsChannel()
                    ActivityLogger.startAiResponse()
                    
                    val buffer = java.nio.ByteBuffer.allocate(8192)
                    while(!src.isClosedForRead) {
                        val read = src.readAvailable(buffer)
                        if (read > 0) {
                            buffer.flip()
                            
                            val chunkBytes = ByteArray(buffer.remaining())
                            buffer.get(chunkBytes)
                            buffer.rewind()
                            
                            this.writeFully(buffer)
                            buffer.clear()
                            
                            val chunkText = String(chunkBytes)
                            var extractedText = ""
                            
                            val lines = chunkText.split("\n")
                            for (line in lines) {
                                if (line.startsWith("data: ") && line.length > 10) {
                                    try {
                                        val json = org.json.JSONObject(line.substring(6))
                                        val choices = json.optJSONArray("choices")
                                        if (choices != null && choices.length() > 0) {
                                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                                            if (delta != null && delta.has("content")) {
                                                extractedText += delta.getString("content")
                                            }
                                        }
                                    } catch(e: Exception) {}
                                }
                            }
                            
                            if (extractedText.isEmpty() && chunkText.trim().startsWith("{")) {
                                try {
                                    val json = org.json.JSONObject(chunkText)
                                    val choices = json.optJSONArray("choices")
                                    if (choices != null && choices.length() > 0) {
                                        val msg = choices.getJSONObject(0).optJSONObject("message")
                                        if (msg != null && msg.has("content")) {
                                            extractedText += msg.getString("content")
                                        }
                                    }
                                } catch(e: Exception) {}
                            }
                            
                            if (extractedText.isNotEmpty()) {
                                ActivityLogger.appendAiChunk(extractedText)
                            }
                        }
                    }
                    ActivityLogger.finishAiResponse()
                }
                return // Success exiting loop
            } catch (e: Exception) {
                if (e.message?.contains("Connection refused") == true && attempt < maxRetries && isRetriable) {
                    println("ApiGatewayServer: Connection refused. Model loading? Retrying ($attempt/$maxRetries)...")
                    kotlinx.coroutines.delay(2000)
                    continue
                }
                withContext(Dispatchers.IO) {
                    call.respond(HttpStatusCode.BadGateway, "Upstream LLM server error: ${e.message}")
                }
                return
            }
        }
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
}
