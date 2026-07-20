# Preview Implementation - Code Breakdown

## Device A (Broadcasting) - Uses Camera Library

### 1. Camera Capture (CameraController.kt)
```kotlin
// Uses CameraX library to capture frames
val analysis = ImageAnalysis.Builder()
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
analysis.setAnalyzer(analysisExecutor, streamingAnalyzer)  // Pass to analyzer

// CameraX handles actual camera hardware interaction
provider.bindToLifecycle(
    lifecycleOwner,
    CameraSelector.DEFAULT_BACK_CAMERA,
    analysis  // ← CameraX library
)
```
**Library:** androidx.camera:camera-camera2 ✅

---

### 2. Frame Encoding (StreamingFrameAnalyzer.kt)
```kotlin
// Converts camera YUV frames to JPEG
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return when (image.format) {
        ImageFormat.YUV_420_888 -> yuvToRgb(image)
        ImageFormat.NV21 -> nv21ToRgb(image)
        else -> null
    }
}

// Compress to JPEG
private fun compressBitmapToJpeg(bitmap: Bitmap, quality: Int = 60): ByteArray {
    return ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
}
```
**Library:** android.graphics.Bitmap (Android Framework) ✅

---

### 3. Streaming Server (LocalStreamingServer.kt)
```kotlin
// Custom HTTP server - NOT a library
class LocalStreamingServer(context: Context, val port: Int = 8888) {
    fun start() {
        val serverSocket = ServerSocket(port)  // ← java.net (Standard Java)
        
        while (isRunning) {
            val clientSocket = serverSocket.accept()
            // Handle client connection
            val out = clientSocket.getOutputStream()
            out.write("HTTP/1.1 200 OK\r\n".toByteArray())
            out.write("Content-Type: multipart/x-mixed-replace\r\n".toByteArray())
        }
    }
    
    fun broadcastFrame(frameData: ByteArray, width: Int, height: Int) {
        // Send to all connected clients over HTTP
        clients.forEach { client ->
            client.getOutputStream().write(frameData)
        }
    }
}
```
**Libraries:** java.net.ServerSocket, java.net.Socket (Standard Java - NOT a camera library) ❌

---

## Device B (Viewing) - NO Camera API

### 1. Stream Connection (RemoteViewerViewModel.kt)
```kotlin
fun connectToDevice(deviceName: String, ipAddress: String, port: Int = 8888) {
    // ← Only uses Socket, NOT Camera API
    streamSocket = Socket(ipAddress, port)  // ← java.net.Socket
    
    val request = "GET /stream HTTP/1.1\r\nHost: $ipAddress\r\n\r\n"
    socket.getOutputStream().write(request.toByteArray())
    
    // Read HTTP response
    val input = BufferedInputStream(socket.getInputStream())
}
```
**Libraries:** java.net.Socket (Standard Java - NOT camera) ❌

---

### 2. Frame Decoding (RemoteViewerViewModel.kt)
```kotlin
// Parse HTTP response and extract JPEG frames
while (input.read(buffer).also { bytesRead = it } != -1) {
    // Look for Content-Length header
    if (line.startsWith("Content-Length:")) {
        contentLength = line.substringAfter(":").trim().toInt()
    }
    
    // Extract exact JPEG bytes
    buffer.copyInto(frameBuffer, bytesOfCurrentFrame, pos, pos + toCopy)
    
    // Decode when frame complete
    if (bytesOfCurrentFrame == contentLength) {
        val bitmap = BitmapFactory.decodeByteArray(
            frameBuffer,  // ← Just decode JPEG bytes
            0,
            bytesOfCurrentFrame
        )
        _streamingState.value = StreamingState.Connected(bitmap, 10)
    }
}
```
**Libraries:** 
- android.graphics.BitmapFactory (Android Framework - NOT Camera API)
- java.net.Socket (Standard Java - NOT Camera API)

---

### 3. Display (RemoteViewerScreen.kt)
```kotlin
@Composable
fun RemoteViewerScreen(
    deviceName: String,
    streamingState: StateFlow<StreamingState>,
    onClose: () -> Unit,
) {
    val state by streamingState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is StreamingState.Connected -> {
                val frame = (state as StreamingState.Connected).currentFrame
                Image(
                    bitmap = frame.asImageBitmap(),  // ← Display bitmap
                    contentDescription = "Camera feed",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
```
**Libraries:** 
- androidx.compose.foundation.Image (Compose - NOT Camera API)
- Nothing else needed!

---

## Summary Table

| Component | Library | Used For | Camera API? |
|-----------|---------|----------|------------|
| **CameraX** | androidx.camera:camera-camera2 | Capture camera frames | ✅ YES |
| **Frame Encoding** | android.graphics.Bitmap | YUV→JPEG conversion | ❌ NO |
| **Streaming** | java.net.Socket | Send/receive over network | ❌ NO |
| **Frame Decoding** | android.graphics.BitmapFactory | JPEG→Bitmap | ❌ NO |
| **Display** | androidx.compose.foundation.Image | Show bitmap on screen | ❌ NO |

---

## The Key Point

```
DEVICE A (Broadcasting):
  Camera Hardware → [CameraX Library] → JPEG → [LocalStreamingServer] → Network

DEVICE B (Viewing Preview):
  Network → [Socket] → [BitmapFactory] → [Compose Image] → Display
  
  ↑ NO CAMERA API USED HERE ↑
```

**The preview is NOT a camera viewer, it's a NETWORK IMAGE VIEWER.**

It receives JPEG frames (already compressed images) over HTTP, decodes them, and displays them. It never touches the device's camera hardware.
