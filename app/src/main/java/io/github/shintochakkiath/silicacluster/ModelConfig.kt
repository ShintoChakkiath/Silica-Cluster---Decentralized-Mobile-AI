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
import org.json.JSONArray
import org.json.JSONObject

data class LLMModel(
    val name: String,
    val paramCount: String,
    val ramRequired: String,
    val useCase: String,
    val tier: String,
    val totalSize: String, // New field for UI display
    val downloadUrl: String = "" 
)

object ModelDirectory {
    private val defaultModels = listOf(
        LLMModel("Danube 3 500M", "500 Million", "~0.6 GB", "Ultra-fast chatbots, basic intent detection.", "Nano", "288 MB", "https://huggingface.co/Edge-Quant/h2o-danube3-500m-chat-Q4_K_M-GGUF/resolve/main/h2o-danube3-500m-chat-q4_k_m.gguf"),
        LLMModel("Qwen 2.5 0.5B", "500 Million", "~0.7 GB", "Best-in-class tiny model for logic/multilingual.", "Nano", "351 MB", "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"),
        LLMModel("Llama 3.2 1B", "1 Billion", "~1.1 GB", "The \"Standard\" for budget phones. Fast & reliable.", "Lite", "638 MB", "https://huggingface.co/hugging-quants/Llama-3.2-1B-Instruct-Q4_K_M-GGUF/resolve/main/llama-3.2-1b-instruct-q4_k_m.gguf"),
        LLMModel("Gemma 2 2B", "2.6 Billion", "~1.9 GB", "High-quality creative writing and prose.", "Efficient", "1.6 GB", "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"),
        LLMModel("Llama 3.2 3B", "3.2 Billion", "~2.4 GB", "Recommended. Best intelligence-to-RAM ratio.", "Standard", "2.0 GB", "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"),
        LLMModel("Phi-3.5 Mini", "3.8 Billion", "~2.6 GB", "Reasoning, math, and logic-heavy tasks.", "Standard", "2.2 GB", "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf"),
        LLMModel("Qwen 2.5 Coder 3B", "3 Billion", "~2.3 GB", "Python scripts, debugging, and code generation.", "Specialist", "1.9 GB", "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/main/qwen2.5-coder-3b-instruct-q4_k_m.gguf"),
        LLMModel("Llama 3.1 8B", "8 Billion", "~5.1 GB", "Advanced reasoning. (Needs 8GB+ phone).", "Power", "5.1 GB", "https://huggingface.co/bartowski/Meta-Llama-3.1-8B-Instruct-GGUF/resolve/main/Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf"),
        LLMModel("Mistral Nemo 12B", "12 Billion", "~8.2 GB", "Near-human intelligence. (Cluster Only).", "Elite", "7.7 GB", "https://huggingface.co/bartowski/Mistral-Nemo-Instruct-2407-GGUF/resolve/main/Mistral-Nemo-Instruct-2407-Q4_K_M.gguf")
    )

    fun getModels(context: Context): List<LLMModel> {
        val prefs = context.getSharedPreferences("silica_custom_models", Context.MODE_PRIVATE)
        val customJsonStr = prefs.getString("custom_models", "[]") ?: "[]"
        val customModels = mutableListOf<LLMModel>()
        try {
            val jsonArray = JSONArray(customJsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                customModels.add(
                    LLMModel(
                        name = obj.getString("name"),
                        paramCount = obj.optString("paramCount", "Custom"),
                        ramRequired = obj.optString("ramRequired", "? GB"),
                        useCase = obj.optString("useCase", "Custom HuggingFace GGUF Link."),
                        tier = obj.optString("tier", "Custom"),
                        totalSize = obj.optString("totalSize", "Variable"),
                        downloadUrl = obj.optString("downloadUrl", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return customModels + defaultModels
    }

    fun addCustomModel(context: Context, model: LLMModel) {
        val prefs = context.getSharedPreferences("silica_custom_models", Context.MODE_PRIVATE)
        val customJsonStr = prefs.getString("custom_models", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(customJsonStr)
            val newObj = JSONObject().apply {
                put("name", model.name)
                put("paramCount", model.paramCount)
                put("ramRequired", model.ramRequired)
                put("useCase", model.useCase)
                put("tier", model.tier)
                put("totalSize", model.totalSize)
                put("downloadUrl", model.downloadUrl)
            }
            jsonArray.put(newObj)
            prefs.edit().putString("custom_models", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class InternetBridge(val displayName: String, val requiresToken: Boolean) {
    Cloudflare_Free("Cloudflare (Free)", false),
    Cloudflare_Token("Cloudflare (Token Required)", true),
    Ngrok("Ngrok (Auth Token Required)", true),
    Tailscale("Tailscale (Auth Key Required)", true)
}
