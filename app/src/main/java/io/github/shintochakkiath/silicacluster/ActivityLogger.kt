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
import org.json.JSONObject

data class ActivityItem(
    val timestamp: Long = System.currentTimeMillis(),
    val isUser: Boolean,
    var content: String,
    val isComplete: Boolean = true,
    val isSystem: Boolean = false
)

object ActivityLogger {
    val activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    private var currentAiItem: ActivityItem? = null

    fun logSystemMessage(message: String) {
        appendActivity(ActivityItem(isUser = false, content = message, isSystem = true))
    }

    fun logUserRequest(jsonBody: String) {
        try {
            val jsonObj = JSONObject(jsonBody)
            val messages = jsonObj.optJSONArray("messages")
            if (messages != null && messages.length() > 0) {
                for (i in messages.length() - 1 downTo 0) {
                    val msg = messages.getJSONObject(i)
                    if (msg.optString("role") == "user") {
                        appendActivity(ActivityItem(isUser = true, content = msg.optString("content")))
                        return
                    }
                }
            } else if (jsonObj.has("prompt")) {
                appendActivity(ActivityItem(isUser = true, content = jsonObj.optString("prompt")))
                return
            }
        } catch (e: Exception) {
            // Ignored
        }
        if (jsonBody.length < 500) {
            appendActivity(ActivityItem(isUser = true, content = "[System]: $jsonBody"))
        } else {
            appendActivity(ActivityItem(isUser = true, content = "[System]: Heavy payload received."))
        }
    }

    fun startAiResponse() {
        currentAiItem = ActivityItem(isUser = false, content = "", isComplete = false)
        appendActivity(currentAiItem!!)
    }

    fun appendAiChunk(chunkText: String) {
        val current = currentAiItem ?: return
        current.content += chunkText
        activities.value = activities.value.toList()
    }
    
    fun finishAiResponse() {
        currentAiItem = null
    }

    private fun appendActivity(item: ActivityItem) {
        val list = activities.value.toMutableList()
        list.add(item)
        if (list.size > 100) list.removeAt(0)
        activities.value = list
    }
}
