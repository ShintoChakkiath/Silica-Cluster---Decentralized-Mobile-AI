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

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.shintochakkiath.silicacluster.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Link

// ----------------------------------------------------------------------------
// HIGH-END UI COMPONENTS (HACKER STYLE)
// ----------------------------------------------------------------------------

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    val bColor = if (borderColor == Color.Unspecified) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else borderColor
    Card(
        modifier = modifier
            .border(1.dp, bColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun HackerButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val cColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.primary else containerColor
    val conColor = if (contentColor != Color.Unspecified) contentColor else if (cColor == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .border(1.dp, cColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = cColor,
            contentColor = conColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
    }
}

class MainActivity : ComponentActivity() {
    override fun onDestroy() {
        super.onDestroy()
        TelemetryServer.stopServer()
    }


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TELEMETRY SERVER REMOVED FOR STABILITY
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(false) } // Light by default
            
            SilicaClusterTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                        // Global App State
                var selectedModel by remember { mutableStateOf<LLMModel?>(null) }
                var selectedBridge by remember { mutableStateOf(InternetBridge.Cloudflare_Free) }
                var isWorkerMode by remember { mutableStateOf(false) }
                var isServerRunning by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text("SILICA CLUSTER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                            actions = {
                                // Theme Toggle moved to TopBar for safety
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                                    Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it })
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            tonalElevation = 8.dp
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        if (currentRoute == "home") Icons.Filled.Hub else Icons.Outlined.Hub, 
                                        contentDescription = "Stats"
                                    ) 
                                },
                                label = { Text("Command") },
                                selected = currentRoute == "home",
                                onClick = { navController.navigate("home") { launchSingleTop = true } }
                            )
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        if (currentRoute == "distributed") Icons.Filled.Dns else Icons.Outlined.Dns, 
                                        contentDescription = "Cluster"
                                    ) 
                                },
                                label = { Text("Cluster") },
                                selected = currentRoute == "distributed",
                                onClick = { navController.navigate("distributed") { launchSingleTop = true } }
                            )
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        if (currentRoute == "downloads") Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload, 
                                        contentDescription = "Models"
                                    ) 
                                },
                                label = { Text("Models") },
                                selected = currentRoute == "downloads",
                                onClick = { navController.navigate("downloads") { launchSingleTop = true } }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            SetupScreen(
                                selectedModel = selectedModel,
                                onModelSelected = { selectedModel = it },
                                selectedBridge = selectedBridge,
                                onBridgeSelected = { selectedBridge = it },
                                isWorkerMode = isWorkerMode,
                                workerIp = "", // Dummy value
                                isServerRunning = isServerRunning,
                                onStartService = { model, bridge, workerMode, _ ->
                                    if (model != null || workerMode) {
                                        startSilicaServer(model ?: ModelDirectory.getModels(applicationContext).first(), bridge, workerMode, "")
                                        isServerRunning = true
                                    }
                                },
                                onStopService = {
                                    stopSilicaServer()
                                    isServerRunning = false
                                }
                            )
                        }
                        composable("distributed") {
                            DistributedScreen(
                                isWorkerMode = isWorkerMode,
                                onWorkerModeChange = { isWorkerMode = it },
                                isServerRunning = isServerRunning,
                                onStartWorkerServer = {
                                    isWorkerMode = true
                                    startSilicaServer(ModelDirectory.getModels(applicationContext).first(), InternetBridge.Cloudflare_Free, true, "")
                                    isServerRunning = true
                                },
                                onStopWorkerServer = {
                                    isWorkerMode = false
                                    stopSilicaServer()
                                    isServerRunning = false
                                }
                            )
                        }
                        composable("downloads") {
                            DownloadsScreen()
                        }
                    }
                }
            }
        }
    }

    private fun startSilicaServer(model: LLMModel, bridge: InternetBridge, isWorker: Boolean, workerIp: String) {
        val intent = Intent(this, SilicaService::class.java).apply {
            action = "START"
            val safeDir = try {
                val ext = applicationContext.getExternalFilesDir(null)
                if (ext != null) java.io.File(ext, "models").absolutePath else java.io.File(applicationContext.filesDir, "models").absolutePath
            } catch (e: Exception) {
                java.io.File(applicationContext.filesDir, "models").absolutePath
            }
            putExtra("MODEL_PATH", "$safeDir/${model.name.replace(" ", "_").lowercase()}.gguf")
            putExtra("BRIDGE", bridge.name)
            putExtra("IS_WORKER", isWorker)
            putExtra("WORKER_IP", workerIp)
            putExtra("BRIDGE_TOKEN", BridgeStateManager.bridgeToken.value)
            putExtra("THREAD_COUNT", BridgeStateManager.threadCount.value)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopSilicaServer() {
        BridgeStateManager.currentBridgeUrl.value = null
        val intent = Intent(this, SilicaService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
    }
}

// ----------------------------------------------------
// SCREEN 1: COMMAND CENTER (SETUP)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    selectedModel: LLMModel?,
    onModelSelected: (LLMModel) -> Unit,
    selectedBridge: InternetBridge,
    onBridgeSelected: (InternetBridge) -> Unit,
    isWorkerMode: Boolean,
    workerIp: String,
    isServerRunning: Boolean,
    onStartService: (LLMModel?, InternetBridge, Boolean, String) -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showApiKeyModal by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }
    
    val activeDownloads by ModelDownloader.activeDownloads.collectAsState()
    val modelActiveState = selectedModel?.let { activeDownloads[it.name] }
    val downloadProgress = modelActiveState?.progress ?: -1
    val downloadSpeed = modelActiveState?.speedMbps ?: ""

    val modelFileName = selectedModel?.name?.replace(" ", "_")?.lowercase()?.plus(".gguf") ?: ""
    val storageDir = try {
        context.getExternalFilesDir(null) ?: context.filesDir
    } catch (e: Exception) {
        context.filesDir
    }
    val modelFile = java.io.File(storageDir, "models/$modelFileName")
    
    var isDownloaded by remember { mutableStateOf(false) }
    LaunchedEffect(selectedModel, activeDownloads) {
        isDownloaded = File(modelFile.absolutePath + ".completed").exists()
    }

    // Hardware Telemetry (Defensively handled on background thread)
    var deviceInfo by remember { mutableStateOf<HardwareManager.DeviceInfo?>(null) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            while(true) {
                try {
                    val info = HardwareManager.getDeviceInfo(context)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        deviceInfo = info
                    }
                } catch (e: Exception) {
                    Log.e("SilicaCluster", "Telemetry failed: ${e.message}")
                }
                kotlinx.coroutines.delay(2500)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // TELEMETRY GAUGES
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Text("RAM USAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                val ramText = deviceInfo?.let { String.format("%.1f / %.1f GB", it.totalRamGb - it.availableRamGb, it.totalRamGb) } ?: "---"
                Text(ramText, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                LinearProgressIndicator(
                    progress = { deviceInfo?.let { (1f - (it.availableRamGb / it.totalRamGb).toFloat()) } ?: 0f },
                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            GlassCard(modifier = Modifier.weight(1f)) {
                Text("THERMALS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                val tempText = deviceInfo?.let { "${it.batteryTempCelsius.toInt()}°C" } ?: "---"
                Text(tempText, style = MaterialTheme.typography.titleLarge, color = if ((deviceInfo?.batteryTempCelsius ?: 0.0) > 40) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CORE SELECTION
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LLM MODEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            
            val activeNodes by NodeManager.activeNodes.collectAsState()
            val onlineCount = activeNodes.count { it.status == NodeStatus.ONLINE }
            if (onlineCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.tertiary) {
                    Text("CLUSTER READY: $onlineCount NODES", fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ModelDropdown(selectedModel = selectedModel, onModelSelected = onModelSelected)

        Spacer(modifier = Modifier.height(16.dp))
        
        // BRIDGE SELECTION (RESTORED)
        Text("NETWORK BRIDGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        BridgeDropdown(selectedBridge = selectedBridge, onBridgeSelected = onBridgeSelected)

        Spacer(modifier = Modifier.height(24.dp))

        val threadCount by BridgeStateManager.threadCount.collectAsState()
        val maxCores = maxOf(1, deviceInfo?.cpuCount ?: BridgeStateManager.threadCount.value).toFloat()
        val safeThreadValue = threadCount.toFloat().coerceIn(1f, maxCores)
        
        Text("COMPUTE THREADS: ${safeThreadValue.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Slider(
            value = safeThreadValue,
            onValueChange = { BridgeStateManager.threadCount.value = it.toInt() },
            valueRange = 1f..maxCores,
            steps = maxOf(0, maxCores.toInt() - 2)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // SYSTEM STATUS (RESTORED LOGIC)
        if (isServerRunning) {
            val bridgeUrlGlobal by BridgeStateManager.currentBridgeUrl.collectAsState()
            val bridgeLogsGlobal by BridgeStateManager.bridgeLogs.collectAsState()
            val isLlamaReady by BridgeStateManager.isLlamaReady.collectAsState()
            
            val activeNodes by NodeManager.activeNodes.collectAsState()
            val engineCrashed by BridgeStateManager.engineCrashed.collectAsState()
            val engineCrashReason by BridgeStateManager.engineCrashReason.collectAsState()
            
            val liveStatus by remember(bridgeUrlGlobal, bridgeLogsGlobal, isLlamaReady, activeNodes, engineCrashed, engineCrashReason) {
                derivedStateOf {
                    val logsString = bridgeLogsGlobal.takeLast(25).joinToString("\n")
                    when {
                        engineCrashed -> {
                            val cleanError = engineCrashReason?.take(47) ?: "ABNORMAL EXIT"
                            "🔴 CRASH: ${cleanError.uppercase()}"
                        }
                        isLlamaReady -> if (activeNodes.any { it.status == NodeStatus.ONLINE }) "🟢 SYSTEM ONLINE" else "🟢 SYSTEM ONLINE"
                        logsString.contains("load_model", ignoreCase = true) || 
                        logsString.contains("load_tensors", ignoreCase = true) ||
                        logsString.contains("tensor", ignoreCase = true) ||
                        logsString.contains("shard", ignoreCase = true) ||
                        logsString.contains("warming up", ignoreCase = true) -> "🟡 LOADING MODEL INTO RAM..."
                        logsString.contains("rpc", ignoreCase = true) && 
                        logsString.contains("distributing", ignoreCase = true) -> "🟡 DISTRIBUTING SHARDS TO CLUSTER..."
                        bridgeUrlGlobal == "Initializing Tunnel..." -> "🔵 STARTING INTERNET BRIDGE..."
                        bridgeUrlGlobal != null -> "🟡 TUNNEL READY - CONNECTING LLM..."
                        isServerRunning -> "🟡 INITIALIZING KERNEL..."
                        else -> "⚪ SYSTEM OFFLINE"
                    }
                }
            }
            
            Text(
                text = liveStatus, 
                style = MaterialTheme.typography.titleMedium, 
                color = if (liveStatus.startsWith("🟢")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // SHOW TUNNEL URL
            if (!bridgeUrlGlobal.isNullOrBlank() && bridgeUrlGlobal != "Initializing Tunnel...") {
                val clipboardManager = LocalClipboardManager.current
                OutlinedTextField(
                    value = bridgeUrlGlobal!!,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("API Host") },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(bridgeUrlGlobal!!))
                            android.widget.Toast.makeText(context, "Link Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Link, contentDescription = "Copy")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // MAIN BOOT CONTROL
        HackerButton(
            onClick = {
                if (isServerRunning) onStopService()
                else if (selectedModel != null && isDownloaded) onStartService(selectedModel, selectedBridge, isWorkerMode, "")
                else if (selectedModel != null) ModelDownloader.startDownload(selectedModel.name, selectedModel.downloadUrl, modelFile)
            },
            text = when {
                isServerRunning -> "TERMINATE CLUSTER"
                !isDownloaded && selectedModel != null && downloadProgress != -1 -> "PULLING DATA..."
                !isDownloaded && selectedModel != null -> "DOWNLOAD MODEL"
                else -> "BOOT CLUSTER"
            },
            modifier = Modifier.fillMaxWidth(),
            containerColor = when {
                isServerRunning -> AlertRed
                !isDownloaded && selectedModel != null -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.primary
            },
            icon = if (isServerRunning) Icons.Default.StopCircle else if (!isDownloaded && selectedModel != null) Icons.Default.FileDownload else Icons.Default.Bolt,
            enabled = selectedModel != null || isWorkerMode
        )

        if (downloadProgress != -1) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = downloadProgress/100f, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DOWNLOADING: $downloadProgress%", style = MaterialTheme.typography.labelSmall)
                Text(downloadSpeed, style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { selectedModel?.let { ModelDownloader.stopDownload(it.name) } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("CANCEL DOWNLOAD", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // SECONDARY ACTIONS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = { showApiKeyModal = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("API KEYS")
            }
            OutlinedButton(
                onClick = { showTerminal = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("TERMINAL")
            }
        }

        if (showApiKeyModal) ApiKeyModal(onDismiss = { showApiKeyModal = false })
        if (showTerminal) ActivityTerminalSheet(onDismiss = { showTerminal = false })
    }
}

// ----------------------------------------------------
// UI COMPONENTS
// ----------------------------------------------------
// SCREEN 2: DISTRIBUTED INFERENCE
// ----------------------------------------------------
// ----------------------------------------------------
// SCREEN 2: CLUSTER MAP (DISTRIBUTED)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributedScreen(
    isWorkerMode: Boolean,
    onWorkerModeChange: (Boolean) -> Unit,
    isServerRunning: Boolean,
    onStartWorkerServer: () -> Unit,
    onStopWorkerServer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeNodes by NodeManager.activeNodes.collectAsState()
    var newNodeIp by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text("CLUSTER MAP", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // WORKER TOGGLE
        GlassCard(borderColor = if (isWorkerMode) MatrixGreen.copy(alpha = 0.5f) else ObsidianCard) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Worker Engine Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Participate as a compute node", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isWorkerMode,
                    onCheckedChange = { onWorkerModeChange(it) },
                    enabled = !isServerRunning,
                    colors = SwitchDefaults.colors(checkedThumbColor = MatrixGreen, checkedTrackColor = MatrixGreen.copy(alpha = 0.2f))
                )
            }
            
            if (isWorkerMode) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // IMPORTANT NOTE FOR USERS
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Row {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "NOTE: In a distributed cluster, the initial handshake and model distribution to worker nodes can take several minutes on mobile hardware.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val localIp = remember { NetworkManager.getLocalIpAddress() }
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                
                OutlinedTextField(
                    value = localIp,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("MY NODE IP (TARGET)") },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(localIp))
                            android.widget.Toast.makeText(context, "IP Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                )

                val lastPeerIp by BridgeStateManager.lastPeerIp.collectAsState()
                val trustedMasterIp by BridgeStateManager.trustedMasterIp.collectAsState()

                if (isServerRunning) {
                    if (trustedMasterIp != null) {
                        Text(
                            "MASTER ATTACHED: $trustedMasterIp",
                            style = MaterialTheme.typography.titleSmall,
                            color = MatrixGreen,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (lastPeerIp != null) {
                        Text(
                            "Master IP Identified: $lastPeerIp (Awaiting Verification)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Text(
                            "Listening for master node connection...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                if (isServerRunning && trustedMasterIp != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    var showWorkerTerminal by remember { mutableStateOf(false) }
                    
                    HackerButton(
                        onClick = { showWorkerTerminal = true },
                        text = "VIEW COMPUTE ACTIVITY",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        icon = Icons.Default.Terminal
                    )
                    
                    if (showWorkerTerminal) {
                        WorkerTerminalDialog(onDismiss = { showWorkerTerminal = false })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                HackerButton(
                    onClick = { if (isServerRunning) onStopWorkerServer() else onStartWorkerServer() },
                    text = if (isServerRunning) "TERMINATE LISTENER" else "START LISTENING",
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (isServerRunning) AlertRed else MatrixGreen,
                    icon = if (isServerRunning) Icons.Default.CloudOff else Icons.Default.CloudQueue
                )
            }
        }

        // DIALOGS
        val incomingRequestIp by BridgeStateManager.incomingRequestIp.collectAsState()
        if (incomingRequestIp != null) {
            AlertDialog(
                onDismissRequest = { BridgeStateManager.incomingRequestIp.value = null },
                title = { Text("Cluster Connection Request") },
                text = { Text("Remote device at $incomingRequestIp is requesting access to your compute cores. Allow this connection?") },
                confirmButton = {
                    Button(onClick = { 
                        BridgeStateManager.trustedMasterIp.value = incomingRequestIp
                        BridgeStateManager.incomingRequestIp.value = null
                        BridgeStateManager.workerStatus.value = "Connected"
                    }, colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen)) {
                        Text("ALLOW", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        BridgeStateManager.incomingRequestIp.value = null
                        BridgeStateManager.workerStatus.value = "Listening"
                    }) {
                        Text("DENY")
                    }
                }
            )
        }

        if (!isWorkerMode) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("NETWORK NODES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            // ADD NODE INPUT
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newNodeIp,
                    onValueChange = { newNodeIp = it },
                    label = { Text("IPv4 Target", color = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { 
                        if (newNodeIp.isNotBlank()) {
                            NodeManager.addNode(newNodeIp.trim())
                            newNodeIp = ""
                        }
                    },
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HackerButton(
                onClick = { showScanner = true },
                text = "AUTO SCAN NETWORK",
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                icon = Icons.Default.WifiTethering
            )
            
            Spacer(modifier = Modifier.height(16.dp))

        // SWARM SUMMARY
        val onlineWorkers = activeNodes.filter { it.status == NodeStatus.ONLINE }
        val leaderInfo = remember { HardwareManager.getDeviceInfo(context) }
        val threadCount by BridgeStateManager.threadCount.collectAsState()
        
        // Use user-selected threadCount for local contribution, plus worker allocated threads
        val totalCores = threadCount + onlineWorkers.sumOf { it.telemetry?.threadCount ?: 0 }
        val totalRam = leaderInfo.totalRamGb + onlineWorkers.sumOf { it.telemetry?.totalRamGb ?: 0.0 }
        val totalAvailableRam = leaderInfo.availableRamGb + onlineWorkers.sumOf { it.telemetry?.availableRamGb ?: 0.0 }

        GlassCard(borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SWARM CAPACITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    Text("${totalCores} THREADS | ${String.format("%.1f", totalAvailableRam)} GB AVAIL", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // NODE LIST
        Text("TOPOLOGY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        
        // 1. LEADER NODE (MASTER)
        LeaderTelemetryCard(leaderInfo, threadCount)
        Spacer(Modifier.height(16.dp))

        // 2. WORKER NODES
        if (activeNodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("WAITING FOR COMPUTE NODES...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            activeNodes.forEach { node ->
                NodeTelemetryCard(node)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showScanner) {
            NetworkScannerSheet(
                onDismiss = { showScanner = false },
                onNodeAttached = { ip -> 
                    NodeManager.addNode(ip)
                    NodeManager.updateNodeStatus(ip, NodeStatus.ONLINE)
                    showScanner = false
                }
            )
        }
    }
}

@Composable
fun LeaderTelemetryCard(info: HardwareManager.DeviceInfo, allocatedThreads: Int) {
    GlassCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text("LOCAL NODE (LEADER)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Primary Orchestrator • Initializing Shards", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            
            Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary) {
                Text("ACTIVE", fontSize = 10.sp, modifier = Modifier.padding(4.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Stats
            Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Column {
                    Text("THREADS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("$allocatedThreads / ${info.cpuCount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            Box(modifier = Modifier.weight(1.5f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Column {
                    Text("RAM USE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${String.format("%.1f", info.totalRamGb - info.availableRamGb)} / ${String.format("%.1f", info.totalRamGb)} GB", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Column {
                    Text("TEMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${info.batteryTempCelsius}°C", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NodeTelemetryCard(node: WorkerNode) {
    val scope = rememberCoroutineScope()
    
    // Auto-refresh telemetry on appear
    LaunchedEffect(node.ip) {
        if (node.status == NodeStatus.UNKNOWN || node.telemetry == null || node.status == NodeStatus.VERIFYING) {
            val masterIp = NetworkManager.getLocalIpAddress()
            
            // 1. Initial Ping
            val isOnline = NetworkManager.pingWorkerNode(node.ip, timeoutMs = 800)
            if (!isOnline) {
                NodeManager.updateNodeStatus(node.ip, NodeStatus.UNREACHABLE)
                return@LaunchedEffect
            }
            
            // 2. Request Join
            NodeManager.updateNodeStatus(node.ip, NodeStatus.VERIFYING)
            val requestSent = NetworkManager.requestJoin(node.ip, masterIp)
            
            if (requestSent) {
                // 3. Poll for Approval (Max 60 seconds)
                for (i in 1..30) {
                    val authorizedIp = NetworkManager.checkJoinStatus(node.ip)
                    if (authorizedIp != null) {
                        NodeManager.updateNodeStatus(node.ip, NodeStatus.ONLINE)
                        val telemetry = NetworkManager.fetchWorkerTelemetry(node.ip)
                        NodeManager.updateNodeTelemetry(node.ip, telemetry)
                        return@LaunchedEffect
                    }
                    kotlinx.coroutines.delay(2000)
                }
            }
            
            NodeManager.updateNodeStatus(node.ip, if (requestSent) NodeStatus.VERIFYING else NodeStatus.UNREACHABLE)
        }
    }

    GlassCard(borderColor = when(node.status) {
        NodeStatus.ONLINE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
        NodeStatus.VERIFYING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when(node.status) {
                NodeStatus.ONLINE -> MaterialTheme.colorScheme.tertiary
                NodeStatus.VERIFYING -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            Box(modifier = Modifier.size(8.dp).background(statusColor, RoundedCornerShape(50)))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(node.ip, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                when(node.status) {
                    NodeStatus.ONLINE -> Text("INFERENCE READY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    NodeStatus.VERIFYING -> Text("WAITING FOR WORKER PERMISSION...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    NodeStatus.UNREACHABLE -> Text("NODE UNREACHABLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    else -> Text("DISCOVERING...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(onClick = { 
                NodeManager.updateNodeStatus(node.ip, NodeStatus.VERIFYING)
            }) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            }
            IconButton(onClick = { NodeManager.removeNode(node.ip) }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            }
        }
        
        if (node.telemetry != null) {
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("THREADS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${node.telemetry!!.threadCount} / ${node.telemetry!!.cpuCount}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("RAM USE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(String.format("%.1f / %.1f GB", node.telemetry!!.totalRamGb - node.telemetry!!.availableRamGb, node.telemetry!!.totalRamGb), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("TEMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${node.telemetry!!.batteryTempCelsius.toInt()}°C", style = MaterialTheme.typography.titleMedium, color = if (node.telemetry!!.batteryTempCelsius > 40) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                }
            }
        } else {
            Text("WAITING FOR TELEMETRY...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ----------------------------------------------------
// SCREEN 3: DOWNLOADED MODELS
data class DownloadedModelInfo(val model: LLMModel, val file: File, var hasUpdate: Boolean = false)

// ----------------------------------------------------
// SCREEN 3: MODEL ARCHIVE (DOWNLOADS)
// ----------------------------------------------------
@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    var models by remember { mutableStateOf(ModelDirectory.getModels(context)) }
    val activeDownloads by ModelDownloader.activeDownloads.collectAsState()
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("LLM Models", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        HackerButton(
            onClick = { showCustomDialog = true },
            text = "ADD CUSTOM MODEL +",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )

        if (showCustomDialog) {
            AddCustomModelDialog(
                onDismiss = { showCustomDialog = false },
                onModelAdded = { 
                    models = ModelDirectory.getModels(context) 
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        var selectedModelForDetails by remember { mutableStateOf<LLMModel?>(null) }

        models.forEach { model ->
            val modelFileName = model.name.replace(" ", "_").lowercase() + ".gguf"
            val storageDir = try {
                context.getExternalFilesDir(null) ?: context.filesDir
            } catch (e: Exception) {
                context.filesDir
            }
            val file = File(storageDir, "models/$modelFileName")
            val isDownloaded = File(file.absolutePath + ".completed").exists()
            val downloadState = activeDownloads[model.name]

            GlassCard(modifier = Modifier.padding(bottom = 12.dp).clickable { selectedModelForDetails = model }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(model.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.secondary) {
                                Text(model.tier, fontSize = 9.sp)
                            }
                        }
                        Text(model.useCase, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    if (downloadState != null) {
                        val infiniteTransition = rememberInfiniteTransition(label = "downloading")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Icon(
                            Icons.Default.FileDownload, 
                            contentDescription = "Downloading", 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp).graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    } else if (isDownloaded) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    } else {
                        IconButton(onClick = { 
                            ModelDownloader.startDownload(model.name, model.downloadUrl, file) 
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                
                if (downloadState != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("${downloadState.progress}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = downloadState.progress / 100f, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${downloadState.speedMbps}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("Size: ${model.totalSize}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    TextButton(onClick = { ModelDownloader.stopDownload(model.name) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("CANCEL DOWNLOAD", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                } else if (!isDownloaded) {
                    Spacer(Modifier.height(4.dp))
                    Text("Ready to Pull: ${model.totalSize}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        if (selectedModelForDetails != null) {
            ModelDetailsDialog(
                model = selectedModelForDetails!!,
                onDismiss = { selectedModelForDetails = null },
                activeDownloads = activeDownloads
            )
        }
    }
}

// ----------------------------------------------------
// UTILITY COMPONENTS
// ----------------------------------------------------

@Composable
fun ModelDetailRow(label: String, value: String) {
    Row(modifier = Modifier
        .padding(vertical = 4.dp)
        .fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.weight(2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDropdown(selectedModel: LLMModel?, onModelSelected: (LLMModel) -> Unit) {
    val context = LocalContext.current
    var models by remember { mutableStateOf(ModelDirectory.getModels(context)) }
    var expanded by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }

    Box {
        GlassCard(modifier = Modifier.clickable { expanded = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedModel?.name ?: "Select LLM Model", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (selectedModel == null) "Initiate sequence..." else "Ready for deployment", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(model.name, fontWeight = FontWeight.Bold)
                            Text("${model.tier} | RAM: ${model.ramRequired}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("ADD CUSTOM MODEL +", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                onClick = {
                    showCustomModelDialog = true
                    expanded = false
                }
            )
        }
    }

    if (showCustomModelDialog) {
        AddCustomModelDialog(
            onDismiss = { showCustomModelDialog = false },
            onModelAdded = { newModel ->
                models = ModelDirectory.getModels(context)
                onModelSelected(newModel)
            }
        )
    }
}

@Composable
fun AddCustomModelDialog(onDismiss: () -> Unit, onModelAdded: (LLMModel) -> Unit) {
    val context = LocalContext.current
    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var customSize by remember { mutableStateOf("Variable") }
    var isVerifyingUrl by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom LLM") },
        text = {
            Column {
                OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("Model Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = customSize, onValueChange = { customSize = it }, label = { Text("File Size (e.g. 1.2 GB)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = customUrl, onValueChange = { customUrl = it }, label = { Text("Direct .gguf Download URL") }, modifier = Modifier.fillMaxWidth())
                if (isVerifyingUrl) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Verifying Network URI...", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (customName.isNotBlank() && customUrl.isNotBlank()) {
                    isVerifyingUrl = true
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val urlStr = customUrl.trim()
                            if (!urlStr.startsWith("http")) throw Exception("Must start with http:// or https://")
                            
                            val url = java.net.URL(urlStr)
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.requestMethod = "HEAD"
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000
                            
                            val responseCode = connection.responseCode
                            if (responseCode !in 200..399) {
                                throw Exception("Server rejected stream (HTTP $responseCode)")
                            }
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                val newModel = LLMModel(
                                    name = customName.trim(), 
                                    paramCount = "Custom", 
                                    ramRequired = "? GB", 
                                    useCase = "Custom User Loaded Link", 
                                    tier = "Custom", 
                                    totalSize = customSize.trim(),
                                    downloadUrl = urlStr
                                )
                                ModelDirectory.addCustomModel(context, newModel)
                                onModelAdded(newModel)
                                onDismiss()
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isVerifyingUrl = false
                                android.widget.Toast.makeText(context, "Invalid Link: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }, enabled = !isVerifyingUrl) {
                Text(if (isVerifyingUrl) "Pinging..." else "Add Model")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ModelDetailsDialog(model: LLMModel, onDismiss: () -> Unit, activeDownloads: Map<String, DownloadState>) {
    val context = LocalContext.current
    val downloadState = activeDownloads[model.name]
    val modelFileName = model.name.replace(" ", "_").lowercase() + ".gguf"
    val storageDir = try {
        context.getExternalFilesDir(null) ?: context.filesDir
    } catch (e: Exception) {
        context.filesDir
    }
    val file = File(storageDir, "models/$modelFileName")
    val isDownloaded = File(file.absolutePath + ".completed").exists()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(model.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Spacer(Modifier.height(24.dp))
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("TECHNICAL SPECIFICATIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        ModelDetailRow("Parameters", model.paramCount)
                        ModelDetailRow("Memory Pressure", model.ramRequired)
                        ModelDetailRow("Model Tier", model.tier)
                        ModelDetailRow("Target Workload", model.useCase)
                        ModelDetailRow("Asset Footprint", model.totalSize)
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                if (downloadState != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text("DECODING BITSTREAM...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("${downloadState.progress}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${downloadState.speedMbps}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            Text("Total Size: ${model.totalSize}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(Modifier.height(24.dp))
                        HackerButton(
                            onClick = { ModelDownloader.stopDownload(model.name) },
                            text = "TERMINATE ACQUISITION",
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    }
                } else if (isDownloaded) {
                    HackerButton(
                        onClick = onDismiss,
                        text = "READY FOR DEPLOYMENT",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    HackerButton(
                        onClick = { ModelDownloader.startDownload(model.name, model.downloadUrl, file) },
                        text = "INITIATE DOWNLOAD",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeDropdown(selectedBridge: InternetBridge, onBridgeSelected: (InternetBridge) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var bridgeToken by remember { mutableStateOf(BridgeStateManager.bridgeToken.value) }
    var showTokenConfigDialog by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedBridge.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Internet Bridge Provider") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                InternetBridge.values().forEach { bridge ->
                    DropdownMenuItem(
                        text = { Text(bridge.displayName) },
                        onClick = {
                            onBridgeSelected(bridge)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        if (selectedBridge.requiresToken) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (BridgeStateManager.bridgeToken.collectAsState().value.isNotBlank()) {
                    Text("✅ Token Configured", color = androidx.compose.ui.graphics.Color(0xFF4CAF50), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Button(onClick = { showTokenConfigDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Text("Edit Config")
                    }
                } else {
                    Text("⚠️ Token Required", color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Button(onClick = { showTokenConfigDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Configure")
                    }
                }
            }
        }
        
        if (showTokenConfigDialog) {
            AlertDialog(
                onDismissRequest = { showTokenConfigDialog = false },
                title = { Text("Configure ${selectedBridge.displayName}") },
                text = {
                    Column {
                        Text("Please paste your internal authentication token or key for ${selectedBridge.name}.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bridgeToken,
                            onValueChange = { bridgeToken = it },
                            label = { Text("Auth Token / Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        BridgeStateManager.bridgeToken.value = bridgeToken
                        showTokenConfigDialog = false
                    }) {
                        Text("Save Config")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTokenConfigDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyModal(onDismiss: () -> Unit) {
    val keys by ApiKeyManager.keys.collectAsState()
    val activeKey by ApiKeyManager.activeKey.collectAsState()
    var newKeyName by remember { mutableStateOf("") }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("API Access Tokens", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Generate and manage authentication bearer keys for remote connections. Only ONE key may be active at any given time.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    OutlinedTextField(
                        value = newKeyName,
                        onValueChange = { newKeyName = it },
                        label = { Text("App or Service Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (newKeyName.isNotBlank()) {
                                ApiKeyManager.generateKey(newKeyName)
                                newKeyName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Generate Key")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Your Secrets", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (keys.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No Keys Found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(keys) { key ->
                        val isActive = key.id == activeKey?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { ApiKeyManager.setActiveKey(key.id) },
                            colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                            border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(key.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(key.key, fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    if (isActive) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE KEY", 
                                                fontSize = 10.sp, 
                                                color = MaterialTheme.colorScheme.onSecondaryContainer, 
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Row {
                                    FilledTonalButton(onClick = { 
                                        clipboardManager.setText(AnnotatedString(key.key))
                                        android.widget.Toast.makeText(context, "Copied Secret!", android.widget.Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("COPY")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { ApiKeyManager.deleteKey(key.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTerminalSheet(onDismiss: () -> Unit) {
    val activities by ActivityLogger.activities.collectAsState()
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(activities.size) {
        if (activities.isNotEmpty()) {
            scrollState.animateScrollToItem(activities.size - 1)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("KERNEL ACTIVITY", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text("Real-time interception of neural engine logs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                
                Box(modifier = Modifier.weight(1f).padding(top = 16.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn(state = scrollState) {
                        items(activities) { activity ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                val prefix = when {
                                    activity.isSystem -> "[SYS]"
                                    activity.isUser -> "[USR]"
                                    else -> "[LLM]"
                                }
                                val color = when {
                                    activity.isSystem -> MaterialTheme.colorScheme.secondary
                                    activity.isUser -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.tertiary
                                }
                                Text(
                                    text = "$prefix ",
                                    color = color,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activity.content,
                                    style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HackerButton(onClick = onDismiss, text = "CLOSE KERNEL LOGS", modifier = Modifier.fillMaxWidth(), contentColor = MatrixGreen)
            }
        }
    }
}

@Composable
fun WorkerTerminalDialog(onDismiss: () -> Unit) {
    val logs by BridgeStateManager.workerLogs.collectAsState()
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scrollState.animateScrollToItem(logs.size - 1)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("COMPUTE CONSOLE", style = MaterialTheme.typography.titleLarge, color = MatrixGreen)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MatrixGreen)
                    }
                }
                
                Text("REAL-TIME RPC COMPUTE STREAM", style = MaterialTheme.typography.labelSmall, color = MatrixGreen.copy(alpha = 0.5f))
                
                Box(modifier = Modifier.weight(1f).padding(top = 16.dp).background(Color.Black).border(1.dp, MatrixGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn(state = scrollState) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp),
                                color = MatrixGreen,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HackerButton(
                    onClick = onDismiss,
                    text = "CLOSE TERMINAL",
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MatrixGreen.copy(alpha = 0.1f),
                    contentColor = MatrixGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScannerSheet(onDismiss: () -> Unit, onNodeAttached: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var discoveredNodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isScanning && !scanComplete) {
            isScanning = true
            discoveredNodes = NetworkManager.discoverLocalWorkers()
            isScanning = false
            scanComplete = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ObsidianBg,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("NETWORK RADAR", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Sweeping subnet for active RPC endpoints...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isScanning) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("MAPPING TOPOLOGY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                if (discoveredNodes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("ZERO RECEPTORS FOUND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(discoveredNodes) { ip ->
                            GlassCard(modifier = Modifier.padding(bottom = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ip, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                        Text("ACTIVE ON 50052", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                    HackerButton(
                                        onClick = { onNodeAttached(ip) },
                                        text = "ATTACH",
                                        modifier = Modifier.height(40.dp),
                                        containerColor = MatrixGreen
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HackerButton(
                    onClick = {
                        isScanning = true
                        scanComplete = false
                        scope.launch {
                            discoveredNodes = NetworkManager.discoverLocalWorkers()
                            isScanning = false
                            scanComplete = true
                        }
                    },
                    text = "RE-SCAN SUBNET",
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}