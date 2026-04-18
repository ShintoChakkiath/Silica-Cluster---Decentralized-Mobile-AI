# Building the Native Binaries for Silica Cluster

In order to run the LLM server and Internet Bridges directly on an Android device without rooting, we compile the binaries targeting `arm64-v8a` (often referred to as `aarch64`) and package them into the Android application.

## 1. Compiling Llama.cpp (llama-server & rpc-server)

**Requirements**: Android NDK, CMake, Ninja.

```bash
# Set NDK path
export NDK_PATH=$HOME/Android/Sdk/ndk/<version>
export TOOLCHAIN=$NDK_PATH/build/cmake/android.toolchain.cmake

# Clone llama.cpp
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp

# Configure CMake for Android targeting arm64-v8a
cmake -B build-android -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE=$TOOLCHAIN \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-24 \
  -DGGML_RPC=ON \
  -DBUILD_SHARED_LIBS=OFF

# Build the binaries
cmake --build build-android --config Release --target llama-server rpc-server

# Copy the binaries to the Android project, renaming them as .so so Android Extracts them
cp build-android/bin/llama-server /path/to/SilicaCluster/app/src/main/jniLibs/arm64-v8a/libllama.so
cp build-android/bin/rpc-server /path/to/SilicaCluster/app/src/main/jniLibs/arm64-v8a/librpc.so
```

## 2. Compiling Cloudflared

**Requirements**: Go 1.21+

```bash
# Clone cloudflared
git clone https://github.com/cloudflare/cloudflared.git
cd cloudflared

# Cross-compile for Android arm64
GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build -ldflags="-w -s" -o cloudflared-android ./cmd/cloudflared

# Copy to the Android Project
cp cloudflared-android /path/to/SilicaCluster/app/src/main/jniLibs/arm64-v8a/libcloudflared.so
```

## 3. Compiling Tailscaled

**Requirements**: Go 1.21+

```bash
git clone https://github.com/tailscale/tailscale.git
cd tailscale
GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build -o tailscaled-android ./cmd/tailscaled
cp tailscaled-android /path/to/SilicaCluster/app/src/main/jniLibs/arm64-v8a/libtailscaled.so
```

Once the binaries are placed inside `app/src/main/jniLibs/arm64-v8a/`, the Android packaging system will compress them into the APK. Upon installation, Android will automatically extract `.so` files into the app's secure, executable `nativeLibraryDir` directory.
