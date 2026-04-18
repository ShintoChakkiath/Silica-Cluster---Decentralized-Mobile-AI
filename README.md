# Silica-Cluster---Decentralized-Mobile-AI
SilicaCluster is an open-source (AGPLv3) application designed to turn Android devices into nodes for a decentralized, local AI infrastructure . Built as a true private alternative to centralized AI services, this app allows you to run open-source Large Language Models (LLMs) locally on your phone or distributed across a cluster of devices.

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

Features
1. Command Center & Hacker-Style UI
The application features a Jetpack Compose-based "Hacker Style" interface
, utilizing high-end UI components like GlassCard (translucent borders) and HackerButton elements
.
Setup Screen: A central hub to configure your model, select an internet bridge, toggle worker modes, and manage your cluster
.
Activity Terminals: Live, real-time logging sheets to monitor user activity, system status, bridge logs, and engine stability (isLlamaReady)
.
2. Distributed Clustering & Inference
SilicaCluster allows you to link multiple devices to distribute heavy AI workloads.
Cluster Map & Network Scanner: Users can scan their local network to discover and attach other worker nodes to the cluster
.
Node Manager: Actively tracks the state of connected nodes (ONLINE, UNREACHABLE, VERIFYING)
.
Hardware Telemetry: A built-in TelemetryServer monitors the hardware health of the leader and worker nodes in real-time. It tracks CPU count, allocated threads, total/available RAM, and battery temperature to prevent overheating during inference
.
3. Built-In Model Downloader (.gguf)
The app includes a specialized ModelDownloader capable of handling .gguf files natively
. Users can add any custom model via a URL
, or choose from a built-in curated directory optimized for different phone capabilities
:
Nano/Lite Models: Danube 3 500M, Qwen 2.5 0.5B, Llama 3.2 1B
Efficient/Standard Models: Gemma 2 2B, Llama 3.2 3B, Phi-3.5 Mini, Qwen 2.5 Coder 3B
Power/Elite Models: Llama 3.1 8B, Mistral Nemo 12B (Designed specifically for clustered environments)
4. API Gateway & Secure Bridges
The system runs a local Ktor-based ApiGatewayServer designed to handle heavy distributed requests with up to a 10-minute timeout limit
. To access your local AI remotely, the app integrates secure internet bridging:
Supported Bridges: Cloudflare (Free or Token), Ngrok (Auth Token), and Tailscale (Auth Key)
.
API Key Management: A dedicated ApiKeyManager generates and stores unique UUID-based keys to secure your gateway against unauthorized access
.
5. Background Execution
The core server and clustering logic run inside a dedicated SilicaService, ensuring the AI node continues operating in the background
