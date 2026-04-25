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

import java.io.File
import java.io.RandomAccessFile

object GgufMetadataParser {
    fun getTotalLayers(file: File): Int? {
        if (!file.exists()) return null
        
        try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                if (String(magic) != "GGUF") return null
                
                val version = readInt32(raf)
                val tensorCount = readInt64(raf)
                val metadataKvCount = readInt64(raf)
                
                for (i in 0 until metadataKvCount) {
                    val keyLength = readInt64(raf)
                    if (keyLength > 10000 || keyLength < 0) return null // Sanity check
                    val keyBytes = ByteArray(keyLength.toInt())
                    raf.readFully(keyBytes)
                    val key = String(keyBytes, Charsets.UTF_8)
                    
                    val valueType = readInt32(raf)
                    
                    if (key.endsWith(".block_count")) {
                        // Found it!
                        return when (valueType) {
                            4 -> readInt32(raf) // UINT32
                            5 -> readInt32(raf) // INT32
                            10 -> readInt64(raf).toInt() // UINT64
                            11 -> readInt64(raf).toInt() // INT64
                            else -> null
                        }
                    } else {
                        skipValue(raf, valueType)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return null
    }

    private fun readInt32(raf: RandomAccessFile): Int {
        val b = ByteArray(4)
        raf.readFully(b)
        return (b[0].toInt() and 0xFF) or
               ((b[1].toInt() and 0xFF) shl 8) or
               ((b[2].toInt() and 0xFF) shl 16) or
               ((b[3].toInt() and 0xFF) shl 24)
    }

    private fun readInt64(raf: RandomAccessFile): Long {
        val b = ByteArray(8)
        raf.readFully(b)
        return (b[0].toLong() and 0xFFL) or
               ((b[1].toLong() and 0xFFL) shl 8) or
               ((b[2].toLong() and 0xFFL) shl 16) or
               ((b[3].toLong() and 0xFFL) shl 24) or
               ((b[4].toLong() and 0xFFL) shl 32) or
               ((b[5].toLong() and 0xFFL) shl 40) or
               ((b[6].toLong() and 0xFFL) shl 48) or
               ((b[7].toLong() and 0xFFL) shl 56)
    }

    private fun skipValue(raf: RandomAccessFile, type: Int) {
        when (type) {
            0, 1, 7 -> raf.skipBytes(1) // UINT8, INT8, BOOL
            2, 3 -> raf.skipBytes(2) // UINT16, INT16
            4, 5, 6 -> raf.skipBytes(4) // UINT32, INT32, FLOAT32
            10, 11, 12 -> raf.skipBytes(8) // UINT64, INT64, FLOAT64
            8 -> { // STRING
                val len = readInt64(raf)
                raf.skipBytes(len.toInt())
            }
            9 -> { // ARRAY
                val arrayType = readInt32(raf)
                val arrayLen = readInt64(raf)
                for (i in 0 until arrayLen) {
                    skipValue(raf, arrayType)
                }
            }
            else -> throw IllegalStateException("Unknown GGUF type: $type")
        }
    }
}
