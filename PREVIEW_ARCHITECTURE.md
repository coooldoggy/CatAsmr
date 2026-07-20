╔══════════════════════════════════════════════════════════════════════════════╗
║                    STREAM PREVIEW ARCHITECTURE BREAKDOWN                     ║
╚══════════════════════════════════════════════════════════════════════════════╝

📱 DEVICE A (Broadcasting)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Camera Capture:
  ┌────────────────────────────────┐
  │ Android Camera Hardware        │
  │ (device's physical camera)     │
  └────────────┬───────────────────┘
               │
               ↓
  ┌────────────────────────────────────────┐
  │ CameraX Library (Jetpack)              │
  │ - androidx.camera:camera-camera2       │
  │ - androidx.camera:camera-lifecycle     │
  │ - androidx.camera:camera-video         │
  └────────────┬───────────────────────────┘
               │
               ↓ (YUV frames)
  ┌────────────────────────────────────────┐
  │ StreamingFrameAnalyzer                 │
  │ - Converts YUV → JPEG                  │
  │ - ~10 FPS throttling                   │
  │ - 60% quality compression              │
  └────────────┬───────────────────────────┘
               │
               ↓ (JPEG bytes)
  ┌────────────────────────────────────────┐
  │ LocalStreamingServer (Custom)          │
  │ - HTTP Server on port 8888             │
  │ - Multipart/x-mixed-replace protocol   │
  │ - Broadcasts to all connected clients  │
  └────────────┬───────────────────────────┘
               │
               ↓ (HTTP stream)
          Network (WiFi)
               ↓
               
📱 DEVICE B (Viewing - LOCAL PREVIEW)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Stream Reception (NO camera API involved):
  ┌────────────────────────────────────┐
  │ Socket/HTTP Client (java.net)      │ ← NOT a camera library
  │ - Connects to 127.0.0.1:8888       │   Just network socket
  └────────────┬───────────────────────┘
               │
               ↓ (HTTP multipart stream)
  ┌────────────────────────────────────┐
  │ RemoteViewerViewModel              │
  │ - Parses HTTP headers              │
  │ - Extracts Content-Length          │
  │ - Collects JPEG frame bytes        │
  └────────────┬───────────────────────┘
               │
               ↓ (JPEG byte array)
  ┌────────────────────────────────────┐
  │ BitmapFactory (Android Framework)  │ ← NOT a camera library
  │ - Decodes JPEG → Bitmap            │   Just image decoder
  │ - DecodeByteArray()                │
  └────────────┬───────────────────────┘
               │
               ↓ (Bitmap)
  ┌────────────────────────────────────┐
  │ RemoteViewerScreen (Compose)       │
  │ - Image composable                 │
  │ - Displays bitmap                  │
  │ - Shows status indicator           │
  └────────────────────────────────────┘
               │
               ↓
          📱 Screen Display


KEY POINT: Preview uses NETWORK, not CAMERA API
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Libraries Used:

Camera (Device A Only):
  ✅ androidx.camera:camera-camera2 (CameraX)
  ✅ androidx.camera:camera-lifecycle
  ✅ androidx.camera:camera-video
  Purpose: Capture frames from physical camera

Streaming (Device A):
  ✅ java.net.Socket (Standard Java)
  ✅ java.net.ServerSocket (Standard Java)
  ✅ Custom LocalStreamingServer
  Purpose: Broadcast JPEG stream over HTTP

Preview (Device B):
  ✅ java.net.Socket (Standard Java)
  ✅ android.graphics.BitmapFactory (Android Framework)
  ✅ androidx.compose.foundation.Image (Compose)
  Purpose: Receive JPEG stream and display
  
  ❌ NO camera API used here
  ❌ NOT accessing device camera
  ❌ Just receiving network data


Architecture Summary:

Device A: Camera → CameraX → JPEG Encoder → HTTP Server
                                               │
                                               └→ Network Stream
                                                       ↓
Device B: HTTP Client → JPEG Decoder → Image Display

The preview on Device B is basically a "network viewer" not a "camera viewer"


Data Flow Example:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. User taps "Start watching now" on Device A
   ↓
2. RecordingService starts
   ↓
3. CameraX captures YUV frame from camera hardware
   ↓
4. StreamingFrameAnalyzer converts YUV → JPEG (12KB)
   ↓
5. LocalStreamingServer sends HTTP response:
   ───────────────────────────────────────
   Content-Length: 12000
   Content-Type: image/jpeg
   
   [12000 bytes of JPEG data]
   ───────────────────────────────────────
   ↓
6. Device B receives HTTP stream
   ↓
7. RemoteViewerViewModel parses Content-Length
   ↓
8. Extracts 12000 bytes of JPEG data
   ↓
9. BitmapFactory.decodeByteArray() → creates Bitmap
   ↓
10. RemoteViewerScreen displays Bitmap on UI
   ↓
11. ~10 FPS cycle repeats

NO camera API involved in steps 6-11!


Code References:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Camera Capture (Device A):
  📄 CameraController.kt
  📄 RecordingService.kt
  📄 StreamingFrameAnalyzer.kt
  Uses: CameraX (androidx.camera.*)

Streaming Server (Device A):
  📄 LocalStreamingServer.kt
  Uses: java.net.Socket, java.net.ServerSocket

Preview Viewer (Device B):
  📄 RemoteViewerViewModel.kt
  📄 RemoteViewerScreen.kt
  Uses: java.net.Socket, BitmapFactory, Compose Image
  
  ← NO Camera API here


Alternative: Could We Use Camera API for Preview?
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

NO - Because:
  ❌ Remote device doesn't have access to local camera
  ❌ Camera API only accesses local device's camera
  ❌ Can't control another device's camera over network
  
Only way to view: Receive frames over network (what we do)

┌─────────────────────────────────────────────────┐
│ Network Streaming is the ONLY way to view       │
│ a remote device's camera                        │
└─────────────────────────────────────────────────┘


Summary:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Q: "Is preview made of library?"

A: PARTIALLY
  ✅ YES - CameraX library captures frames on Device A
  ✅ YES - Android BitmapFactory decodes JPEGs on Device B
  ❌ NO - Socket/HTTP is standard Java, not a library
  ❌ NO - Our streaming server is custom code
  ❌ NO - NO camera API used for preview display

The preview works through NETWORK STREAMING, not camera API.
