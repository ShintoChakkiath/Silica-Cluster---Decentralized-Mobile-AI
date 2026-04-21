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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ChatClient {
    suspend fun sendMessage(
        hostUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        timeoutSec: Int = 300
    ): ChatMessage? = withContext(Dispatchers.IO) {
        try {
            if (hostUrl.isBlank()) return@withContext ChatMessage("assistant", "No LLM Server Initiated, please configure settings.")
            if (hostUrl == "Initializing Tunnel...") return@withContext ChatMessage("assistant", "The networking tunnel is currently initializing, please wait a moment.")
            
            var cleanHost = hostUrl.trim().replace("\n", "").replace("\r", "")
            if (!cleanHost.startsWith("http://") && !cleanHost.startsWith("https://")) {
                cleanHost = "http://$cleanHost"
            }
            
            // Normalize URL to point to completions endpoint
            val normalizedHost = if (cleanHost.endsWith("/v1/chat/completions")) {
                cleanHost
            } else if (cleanHost.endsWith("/")) {
                "${cleanHost}v1/chat/completions"
            } else {
                "$cleanHost/v1/chat/completions"
            }

            val url = URL(normalizedHost)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            connection.doOutput = true
            connection.connectTimeout = 30000 // 30s
            connection.readTimeout = timeoutSec * 1000 // configurable inference wait

            // Build request payload
            val payload = JSONObject()
            payload.put("model", model.ifBlank { "silica-network" })
            payload.put("stream", false)
            
            val validMessages = messages.filter {
                !it.content.startsWith("API Error", ignoreCase = true) && 
                !it.content.startsWith("Network Exception", ignoreCase = true) && 
                !it.content.startsWith("Error:", ignoreCase = true)
            }

            // Merge consecutive roles to comply with strict Jinja chat templates
            val mergedMessages = mutableListOf<ChatMessage>()
            for (msg in validMessages) {
                if (mergedMessages.isNotEmpty() && mergedMessages.last().role == msg.role) {
                    val last = mergedMessages.removeLast()
                    val combinedContent = last.content + "\n\n" + msg.content
                    mergedMessages.add(ChatMessage(msg.role, combinedContent, msg.timestamp))
                } else {
                    mergedMessages.add(msg)
                }
            }
            
            // Ensure the first message is not an assistant response
            while(mergedMessages.isNotEmpty() && mergedMessages.first().role == "assistant") {
                mergedMessages.removeAt(0)
            }

            val messagesArray = JSONArray()
            mergedMessages.forEach { msg ->
                val obj = JSONObject()
                obj.put("role", msg.role)
                obj.put("content", msg.content)
                messagesArray.put(obj)
            }
            payload.put("messages", messagesArray)

            val outBytes = payload.toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(outBytes.size)
            connection.outputStream.write(outBytes)
            connection.outputStream.flush()
            connection.outputStream.close()

            // Handle Response
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseString)
                val choices = responseJson.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val replyMessage = firstChoice.getJSONObject("message")
                    val replyContent = replyMessage.getString("content")
                    return@withContext ChatMessage("assistant", replyContent)
                }
            } else {
                val errorString = connection.errorStream?.bufferedReader()?.use { it.readText() }
                return@withContext ChatMessage("assistant", "API Error ($responseCode): $errorString")
            }
        } catch (e: java.net.UnknownHostException) {
            return@withContext ChatMessage("assistant", "LLM Server Not Found. Ensure the engine is running or verify your API Host link.")
        } catch (e: java.net.ConnectException) {
            return@withContext ChatMessage("assistant", "Connection Refused. The engine may still be booting up, please wait a moment.")
        } catch (e: Exception) {
            return@withContext ChatMessage("assistant", "Network Exception: ${e.message}")
        }
        return@withContext null
    }
}
