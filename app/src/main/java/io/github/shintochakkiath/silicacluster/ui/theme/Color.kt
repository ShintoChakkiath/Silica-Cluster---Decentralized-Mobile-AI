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
package io.github.shintochakkiath.silicacluster.ui.theme

import androidx.compose.ui.graphics.Color

// Silica Cluster Branding
val SilicaNavy = Color(0xFF04162E)       // Deep navy from logo
val SilicaNavyDark = Color(0xFF020B17)   // Even darker for background
val SilicaNavySurface = Color(0xFF0A1F3D) // Lighter navy for surfaces
val SilicaLightBlue = Color(0xFF89CFF0)  // Light blue from logo nodes
val SilicaWhite = Color(0xFFF0F8FF)     // Off-white from logo nodes

// Obsidian Base (Keeping for logic but updated to brand feel)
val ObsidianBg = SilicaNavyDark         // Using navy dark instead of pure black
val ObsidianSurface = SilicaNavySurface
val ObsidianCard = Color(0xFF0E284D)

// Neons & Accents -> Mapped to Brand Light Blue and White
val NeonCyan = SilicaLightBlue
val CyberPurple = Color(0xFF9EA6F0)      // Soft purple/blue blend
val MatrixGreen = Color(0xFF6DE8C3)      // Teal variant for status
val AlertRed = Color(0xFFFF5E5E)         // Soft alert red
val SoftAlertRed = Color(0xFFFF8585)

// Soft Accents
val SoftNeonCyan = SilicaLightBlue.copy(alpha = 0.7f)
val SoftMatrixGreen = Color(0xFF6DE8C3).copy(alpha = 0.7f)
val SoftCyberPurple = Color(0xFF9EA6F0).copy(alpha = 0.7f)

// Text
val TextPrimary = SilicaWhite
val TextSecondary = Color(0xFFB0C4DE)    // Light Steel Blue for secondary text
val TextMuted = Color(0xFF708090)        // Slate Gray for muted text

// Light Mode variants (keeping if needed)
val LightTextPrimary = Color(0xFF04162E)
val LightTextSecondary = Color(0xFF334B6B)
val LightTextMuted = Color(0xFF5A7596)

// Legacy / Support
val ObsidianLight = Color(0xFFF8FAFC)
val ObsidianLightSurface = Color(0xFFF1F5F9)
val ObsidianLightCard = Color(0xFFE2E8F0)

val Purple80 = SilicaLightBlue
val PurpleGrey80 = TextSecondary
val Pink80 = SilicaWhite