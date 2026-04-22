# Silica Cluster-Decentralized Mobile AI 
<img src = "/logo.png" width = "100" >

SilicaCluster is an open-source (AGPLv3) application designed to turn Android devices into nodes for a decentralized, local AI infrastructure . Built as a true private alternative to centralized AI services, this app allows you to run open-source Large Language Models (LLMs) locally on your phone or distributed across a cluster of devices.

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

Silica Cluster is an Android-based Decentralized Mobile AI application. Its primary goal is to let users run, manage, and distribute Large Language Models (LLMs) entirely on their mobile devices without relying on third-party cloud services.

Here is a breakdown of what it is and what it does, 

1. The Core AI Engine (BinaryRunner.kt & ModelConfig.kt)

The app does not just use APIs to talk to AI; it runs the AI locally on the phone's hardware.
•	It uses pre-compiled llama.cpp binaries (libllama.so for the main server and librpc.so for worker servers).
•	It downloads highly compressed "Quantized" GGUF models (like Llama 3.2 1B or 3B) and loads them directly into the Android device's RAM.

2. The "Cluster" / Decentralization (NodeManager.kt & SilicaService.kt)
   
This is where the "Cluster" name comes from. Running a massive AI model on one phone might be too slow or crash the device due to lack of RAM.
•	The app allows the user to link multiple phones together over a Wi-Fi network.
•	One phone acts as the Main Node (running libllama.so), and other phones act as Worker Nodes (running librpc.so).
•	The main phone splits the AI model into chunks (tensor layers) and sends them to the worker phones to process the math collaboratively, effectively creating a decentralized supercomputer out of old/spare mobile phones.

3. Public API Exposure (BridgeStateManager.kt & ApiGatewayServer.kt)

The app allows you to use your phone as a host for an AI API.
•	It uses an embedded Cloudflare Tunnel binary to punch a secure hole through your local network, generating a public URL (e.g., https://something.trycloudflare.com).
•	It runs an internal ApiGatewayServer using Ktor to intercept incoming API requests, validate API keys, fix any header issues (like chunked-encoding), and pass the requests to the local Llama server.

4. Smart Chat & Real-Time RAG (ChatScreen.kt & WebSearcher.kt)
   
It features a GPT-style interface built in Jetpack Compose, but with advanced features:
•	Real-Time Access: It can scrape DuckDuckGo, Wikipedia, Weather APIs, and custom RSS feeds.
•	Invisible Routing: When you send a message, it secretly queries the LLM to ask if it needs to search the web. If it does, it fetches the live data, injects it into your prompt as "Context", and forces the local LLM to answer using only the newly retrieved facts.

Main Pages

1. Command Center (SetupScreen)
This is the main control deck for your local AI server. Its purpose is to configure and boot the main llama-server engine on the device.
•	Hardware Telemetry: At the very top, it constantly monitors your phone's physical hardware on a background thread. It displays live RAM Usage and Thermals (battery temperature) so you can ensure the phone doesn't overheat or crash while running heavy models.
•	Engine Configuration:
•	You select the LLM Model you want to load from your downloaded library.
•	You select the Network Bridge (like Cloudflare Free) if you want to expose your local AI to the internet as a public API.
•	You adjust the Compute Threads slider. The app automatically detects your phone's maximum CPU cores and lets you restrict how many the AI is allowed to consume.
•	Live Status & Boot: The massive "BOOT CLUSTER" button starts a foreground service. Below it, a dynamic status text reads the engine logs in real-time, showing exactly what's happening under the hood (e.g., "🟡 LOADING MODEL INTO RAM...", "🔵 STARTING INTERNET BRIDGE...", "🟢 SYSTEM ONLINE"). Once online, it provides the secure public URL for your server.

2. Cluster Map (DistributedScreen)
This page handles the decentralized networking, transforming multiple separate phones into one giant "Swarm" or "Cluster".
•	Swarm Capacity Summary: It calculates the combined processing power of your network. It adds up your main device's threads/RAM with the telemetry data from all connected worker nodes to show you the total capacity (e.g., "8 THREADS | 4.5 GB AVAIL").
•	Auto-Scanner & Manual Addition: You can type in the local IPv4 address of an old phone on your Wi-Fi, or use the "AUTO SCAN NETWORK" button to find devices running the app automatically.
•	Topology View: It displays a visual map of the network:
•	Local Node (Leader): Your primary phone, orchestrating the chunks of data.
•	Worker Nodes: Cards for every connected phone. The app constantly pings these workers to check their status (Verifying, Online, Unreachable) and pulls their live hardware telemetry (RAM, Temp, Threads) over the network so you can monitor the health of the entire cluster from one screen.

3. LLM Models (DownloadsScreen)
This is the library and file manager for the AI brains themselves.
•	Model Archive: It lists pre-configured quantized .gguf models (like Llama 3.2 1B, 3B, or 8B). Each card tells you the model's tier, its primary use case (e.g., "The Standard for budget phones"), and its total file size.
•	Active Downloader: If you click the download icon, the app uses ModelDownloader.kt to pull the massive multi-gigabyte .gguf files directly from HuggingFace to your phone's local storage. It features a live progress bar and download speed indicator.
•	Custom Models: There is a button to add a "Custom Model" via URL, allowing you to load any compatible llama.cpp model from the internet into the app's ecosystem.

Beyond the main pages and AI networking, there are several "hidden" or system-level features in the code that are crucial to making this run smoothly on a mobile device:

1. The API Gateway Middleware (ApiGatewayServer.kt)
The app doesn't just connect the internet directly to the LLM; it places a custom-built "Gateway" in the middle.
•	Crash Prevention: Native llama.cpp servers often crash when receiving "chunked" HTTP requests from services like Cloudflare. The Gateway intercepts the request, calculates the exact byte size, reformats the headers, and safely feeds it to the LLM.
•	Security Check: It intercepts every incoming request to verify that the sender provided the correct API Key before the LLM even sees it.

2. Battery & System Persistence (SilicaService.kt)
Android operating systems are designed to aggressively kill background tasks to save battery. If you are hosting a server, this is a disaster.
•	WakeLocks: The code requests an OS-level PARTIAL_WAKE_LOCK. This explicitly commands the phone’s CPU to stay awake and process math even if you turn off the screen and put the phone in your pocket.
•	Foreground Services: It attaches the LLM server to a persistent Android Notification. This tells the Android OS that the app is performing critical work and should not be killed to free up RAM.

3. Chat Rewinding & Persistence (ChatRepository.kt & ChatScreen.kt)
The chat system is more advanced than a standard text log:
•	Local Storage: All your conversations are saved locally in a JSON repository, complete with "Pinned" states and message history.
•	Time Travel (Rewind): If you ask a question and the AI gives a bad answer, there is an "Edit" button next to your prompt. Clicking it triggers ChatRepository.revertToMessage(), which literally erases the AI's response, puts your text back in the input box, and lets you re-roll the conversation from that exact point in time.

4. True Offline / Air-Gapped Mode (NetworkManager.kt)
The app is designed with absolute privacy in mind.
•	Network Detection: It constantly checks your connection. If you turn on Airplane Mode, the app seamlessly drops into a pure Air-Gapped mode.
•	Behavior Change: It disables the Cloudflare tunnel attempting to connect, shuts down the Real-Time WebSearcher, and relies 100% on the data stored in the local .gguf file.

5. Native Voice Dictation (ChatScreen.kt)
Instead of relying on the keyboard's voice typing, the app has a built-in listener using Android's SpeechRecognizer.
You hold the microphone icon, and it streams your voice directly into the text field using the device's native language models, making it much faster to write massive context prompts.

How Distributed AI (specifically Pipeline Parallelism) works under the hood.

Once the worker nodes are connected, two tensor data transfers happens between the master node and the worker node.
Here is exactly what is happening in both of those situations:

Phase 1: Starting the Server ("Loading LLM into RAM")
What is being sent: The Model Weights (Layers) 
What is happening:  The main node is splitting the LLM into chunks and sending specific layers (tensors containing the neural network's weights) to the worker node over the network.
•	The worker node receives these layers and stores them in its own RAM.
•	This only happens once when the server starts up. It is essentially setting up the "assembly line" across multiple devices.

Phase 2: Sending a Message (Inference)
What is being sent: Intermediate Results (Activations) 
What is happening: When you send a message, it is NOT sending the layers again. The layers are already sitting in the worker node's RAM. Instead, the network is transmitting the actual mathematical data flowing through the AI.

Here is the step-by-step of a single message:
1.	The Main Node converts your text into numbers (tokens) and runs them through its own layers.
2.	Once it finishes its portion, it sends the resulting intermediate tensor over the network to the worker node.
3.	The worker node takes that tensor, runs it through the layers it holds in its RAM, and computes the next step.
4.	The worker node then sends its output tensor back to the main node (or the next worker). This constant back-and-forth stream of numbers happens incredibly fast for every single word (token) the AI generates.

The Assembly Line Analogy
Think of the LLM as a long car assembly line:
•	Starting the server: The factory manager (Main Node) ships heavy robot arms (Layers/Weights) to a new warehouse (Worker Node) and bolts them to the floor. This takes a lot of effort upfront.
•	Sending a message: You put a car frame on the conveyor belt. The first warehouse does some work, ships the half-built car (Activations) to the second warehouse, they finish it, and ship the final car back. The robot arms never move again; only the cars do.
So, the massive stream of data during startup is the model itself, and the stream during chatting is the brainwaves (data) being processed by the model.

Special Settings

1. RSS Source Management (WebSearcher.kt & RssManager.kt)

The app uses a custom-built RSS engine to fetch real-time news and data without relying on a paid API.

How it works:
•	Storage: The RssManager stores a list of RSS feeds (which you can add to or reorder in the app settings) and saves them locally.
•	Concurrent Fetching: When a search is triggered, WebSearcher.rssSearch() loops through your active RSS sources in parallel using Kotlin Coroutines.
•	Query Injection: It takes your search term, encodes it, and injects it into the RSS URL (replacing <query> in the feed link).
•	Regex Parsing: Instead of a heavy XML parser, the app uses lightweight Regex (<item>, <title>, <description>) to extract the headlines and summaries directly from the XML body.
•	Limiting & Formatting: It grabs the top 5 results per source, formats them into a neat string ("Source 1: [Title] \n Summary: [Snippet]"), and compiles a massive block of text containing the latest live news.

2. LLM Context Response Programming (ChatScreen.kt)

This is where the "magic" happens. The app acts as a middleman, secretly manipulating your prompt before the LLM ever sees it.

Here is the exact pipeline in the code:

Step A: The Invisible "Router" LLM Call When you hit send, the app doesn't immediately answer. Instead, it sends an invisible, background request to the LLM with your chat history and a strict instruction:
"If the user's latest message requires searching the internet... reply EXACTLY with 'SEARCH: [query]'. Otherwise, reply EXACTLY with 'CHAT'."

Step B: Fetching Context If the router LLM replies with SEARCH:, the app pauses the chat and passes that query to the WebSearcher (which gathers the Web, Weather, and RSS data mentioned above).

Step C: The Prompt Injection (Context Programming) Once the data is gathered, the app pulls your custom instructions using ContextProgrammingManager.getPromptForQuery(). It then completely overwrites your original message by appending a giant, hidden block of text to the end of it.

In the code, it looks exactly like this:
text
[Your Original Question]
Here is the real-time information you requested from the internet to help answer the question:
[ALL THE RSS/WEB DATA FETCHED]
Please answer my question using only the information provided above.
[YOUR CUSTOM SYSTEM/CONTEXT RULES]

Step D: The Final Send This newly constructed "Mega-Prompt" is then sent to the raw LLM server. Because the LLM is stateless and just follows instructions, it reads the appended data, reads your custom rules, and generates a response that perfectly integrates the live data while strictly adhering to your programming constraints.

Other features

1. The Native Bridge Integration (download_bridges.ps1)
   
To expose your phone to the internet, the app needs to use networking tools like Ngrok or Tailscale. But those are Linux programs, not Android apps.
•	The Method: Android development usually only allows you to package C++ libraries (files ending in .so). The download_bridges.ps1 script downloads the raw Linux ARM64 versions of Ngrok/Tailscale, forcibly renames them from .exe/binary to libngrok.so, and hides them in the jniLibs folder.
•	This method makes Android Studio into packaging them directly inside the app, allowing the app to execute raw Linux binaries as if they were native Android libraries.

2. Large Model Storage Management (ModelDownloader.kt & MainActivity.kt)
   
AI models (.gguf files) are massive, ranging from 1GB to 6GB. Android heavily restricts how much data an app can store in its internal private directory.
•	The Bypass: The code specifically wraps the file paths in a try/catch block that attempts to fetch context.getExternalFilesDir(null) before defaulting to internal storage.
•	This forces the app to save the massive AI models onto the "External Shared Storage" partition (which usually has much more free space), preventing the app from instantly crashing or corrupting the user's phone by maxing out the tiny internal partition.

3. Clickable Source Attributions (ChatScreen.kt)
   
When the WebSearcher fetches data, it doesn't just feed the text to the LLM.
•	It also returns a list of the exact URLs it scraped.
•	In the chat interface, if the LLM's response was generated using real-time data, the app dynamically generates Clickable Chips underneath the chat bubble.
•	It even includes a custom parser for Google News RSS feeds that extracts the original publisher's domain (e.g., stripping away "news.google.com" to show the user the actual news source like "bbc.com"), making the AI highly transparent about where it got its facts.

1. How Many Worker Nodes Can Be Connected?

There is no hardcoded limit. In NodeManager.kt, the list of worker nodes is managed as an infinitely growing list (List<WorkerNode>). You can keep adding IP addresses as long as they are unique. The true limit is based purely on your Wi-Fi network's bandwidth and how many RPC connections the main node can handle before latency becomes an issue.

2. How Memory and Core Pooling Works

Core (CPU) Pooling: The app doesn't technically merge CPU cores into one giant processor. Instead, it works on a "distributed workload" principle:
•	When a worker device starts up, it reads its own threadCount setting and limits the librpc.so engine to only use that many cores.
•	The Main Node (running libllama.so) calculates a combined totalCores metric just for the UI (e.g., Main 4 + Worker 4 = 8 Cores), but under the hood, each device uses its own physical cores to compute only the math it was assigned.

Memory (RAM) Pooling: Memory pooling is handled automatically by the native llama.cpp engine via RPC (Remote Procedure Calls).
•	When you start the Main Node, BinaryRunner.kt grabs all the online worker IPs and passes them to the engine using the command --rpc <Worker1_IP>,<Worker2_IP>.
•	Once the engine boots, it automatically analyzes the RAM across all those devices and physically splits the LLM (tensors/layers) across them. This means a 5GB model could be split across two phones with 3GB of RAM each.

3. How to Customize It
   
You customize these settings directly within the app's User Interface:
•	Customizing Cores: You can adjust the threadCount slider in the app. The app automatically detects your phone's maximum CPU cores (e.g., an 8-core Snapdragon) and restricts the slider so you can't assign more cores than your phone physically has. You must set this slider on each individual phone before starting the engine.
•	Adding Nodes: You customize the cluster by navigating to the "Nodes" or "Cluster" section of the UI and manually typing in the local IP addresses of the worker phones. This calls NodeManager.addNode(ip) and links them to the network.

⚠️ Essential Safety & Technical Warnings

1. Physical & Hardware Risks
•	Thermal Damage: Running LLMs is the most intensive task a mobile CPU can perform. Continuous use can lead to thermal throttling or, in extreme cases, permanent battery degradation.
o	Requirement: Always use active cooling (a fan) if running a cluster node for more than 30 minutes.
•	Battery Lifecycle Stress: Frequent deep-discharge and high-heat cycles will significantly shorten the lifespan of your device's battery.
•	Power Demand: The app’s use of PARTIAL_WAKE_LOCK prevents the phone from sleeping. If not plugged into a wall charger, a flagship phone could go from 100% to 0% in under 3 hours.

2. Security & Network Risks
•	Public Exposure Hazards: Using Cloudflare or Ngrok tunnels "punches a hole" in your local firewall. If your API Key is leaked or if there is a bug in the ApiGatewayServer.kt, a hacker could theoretically access your local network.
•	Unencrypted Internal RPC: Tensors are sent between phones over local Wi-Fi. In its current open-source state, this data is likely unencrypted. Do not use the cluster on public Wi-Fi (like a coffee shop) as your conversation could be intercepted.
•	Malicious Model Weights: Loading custom .gguf models from untrusted sources is risky. While a model file is usually "data," malicious actors can craft files that exploit vulnerabilities in the llama.cpp parser.

3. Privacy & Data Risks
•	Web Scraping Footprint: When using the real-time search/RSS feature, your IP address is exposed to every site the app scrapes (DuckDuckGo, Wikipedia, etc.). To the website, it looks like a bot is visiting from your home IP. Kindly configure in the settings if you prefer to avoid certain sites or to add websites of your choice.
•	Local Data Persistence: The app stores chat history in a local JSON file. If your phone is stolen and not encrypted, your entire conversation history is accessible to the thief.

Screenshots


<img src = "/Screenshot_2026-04-22-16-51-47-199_io.github.shintochakkiath.silicacluster.jpg" width = "250" >   <img src = "/Screenshot_2026-04-22-16-52-00-977_io.github.shintochakkiath.silicacluster.jpg" width = "250" >   <img src = "/Screenshot_2026-04-22-16-52-08-737_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-52-18-494_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-52-26-620_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-52-36-997_io.github.shintochakkiath.silicacluster.jpg" width = "250" >
<img src = "/Screenshot_2026-04-22-16-53-08-133_io.github.shintochakkiath.silicacluster.jpg" width = "250" >



