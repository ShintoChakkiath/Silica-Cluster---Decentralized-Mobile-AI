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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HackerDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = CyberPurple,
    tertiary = MatrixGreen,
    background = ObsidianBg,
    surface = ObsidianSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = AlertRed,
    outline = Color.White,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary
)

private val HackerLightColorScheme = lightColorScheme(
    primary = SoftNeonCyan,
    secondary = SoftCyberPurple,
    tertiary = SoftMatrixGreen,
    background = ObsidianLight,
    surface = ObsidianLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    error = SoftAlertRed,
    outline = Color.Black,
    surfaceVariant = ObsidianLightCard,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun SilicaClusterTheme(
    darkTheme: Boolean = false, // Light mode by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HackerDarkColorScheme else HackerLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}