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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ApiKey(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val key: String,
    val createdAt: Long = System.currentTimeMillis()
)

object ApiKeyManager {
    private val _keys = MutableStateFlow<List<ApiKey>>(emptyList())
    val keys: StateFlow<List<ApiKey>> = _keys.asStateFlow()

    private val _activeKey = MutableStateFlow<ApiKey?>(null)
    val activeKey: StateFlow<ApiKey?> = _activeKey.asStateFlow()

    init {
        generateKey("Default Key")
    }

    fun generateKey(name: String): ApiKey? {
        val currentKeys = _keys.value
        if (currentKeys.size >= 10) return null

        val newKey = ApiKey(
            name = name,
            key = "sc-" + UUID.randomUUID().toString().replace("-", "")
        )
        _keys.value = currentKeys + newKey

        if (_activeKey.value == null) {
            _activeKey.value = newKey
        }
        return newKey
    }

    fun setActiveKey(keyId: String) {
        val key = _keys.value.find { it.id == keyId }
        if (key != null) {
            _activeKey.value = key
        }
    }

    fun deleteKey(keyId: String) {
        val remaining = _keys.value.filter { it.id != keyId }
        _keys.value = remaining
        if (_activeKey.value?.id == keyId) {
            _activeKey.value = remaining.firstOrNull()
        }
    }

    fun validateKey(providedKey: String): Boolean {
        // Only the SINGLE active key is allowed
        val incoming = providedKey.trim()
        val stored = _activeKey.value?.key?.trim()
        println("ApiGatewayServer: Validating API Key. Received: '$incoming', Stored: '$stored'")
        return incoming.isNotBlank() && incoming == stored
    }
}
