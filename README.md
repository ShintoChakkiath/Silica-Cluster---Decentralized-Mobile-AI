# Silica Cluster-Decentralized Mobile AI 
<img src = "/logo.png" width = "100" >

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)


SilicaCluster is an open-source (AGPLv3) application designed to turn Android devices into nodes for a decentralized, local AI infrastructure . Built as a true private alternative to centralized AI services, this app allows you to run open-source Large Language Models (LLMs) locally on your phone or distributed across a cluster of devices.


<p align="center">
  <a href="https://github.com/ShintoChakkiath/Silica-Cluster-Decentralized-Mobile-AI/releases/download/v1.on.publish/Silica.Cluster.v1.apk">
    <img src="https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android" alt="Download APK">
  </a>
</p>

Silica Cluster is an Android-based Decentralized Mobile AI application. Its primary goal is to let users run, manage, and distribute Large Language Models (LLMs) entirely on their mobile devices without relying on third-party cloud services.

<table>
  <tr>
    <td align="center">
      <a href="https://youtu.be/3alLPsRKvJM?si=W6gMAo91HiZnTkW-">
        <img src="https://static.vecteezy.com/system/resources/thumbnails/011/998/173/small_2x/youtube-icon-free-vector.jpg" width="100"><br>
        <sub><b>Presentation</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://youtu.be/tdAJc6_iAIA?si=suuCA2rxNJ0ZXfeN">
        <img src="https://static.vecteezy.com/system/resources/thumbnails/011/998/173/small_2x/youtube-icon-free-vector.jpg" width="100"><br>
        <sub><b>Testing</b></sub>
      </a>
    </td>
  </tr>
</table>

<!-- Introduction -->
<h2>Project Breakdown</h2>
<p>Here is a detailed breakdown of the system architecture and its core capabilities.</p>

<hr>

<!-- Section 1: Core Engine -->
<h3>1. The Core AI Engine 🧠</h3>
<p><i>(BinaryRunner.kt & ModelConfig.kt)</i></p>
<p>The app runs AI <b>locally</b> on the phone's hardware rather than relying on external APIs.</p>
<ul>
  <li><b>Native Binaries:</b> Uses pre-compiled <code>llama.cpp</code> binaries (<code>libllama.so</code> and <code>librpc.so</code>).</li>
  <li><b>Quantized Models:</b> Downloads compressed GGUF models (Llama 3.2 1B/3B) and loads them directly into RAM.</li>
</ul>

<!-- Section 2: Clustering -->
<h3>2. The "Cluster" / Decentralization 🌐</h3>
<p><i>(NodeManager.kt & SilicaService.kt)</i></p>
<p>Overcomes hardware limitations by linking multiple devices over Wi-Fi.</p>
<ul>
  <li><b>Main Node:</b> The primary phone orchestrating the engine.</li>
  <li><b>Worker Nodes:</b> Spare phones that process "chunks" (tensor layers) of the model.</li>
  <li><b>Collaborative Computing:</b> Effectively creates a decentralized supercomputer from mobile devices.</li>
</ul>

<!-- Section 3: API & Tunneling -->
<h3>3. Public API Exposure 🛠️</h3>
<p><i>(BridgeStateManager.kt & ApiGatewayServer.kt)</i></p>
<ul>
  <li><b>Cloudflare Tunneling:</b> Generates a public URL to access your local AI from anywhere.</li>
  <li><b>Internal Gateway:</b> Uses a Ktor server to validate API keys and fix header issues like chunked-encoding.</li>
</ul>

<hr>

<!-- Main Pages Section -->
<h2>Main Pages & UI</h2>

<details>
<summary><b>📂 View Page Descriptions (Command Center, Cluster Map, etc.)</b></summary>
<br>

<h4>1. Command Center (SetupScreen)</h4>
<p>The control deck for your local AI server.</p>
<ul>
  <li><b>Hardware Telemetry:</b> Monitor live RAM usage and battery thermals.</li>
  <li><b>Engine Config:</b> Select models, set network bridges, and adjust CPU thread counts.</li>
  <li><b>Boot System:</b> A foreground service with real-time log streaming.</li>
</ul>

<h4>2. Cluster Map (DistributedScreen)</h4>
<p>Handles decentralized networking and swarm management.</p>
<ul>
  <li><b>Swarm Capacity:</b> Shows combined threads and available RAM across the network.</li>
  <li><b>Topology View:</b> Visual map showing the status (Online/Unreachable) of every node.</li>
</ul>

<h4>3. LLM Models (DownloadsScreen)</h4>
<p>The library manager for AI brains.</p>
<ul>
  <li><b>Direct Downloads:</b> Pulls <code>.gguf</code> files directly from HuggingFace.</li>
  <li><b>Custom Models:</b> Support for adding models via custom URLs.</li>
</ul>
</details>

<hr>

<!-- Deep Dive / How it Works -->
<h2>How Distributed AI Works ⚙️</h2>
<p>This app uses <b>Pipeline Parallelism</b> to split the workload.</p>

<table>
  <tr>
    <th>Phase</th>
    <th>What is sent</th>
    <th>Frequency</th>
  </tr>
  <tr>
    <td><b>1. Initialization</b></td>
    <td>Model Weights (Layers)</td>
    <td>Once (at startup)</td>
  </tr>
  <tr>
    <td><b>2. Inference</b></td>
    <td>Intermediate Tensors (Activations)</td>
    <td>Every token generated</td>
  </tr>
</table>

<blockquote>
  <b>The Assembly Line Analogy:</b> 
  Starting the server is like shipping heavy robot arms to a factory. Sending a message is like putting a car frame on the belt; the arms stay put, only the data (the car) moves between warehouses.
</blockquote>

<hr>

<!-- Safety Warnings -->
<h2>⚠️ Essential Safety & Technical Warnings</h2>

<details>
<summary><b>🔴 READ BEFORE RUNNING</b></summary>
<br>
<ul>
  <li><b>Thermal Risk:</b> Running LLMs is intensive. Use a fan to avoid battery degradation.</li>
  <li><b>Power Demand:</b> The app uses <code>WAKE_LOCK</code>; the phone will not sleep. Plug into a charger!</li>
  <li><b>Security:</b> Local RPC traffic is currently unencrypted. Do not use on public Wi-Fi.</li>
  <li><b>Privacy:</b> Web scraping exposes your home IP to the sites being searched.</li>
</ul>
</details>

<hr>

<p align="center">
  <i>Built with Jetpack Compose, Kotlin Coroutines, and llama.cpp</i>
</p>

Screenshots


<img src = "/Screenshot_2026-04-22-16-51-47-199_io.github.shintochakkiath.silicacluster.jpg" width = "250" >   <img src = "/Screenshot_2026-04-22-16-52-00-977_io.github.shintochakkiath.silicacluster.jpg" width = "250" >   <img src = "/Screenshot_2026-04-22-16-52-08-737_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-52-18-494_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-52-26-620_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-52-36-997_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-53-08-133_io.github.shintochakkiath.silicacluster.jpg" width = "250" >



