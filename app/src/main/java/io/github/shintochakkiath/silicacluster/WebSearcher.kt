package io.github.shintochakkiath.silicacluster

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder

data class SearchResult(val title: String, val snippet: String, val url: String)

object WebSearcher {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 6000 // Reduced timeout to prevent long hangs
        }
    }
    
    suspend fun getRealTimeContext(query: String, onStatusUpdate: (String) -> Unit = {}): Pair<String, List<String>> = coroutineScope {
        val links = java.util.concurrent.CopyOnWriteArrayList<String>()
        
        // 1. Start Location Fetch Concurrently
        val locationDeferred = async(Dispatchers.IO) {
            var city = "Unknown"
            var country = "Unknown"
            try {
                val ipData = client.get("https://ipinfo.io/json").bodyAsText()
                val json = org.json.JSONObject(ipData)
                city = json.optString("city", "Unknown")
                country = json.optString("country", "Unknown")
            } catch (e: Exception) {}
            Pair(city, country)
        }

        val isWeather = query.contains("weather", ignoreCase = true) || query.contains("temperature", ignoreCase = true)
        val isNews = query.contains("news", ignoreCase = true) || query.contains("latest", ignoreCase = true) || query.contains("today", ignoreCase = true)

        var searchResultsText = ""

        // 2. Process Intent
        if (isWeather) {
            val (city, _) = locationDeferred.await()
            val weatherSource = RssManager.sources.value.find { it.url.contains("wttr.in") }
            if (weatherSource != null) {
                onStatusUpdate("Looking up ${weatherSource.name}...")
                try {
                    val weatherUrl = weatherSource.url.replace("<query>", if (city != "Unknown" && city.isNotBlank()) URLEncoder.encode(city, "UTF-8") else "")
                    val weatherData = client.get(weatherUrl).bodyAsText()
                    val wJson = org.json.JSONObject(weatherData)
                    val current = wJson.optJSONArray("current_condition")?.optJSONObject(0)
                    if (current != null) {
                        val temp = current.optString("temp_C")
                        val desc = current.optJSONArray("weatherDesc")?.optJSONObject(0)?.optString("value")
                        val humidity = current.optString("humidity")
                        onStatusUpdate("Processing Information...")
                        searchResultsText = "REAL-TIME WEATHER DATA FOR $city: $temp °C, Condition: $desc, Humidity: $humidity%.\n"
                        links.add("https://wttr.in/${URLEncoder.encode(city, "UTF-8")}")
                    } else {
                        onStatusUpdate("Did not find any related data.")
                    }
                } catch (e: Exception) {
                    onStatusUpdate("Did not find any related data.")
                }
            }
        } else {
            // Search in parallel to location
            val searchDeferred = async(Dispatchers.IO) {
                var text = ""
                
                if (isNews) {
                    try {
                        val newsResults = rssSearch(query, onStatusUpdate)
                        if (newsResults.isNotEmpty()) {
                            onStatusUpdate("Processing Information...")
                            newsResults.forEach { links.add(it.url) }
                            val snippets = newsResults.mapIndexed { index, it -> "Source ${index + 1}: ${it.title}\nLink: ${it.url}\nSummary: ${it.snippet}" }.joinToString("\n\n")
                            text = "LATEST LIVE NEWS & INFORMATION:\n$snippets"
                        }
                    } catch(e: Exception) {}
                }
                
                if (text.isEmpty()) {
                    try {
                        val ddgSource = RssManager.sources.value.find { it.url.contains("duckduckgo.com") }
                        if (ddgSource != null) onStatusUpdate("Looking up ${ddgSource.name}...")
                        
                        val ddgResults = search(query).take(4)
                        if (ddgResults.isNotEmpty()) {
                            onStatusUpdate("Processing Information...")
                            ddgResults.forEach { links.add(it.url) }
                            val snippets = ddgResults.mapIndexed { index, it -> "Source ${index + 1}: ${it.title}\nURL: ${it.url}\nInformation: ${it.snippet}" }.joinToString("\n\n")
                            text = "REAL-TIME WEB SEARCH RESULTS:\n$snippets"
                        }
                    } catch(e: Exception) {}
                }

                if (text.isEmpty() && !isNews) {
                    try {
                        val newsResults = rssSearch(query, onStatusUpdate).take(5)
                        if (newsResults.isNotEmpty()) {
                            onStatusUpdate("Processing Information...")
                            newsResults.forEach { links.add(it.url) }
                            val snippets = newsResults.mapIndexed { index, it -> "Source ${index + 1}: ${it.title}\nLink: ${it.url}\nSummary: ${it.snippet}" }.joinToString("\n\n")
                            text = "LATEST LIVE NEWS & INFORMATION:\n$snippets"
                        }
                    } catch(e: Exception) {}
                }
                
                if (text.isEmpty()) {
                    val wikiSource = RssManager.sources.value.find { it.url.contains("wikipedia.org") }
                    if (wikiSource != null) {
                        onStatusUpdate("Looking up ${wikiSource.name}...")
                        try {
                            val wikiUrl = wikiSource.url.replace("<query>", URLEncoder.encode(query, "UTF-8"))
                            val wikiData = client.get(wikiUrl).bodyAsText()
                            val wikiJson = org.json.JSONObject(wikiData)
                            val searchArray = wikiJson.optJSONObject("query")?.optJSONArray("search")
                            if (searchArray != null && searchArray.length() > 0) {
                                onStatusUpdate("Processing Information...")
                                val firstTitle = searchArray.getJSONObject(0).optString("title")
                                val firstSnippet = searchArray.getJSONObject(0).optString("snippet").replace(Regex("<[^>]*>"), "")
                                text = "WIKIPEDIA SEARCH RESULT:\nTitle: $firstTitle\nSnippet: ${firstSnippet}"
                                links.add("https://en.wikipedia.org/wiki/${URLEncoder.encode(firstTitle.replace(" ", "_"), "UTF-8")}")
                            } else {
                                onStatusUpdate("Did not find any related data.")
                            }
                        } catch(e: Exception) {
                            onStatusUpdate("Did not find any related data.")
                        }
                    }
                }
                text
            }
            searchResultsText = searchDeferred.await()
        }

        val (city, country) = locationDeferred.await()
        val contextText = "CURRENT LOCATION OF USER: City: $city, Country: $country. Current Time: ${java.util.Date()}\n$searchResultsText"
        
        Pair(contextText, links.toList())
    }

    suspend fun rssSearch(query: String, onStatusUpdate: (String) -> Unit = {}): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val sources = RssManager.sources.value
            if (sources.isEmpty()) return@withContext emptyList()
            
            for (source in sources) {
                if (source.url.contains("duckduckgo.com") || source.url.contains("wikipedia.org") || source.url.contains("wttr.in")) continue
                
                onStatusUpdate("Looking up ${source.name}...")
                try {
                    val url = source.url.replace("<query>", encodedQuery)
                    val xml = client.get(url).bodyAsText()
                    
                    val itemRegex = Regex("""<item>(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                    val items = itemRegex.findAll(xml)
                    
                    var sourceAddedCount = 0
                    for (item in items) {
                        val blockText = item.groupValues[1]
                        val title = Regex("""<title>(.*?)</title>""").find(blockText)?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
                        var link = Regex("""<link>(.*?)</link>""").find(blockText)?.groupValues?.get(1)?.trim() ?: ""
                        val desc = Regex("""<description>(.*?)</description>""", RegexOption.DOT_MATCHES_ALL).find(blockText)?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.replace("&nbsp;", " ")?.trim() ?: ""
                        
                        // Extract actual source domain for Google News feeds
                        val sourceUrlMatch = Regex("""<source url="([^"]+)"""").find(blockText)
                        if (sourceUrlMatch != null && link.contains("news.google.com")) {
                            try {
                                val sourceHost = java.net.URI(sourceUrlMatch.groupValues[1]).host?.removePrefix("www.")
                                if (sourceHost != null) {
                                    link = "$link#silica_domain=$sourceHost"
                                }
                            } catch (e: Exception) {}
                        }
                        
                        if (title.isNotBlank()) {
                            results.add(SearchResult("[${source.name}] $title", desc, link))
                            sourceAddedCount++
                        }
                        if (sourceAddedCount >= 5) break // Limit to top 5 results per source
                    }
                    
                    if (sourceAddedCount == 0) {
                        onStatusUpdate("Did not find any related data. Trying another source...")
                    } else {
                        onStatusUpdate("Processing Information...")
                    }
                } catch (e: Exception) {
                    onStatusUpdate("Did not find any related data. Trying another source...")
                    e.printStackTrace() // Skip failed sources and continue
                }
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val ddgSource = RssManager.sources.value.find { it.url.contains("duckduckgo.com") }
        if (ddgSource == null) return@withContext emptyList()
        
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = ddgSource.url.substringBefore("?") + "?q=$encodedQuery"
            
            val html = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
            }.bodyAsText()

            val results = mutableListOf<SearchResult>()
            
            val blockRegex = Regex("""<div class="[^"]*result[^"]*"(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val titleRegex = Regex("""<h2 class="result__title">.*?<a[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val snippetRegex = Regex("""<a class="result__snippet[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val urlRegex = Regex("""<a class="result__url" href="([^"]+)"""")

            val blocks = blockRegex.findAll(html)
            for (block in blocks) {
                val blockText = block.value
                val titleMatch = titleRegex.find(blockText)
                val snippetMatch = snippetRegex.find(blockText)
                val urlMatch = urlRegex.find(blockText)

                if (titleMatch != null && snippetMatch != null && urlMatch != null) {
                    val rawTitle = titleMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                    val rawSnippet = snippetMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                    var rawUrl = urlMatch.groupValues[1]
                    if (rawUrl.startsWith("//duckduckgo.com/l/?uddg=")) {
                        rawUrl = rawUrl.substringAfter("uddg=").substringBefore("&amp;")
                        rawUrl = java.net.URLDecoder.decode(rawUrl, "UTF-8")
                    } else if (!rawUrl.startsWith("http")) {
                        rawUrl = "https://$rawUrl"
                    }
                    
                    if (rawTitle.isNotBlank() && rawSnippet.isNotBlank()) {
                        results.add(SearchResult(rawTitle, rawSnippet, rawUrl))
                    }
                    if (results.size >= 4) break
                }
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
