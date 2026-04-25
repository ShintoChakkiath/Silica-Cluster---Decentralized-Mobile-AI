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
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceUrls: List<String> = emptyList()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Chat",
    val messages: MutableList<ChatMessage> = mutableListOf(),
    var timestamp: Long = System.currentTimeMillis(),
    var isPinned: Boolean = false,
    val usedModels: MutableSet<String> = mutableSetOf()
)

object ChatRepository {
    private const val FILE_NAME = "silica_chats.json"
    
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId

    suspend fun initialize(context: Context) {
        val loaded = loadFromFile(context)
        val sortedList = loaded.sortedWith(compareByDescending<ChatSession> { it.isPinned }.thenByDescending { it.timestamp })
        
        // Find an existing empty unpinned chat to reuse, or create a new one
        val existingEmpty = sortedList.find { it.messages.isEmpty() && !it.isPinned }
        
        if (existingEmpty != null) {
            // Reuse the existing empty chat and bring it to the top implicitly by updating timestamp
            existingEmpty.timestamp = System.currentTimeMillis()
            _sessions.value = sortedList.sortedWith(compareByDescending<ChatSession> { it.isPinned }.thenByDescending { it.timestamp })
            _activeSessionId.value = existingEmpty.id
        } else {
            // Create a fresh new chat for this session
            val newSession = ChatSession()
            _sessions.value = listOf(newSession) + sortedList
            _activeSessionId.value = newSession.id
            saveToFileAsync(context)
        }
    }

    fun createNewChat(context: Context) {
        val newSession = ChatSession()
        val updatedList = listOf(newSession) + _sessions.value
        _sessions.value = updatedList
        _activeSessionId.value = newSession.id
        saveToFileAsync(context)
    }

    fun setActiveSession(id: String) {
        _activeSessionId.value = id
    }

    fun deleteSession(context: Context, id: String) {
        val updatedList = _sessions.value.filter { it.id != id }
        _sessions.value = updatedList
        if (_activeSessionId.value == id) {
            _activeSessionId.value = updatedList.firstOrNull()?.id
        }
        saveToFileAsync(context)
    }

    fun addMessageToActive(context: Context, message: ChatMessage, usedModel: String? = null) {
        val currentSessions = _sessions.value.toMutableList()
        val activeId = _activeSessionId.value
        
        val activeIndex = currentSessions.indexOfFirst { it.id == activeId }
        if (activeIndex != -1) {
            val session = currentSessions[activeIndex]
            val newMessages = session.messages.toMutableList()
            newMessages.add(message)
            
            val newUsedModels = session.usedModels.toMutableSet()
            if (usedModel != null) {
                newUsedModels.add(usedModel)
            }
            
            // Auto-generate title from first user message
            val newTitle = if (newMessages.count { it.role == "user" } == 1 && session.title == "New Chat") {
                message.content.take(30) + if (message.content.length > 30) "..." else ""
            } else {
                session.title
            }
            
            val newSession = session.copy(
                messages = newMessages,
                timestamp = System.currentTimeMillis(),
                title = newTitle,
                usedModels = newUsedModels
            )
            
            // Bring to top
            currentSessions.removeAt(activeIndex)
            currentSessions.add(0, newSession)
            
            _sessions.value = currentSessions.sortedWith(compareByDescending<ChatSession> { it.isPinned }.thenByDescending { it.timestamp })
            saveToFileAsync(context)
        }
    }

    fun togglePinSession(context: Context, id: String) {
        val currentSessions = _sessions.value.toMutableList()
        val index = currentSessions.indexOfFirst { it.id == id }
        if (index != -1) {
            val session = currentSessions[index]
            session.isPinned = !session.isPinned
            _sessions.value = currentSessions.sortedWith(compareByDescending<ChatSession> { it.isPinned }.thenByDescending { it.timestamp })
            saveToFileAsync(context)
        }
    }

    fun revertToMessage(context: Context, id: String, messageIndex: Int) {
        val currentSessions = _sessions.value.toMutableList()
        val sessionIndex = currentSessions.indexOfFirst { it.id == id }
        if (sessionIndex != -1) {
            val oldSession = currentSessions[sessionIndex]
            val newMessages = oldSession.messages.subList(0, messageIndex).toMutableList()
            val newSession = oldSession.copy(messages = newMessages)
            currentSessions[sessionIndex] = newSession
            _sessions.value = currentSessions.toList() // Trigger state update
            saveToFileAsync(context)
        }
    }

    private fun saveToFileAsync(context: Context) {
        Thread {
            try {
                val file = File(context.filesDir, FILE_NAME)
                val rootArray = JSONArray()
                
                _sessions.value.forEach { session ->
                    val sessionObj = JSONObject()
                    sessionObj.put("id", session.id)
                    sessionObj.put("title", session.title)
                    sessionObj.put("timestamp", session.timestamp)
                    sessionObj.put("isPinned", session.isPinned)
                    
                    val modelsArray = JSONArray()
                    session.usedModels.forEach { modelsArray.put(it) }
                    sessionObj.put("usedModels", modelsArray)
                    
                    val messagesArray = JSONArray()
                    session.messages.forEach { msg ->
                        val msgObj = JSONObject()
                        msgObj.put("role", msg.role)
                        msgObj.put("content", msg.content)
                        msgObj.put("timestamp", msg.timestamp)
                        if (msg.sourceUrls.isNotEmpty()) {
                            val urlsArr = JSONArray()
                            msg.sourceUrls.forEach { urlsArr.put(it) }
                            msgObj.put("sourceUrls", urlsArr)
                        }
                        messagesArray.put(msgObj)
                    }
                    sessionObj.put("messages", messagesArray)
                    rootArray.put(sessionObj)
                }
                
                file.writeText(rootArray.toString())
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to save chats: ${e.message}")
            }
        }.start()
    }

    private suspend fun loadFromFile(context: Context): List<ChatSession> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return@withContext emptyList()
            
            val jsonString = file.readText()
            val rootArray = JSONArray(jsonString)
            val result = mutableListOf<ChatSession>()
            
            for (i in 0 until rootArray.length()) {
                val sessionObj = rootArray.getJSONObject(i)
                val id = sessionObj.getString("id")
                val title = sessionObj.getString("title")
                val timestamp = sessionObj.getLong("timestamp")
                val isPinned = sessionObj.optBoolean("isPinned", false)
                
                val messagesArray = sessionObj.getJSONArray("messages")
                val messages = mutableListOf<ChatMessage>()
                
                for (j in 0 until messagesArray.length()) {
                    val msgObj = messagesArray.getJSONObject(j)
                    val sourceUrls = mutableListOf<String>()
                    if (msgObj.has("sourceUrls")) {
                        val urlsArr = msgObj.getJSONArray("sourceUrls")
                        for (k in 0 until urlsArr.length()) {
                            sourceUrls.add(urlsArr.getString(k))
                        }
                    } else if (msgObj.has("sourceUrl")) {
                        sourceUrls.add(msgObj.getString("sourceUrl"))
                    }
                    messages.add(
                        ChatMessage(
                            role = msgObj.getString("role"),
                            content = msgObj.getString("content"),
                            timestamp = msgObj.getLong("timestamp"),
                            sourceUrls = sourceUrls
                        )
                    )
                }
                
                val usedModels = mutableSetOf<String>()
                if (sessionObj.has("usedModels")) {
                    val modelsArr = sessionObj.getJSONArray("usedModels")
                    for (m in 0 until modelsArr.length()) {
                        usedModels.add(modelsArr.getString(m))
                    }
                }
                
                result.add(ChatSession(id, title, messages, timestamp, isPinned, usedModels))
            }
            return@withContext result
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to load chats: ${e.message}")
            emptyList()
        }
    }
}
