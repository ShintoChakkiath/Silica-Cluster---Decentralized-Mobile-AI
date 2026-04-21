package io.github.shintochakkiath.silicacluster

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class RssSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String
)

object RssManager {
    private const val FILE_NAME = "silica_rss_sources.json"

    private val _sources = MutableStateFlow<List<RssSource>>(emptyList())
    val sources: StateFlow<List<RssSource>> = _sources

    private val defaultSources = listOf(
        RssSource(
            id = "default_google_news",
            name = "Google News Search",
            url = "https://news.google.com/rss/search?q=<query>&hl=en-US&gl=US&ceid=US:en"
        ),
        RssSource(
            id = "default_weather",
            name = "Live Weather API",
            url = "https://wttr.in/<query>?format=j1"
        ),
        RssSource(
            id = "default_wikipedia",
            name = "Wikipedia Search",
            url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=<query>&utf8=&format=json"
        ),
        RssSource(
            id = "default_duckduckgo",
            name = "DuckDuckGo Web Search",
            url = "https://html.duckduckgo.com/html/?q=<query>"
        )
    )

    fun initialize(context: Context) {
        val loaded = loadFromFile(context)
        val prefs = context.getSharedPreferences("silica_rss_prefs", Context.MODE_PRIVATE)
        val migratedDefaults = prefs.getBoolean("migrated_defaults_v2", false)
        
        if (loaded.isEmpty()) {
            _sources.value = defaultSources
            prefs.edit().putBoolean("migrated_defaults_v2", true).apply()
            saveToFileAsync(context)
        } else if (!migratedDefaults) {
            val currentIds = loaded.map { it.id }.toSet()
            val toAdd = defaultSources.filter { it.id !in currentIds }
            _sources.value = loaded + toAdd
            prefs.edit().putBoolean("migrated_defaults_v2", true).apply()
            saveToFileAsync(context)
        } else {
            _sources.value = loaded
        }
    }

    fun addSource(context: Context, name: String, url: String) {
        val current = _sources.value.toMutableList()
        current.add(RssSource(name = name, url = url))
        _sources.value = current.toList()
        saveToFileAsync(context)
    }

    fun removeSource(context: Context, id: String) {
        val current = _sources.value.toMutableList()
        current.removeAll { it.id == id }
        _sources.value = current.toList()
        saveToFileAsync(context)
    }

    fun moveSourceUp(context: Context, id: String) {
        val current = _sources.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index > 0) {
            val temp = current[index - 1]
            current[index - 1] = current[index]
            current[index] = temp
            _sources.value = current.toList()
            saveToFileAsync(context)
        }
    }

    fun moveSourceDown(context: Context, id: String) {
        val current = _sources.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0 && index < current.size - 1) {
            val temp = current[index + 1]
            current[index + 1] = current[index]
            current[index] = temp
            _sources.value = current.toList()
            saveToFileAsync(context)
        }
    }

    private fun saveToFileAsync(context: Context) {
        Thread {
            try {
                val file = File(context.filesDir, FILE_NAME)
                val rootArray = JSONArray()
                
                _sources.value.forEach { source ->
                    val obj = JSONObject()
                    obj.put("id", source.id)
                    obj.put("name", source.name)
                    obj.put("url", source.url)
                    rootArray.put(obj)
                }
                
                file.writeText(rootArray.toString())
            } catch (e: Exception) {
                Log.e("RssManager", "Failed to save RSS sources: ${e.message}")
            }
        }.start()
    }

    private fun loadFromFile(context: Context): List<RssSource> {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()
            
            val jsonString = file.readText()
            val rootArray = JSONArray(jsonString)
            val result = mutableListOf<RssSource>()
            
            for (i in 0 until rootArray.length()) {
                val obj = rootArray.getJSONObject(i)
                result.add(
                    RssSource(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        url = obj.getString("url")
                    )
                )
            }
            return result
        } catch (e: Exception) {
            Log.e("RssManager", "Failed to load RSS sources: ${e.message}")
            return emptyList()
        }
    }
}
