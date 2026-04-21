package io.github.shintochakkiath.silicacluster

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ContextPrompt(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val keywords: String,
    val prompt: String
)

object ContextProgrammingManager {
    private const val FILE_NAME = "silica_context_prompts.json"

    private val _prompts = MutableStateFlow<List<ContextPrompt>>(emptyList())
    val prompts: StateFlow<List<ContextPrompt>> = _prompts

    private val defaultPrompts = listOf(
        ContextPrompt(
            id = "default_news",
            title = "News & Events",
            keywords = "news,event,war,breaking",
            prompt = "Respond strictly in the following format:\n- A concise summary paragraph of the situation.\n- Bullet points summarizing the key facts from each source.\n- A conclusion or ending paragraph based on the context."
        ),
        ContextPrompt(
            id = "default_weather",
            title = "Weather & Climate",
            keywords = "weather,temperature,climate,forecast",
            prompt = "Provide a detailed, natural-sounding weather report. Include current temperature, condition, humidity, and any notable observations."
        ),
        ContextPrompt(
            id = "default_general",
            title = "General Search",
            keywords = "",
            prompt = "Answer accurately and provide a highly detailed, comprehensive response using the provided data. Structure your response cleanly with headings or bullet points if necessary. Do not provide a short summary."
        )
    )

    fun initialize(context: Context) {
        val loaded = loadFromFile(context)
        if (loaded.isEmpty()) {
            _prompts.value = defaultPrompts
            saveToFileAsync(context)
        } else {
            val updatedLoaded = loaded.map { prompt ->
                if (prompt.id == "default_news" && prompt.keywords.contains("today")) {
                    prompt.copy(keywords = "news,event,war,breaking")
                } else {
                    prompt
                }
            }
            val currentIds = updatedLoaded.map { it.id }.toSet()
            val toAdd = defaultPrompts.filter { it.id !in currentIds }
            _prompts.value = updatedLoaded + toAdd
            saveToFileAsync(context)
        }
    }

    fun addOrUpdatePrompt(context: Context, id: String?, title: String, keywords: String, prompt: String) {
        val current = _prompts.value.toMutableList()
        if (id != null) {
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = current[index].copy(title = title, keywords = keywords, prompt = prompt)
            } else {
                current.add(ContextPrompt(title = title, keywords = keywords, prompt = prompt))
            }
        } else {
            current.add(ContextPrompt(title = title, keywords = keywords, prompt = prompt))
        }
        _prompts.value = current.toList()
        saveToFileAsync(context)
    }

    fun removePrompt(context: Context, id: String) {
        val current = _prompts.value.toMutableList()
        current.removeAll { it.id == id }
        _prompts.value = current.toList()
        saveToFileAsync(context)
    }

    fun getPromptForQuery(query: String): String {
        val activePrompts = _prompts.value
        for (p in activePrompts) {
            if (p.keywords.isBlank()) continue
            val keywordsList = p.keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (keywordsList.any { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
                return p.prompt
            }
        }
        // Fallback to default_general
        return activePrompts.find { it.keywords.isBlank() }?.prompt ?: "Answer accurately using the provided data."
    }

    private fun saveToFileAsync(context: Context) {
        Thread {
            try {
                val file = File(context.filesDir, FILE_NAME)
                val rootArray = JSONArray()
                _prompts.value.forEach { p ->
                    val obj = JSONObject()
                    obj.put("id", p.id)
                    obj.put("title", p.title)
                    obj.put("keywords", p.keywords)
                    obj.put("prompt", p.prompt)
                    rootArray.put(obj)
                }
                file.writeText(rootArray.toString())
            } catch (e: Exception) {
                Log.e("ContextProgManager", "Failed to save: ${e.message}")
            }
        }.start()
    }

    private fun loadFromFile(context: Context): List<ContextPrompt> {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()
            val jsonString = file.readText()
            val rootArray = JSONArray(jsonString)
            val result = mutableListOf<ContextPrompt>()
            for (i in 0 until rootArray.length()) {
                val obj = rootArray.getJSONObject(i)
                result.add(ContextPrompt(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    keywords = obj.getString("keywords"),
                    prompt = obj.getString("prompt")
                ))
            }
            return result
        } catch (e: Exception) {
            Log.e("ContextProgManager", "Failed to load: ${e.message}")
            return emptyList()
        }
    }
}
