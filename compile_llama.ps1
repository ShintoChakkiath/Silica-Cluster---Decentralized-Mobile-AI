$ErrorActionPreference = "Stop"
$CMAKE = "C:\Users\Shinto Chakkiath\AppData\Local\Android\Sdk\cmake\3.22.1\bin\cmake.exe"
$NINJA = "C:\Users\Shinto Chakkiath\AppData\Local\Android\Sdk\cmake\3.22.1\bin\ninja.exe"
$TOOLCHAIN = "C:\Users\Shinto Chakkiath\AppData\Local\Android\Sdk\ndk\25.1.8937393\build\cmake\android.toolchain.cmake"

cd C:\ANTIGRAVITYPROJECTS\SilicaCluster

if (-not (Test-Path "llama.cpp")) {
    git clone https://github.com/ggerganov/llama.cpp
}
cd llama.cpp

# Temporarily add ninja to PATH so cmake can find it as the generator
$env:PATH += ";C:\Users\Shinto Chakkiath\AppData\Local\Android\Sdk\cmake\3.22.1\bin"

Write-Output "Configuring CMake for Android arm64-v8a..."
& $CMAKE -B build-android -G Ninja "-DCMAKE_TOOLCHAIN_FILE=$TOOLCHAIN" "-DANDROID_ABI=arm64-v8a" "-DANDROID_PLATFORM=android-28" "-DGGML_RPC=ON" "-DBUILD_SHARED_LIBS=OFF"

Write-Output "Building llama-server and rpc-server (This will take a few minutes)..."
& $CMAKE --build build-android --config Release --target llama-server rpc-server

Write-Output "Packaging binaries into SilicaCluster jniLibs..."
$dest = "C:\ANTIGRAVITYPROJECTS\SilicaCluster\app\src\main\jniLibs\arm64-v8a"
if (-not (Test-Path $dest)) { New-Item -ItemType Directory -Force -Path $dest }
Copy-Item "build-android\bin\llama-server" "$dest\libllama.so" -Force
Copy-Item "build-android\bin\rpc-server" "$dest\librpc.so" -Force

Write-Output "SUCCESS! The native binaries have been successfully compiled and packaged."
