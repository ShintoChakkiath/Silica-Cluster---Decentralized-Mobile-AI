$ErrorActionPreference = "Stop"

# Download Ngrok linux-arm64
Write-Output "Downloading Ngrok..."
Invoke-WebRequest -Uri "https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-arm64.tgz" -OutFile "ngrok.tgz"
tar -xzf ngrok.tgz
Copy-Item "ngrok" "app\src\main\jniLibs\arm64-v8a\libngrok.so" -Force
Remove-Item "ngrok" -ErrorAction SilentlyContinue
Remove-Item "ngrok.tgz" -ErrorAction SilentlyContinue

# Download Tailscale linux-arm64
Write-Output "Downloading Tailscale..."
Invoke-WebRequest -Uri "https://pkgs.tailscale.com/stable/tailscale_1.60.1_arm64.tgz" -OutFile "tailscale.tgz"
tar -xzf tailscale.tgz
Copy-Item "tailscale_1.60.1_arm64\tailscaled" "app\src\main\jniLibs\arm64-v8a\libtailscaled.so" -Force
Remove-Item -Recurse -Force "tailscale_1.60.1_arm64" -ErrorAction SilentlyContinue
Remove-Item "tailscale.tgz" -ErrorAction SilentlyContinue

Write-Output "Successfully downloaded and packaged Ngrok and Tailscaled binaries!"
