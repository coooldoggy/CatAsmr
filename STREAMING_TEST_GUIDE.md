# CatAsmr Streaming Feature - Test Guide

## 🎬 Testing Overview

This guide walks you through testing the live camera streaming feature on two Android devices.

---

## ✅ Prerequisites

- **Device 1 (Main)**: Running CatAsmr app - will broadcast camera feed
- **Device 2 (Remote)**: Running CatAsmr app - will view the stream
- **Network**: Both devices connected to same WiFi network
- **Permissions**: Camera and Network permissions granted

---

## 🧪 Test Plan

### Test 1: Single Device Streaming Server

**Objective**: Verify streaming server starts and listens on port 8888

**Steps**:
1. Open CatAsmr on Device 1
2. Grant camera and microphone permissions
3. Tap "Start watching now" button
4. **Expected**: Foreground notification shows "Recording your cat…"

**Verification**:
```bash
# From your development machine
adb shell netstat -an | grep 8888
# Should show: LISTEN port 8888
```

---

### Test 2: Frame Capture & Compression

**Objective**: Verify frames are being captured and compressed

**Steps**:
1. Keep Device 1 recording (from Test 1)
2. Point camera at well-lit scene (face, object)
3. Watch for 10 seconds

**Verification**:
- Check logcat for streaming logs:
```bash
adb logcat | grep "StreamingFrame\|LocalStreaming"
# Should show frame processing logs every ~100ms
```

---

### Test 3: Local Network Connection (Same WiFi)

**Objective**: Connect from Device 2 to Device 1's stream

**Steps**:
1. **Device 1**: Keep recording (streaming server active)
2. **Device 2**: Open CatAsmr
3. **Device 2**: Go to Settings → Connect to Camera
4. **Device 2**: Enter pairing code from Device 1
5. **Device 2**: Wait for connection (should show "Connecting...")

**Expected**: 
- ✅ Connection status changes to "Connected"
- ✅ Live camera frames appear
- ✅ ~10 FPS video stream

**Verification**:
- Frames update smoothly
- No lag/stuttering
- Video quality clear

---

### Test 4: Real-Time Feed Quality

**Objective**: Verify streaming quality and latency

**Setup**:
1. **Device 1**: Point camera at moving object (hand waving)
2. **Device 2**: Watch stream in RemoteViewerScreen
3. Measure latency: Wave hand, count delay on Device 2

**Expected Results**:
- Latency: 100-500ms (WiFi-based)
- Resolution: 640x480 JPEG
- Frame rate: ~10 FPS
- Quality: Clear enough to identify motion

**Quality Checklist**:
- ✅ Frames display correctly
- ✅ Motion is smooth (not jerky)
- ✅ Colors are accurate
- ✅ No missing frames

---

### Test 5: Multiple Simultaneous Viewers

**Objective**: Verify multiple devices can watch simultaneously

**Setup**:
1. **Device 1**: Recording (streaming active)
2. **Device 2**: Connected and watching
3. **Device 3**: Also connect to same stream
4. **Device 4**: Try to connect

**Expected**:
- ✅ Devices 2, 3, 4 all receive live feed
- ✅ No performance degradation
- ✅ All get same frames

**Verification**:
```bash
# Check connected clients on Device 1
adb shell logcat | grep "Client connected"
```

---

### Test 6: Connection Stability

**Objective**: Test graceful handling of disconnections

**Scenario A: Network Interruption**
1. **Device 1**: Recording/streaming
2. **Device 2**: Connected and watching
3. WiFi: Briefly disable WiFi on Device 2
4. Re-enable WiFi after 5 seconds

**Expected**:
- ✅ Device 2 shows "Disconnected" → "Reconnecting..." → "Connected"
- ✅ Stream resumes automatically
- ✅ No crashes

**Scenario B: Stop Recording**
1. **Device 1**: Recording/streaming
2. **Device 2**: Watching stream
3. **Device 1**: Tap "Stop watching"

**Expected**:
- ✅ Streaming server stops
- ✅ Device 2 shows "Disconnected"
- ✅ No error messages

---

### Test 7: Device Pairing

**Objective**: Verify 6-digit pairing code works

**Setup**:
1. **Device 1**: Start watching (generates pairing code)
2. **Device 1**: Note the 6-digit code shown

**Steps**:
1. **Device 2**: Settings → Connect to Camera
2. **Device 2**: Tap "Enter pairing code"
3. **Device 2**: Type the 6-digit code
4. **Device 2**: Tap "Connect"

**Expected**:
- ✅ Pairing succeeds
- ✅ Device 2 connects to stream
- ✅ Device 1 remembers Device 2 as paired

---

### Test 8: Performance & Battery

**Objective**: Monitor resource usage during streaming

**Steps**:
1. Start streaming on Device 1
2. Keep streaming for 5 minutes
3. Monitor metrics:

```bash
# Check CPU usage
adb shell top -n 1 | grep catasmr

# Check memory
adb shell dumpsys meminfo | grep catasmr

# Check temperature (if available)
adb shell cat /sys/class/thermal/thermal_zone0/temp
```

**Expected**:
- CPU: <50% average
- Memory: <200MB
- Temperature: No significant increase
- Battery: Normal drain rate

---

### Test 9: Error Scenarios

**Scenario A: Camera Permission Denied**
1. Revoke camera permission
2. Try to start watching

**Expected**:
- ✅ Service stops gracefully
- ✅ Error message shown
- ✅ No crashes

**Scenario B: Invalid Pairing Code**
1. Enter wrong 6-digit code

**Expected**:
- ✅ "Invalid code" error
- ✅ Can retry with correct code

**Scenario C: Out of Range WiFi**
1. Device moves out of WiFi range

**Expected**:
- ✅ Connection drops gracefully
- ✅ Shows "Disconnected" status
- ✅ Can reconnect when in range

---

## 📊 Test Results Template

```
Date: ____
Device 1 Model: ____
Device 2 Model: ____
Android Versions: ____
WiFi: ____

Test 1 - Streaming Server: ✅ / ❌
Test 2 - Frame Capture: ✅ / ❌
Test 3 - Local Connection: ✅ / ❌
Test 4 - Feed Quality: ✅ / ❌
Test 5 - Multiple Viewers: ✅ / ❌
Test 6 - Connection Stability: ✅ / ❌
Test 7 - Device Pairing: ✅ / ❌
Test 8 - Performance: ✅ / ❌
Test 9 - Error Scenarios: ✅ / ❌

Issues Found:
- 
- 

Notes:
-
-
```

---

## 🛠️ Debugging Tips

### Enable Verbose Logging
```bash
adb logcat | grep -E "Streaming|LocalStreaming|CameraStreaming"
```

### Monitor Network Traffic
```bash
# On Device 1 (if adb reverse tunnel set up)
nc -l 8888  # Listen on streaming port
```

### Check Server Port
```bash
adb shell netstat -an | grep 8888
# Look for: tcp    0    0 0.0.0.0:8888    0.0.0.0:*    LISTEN
```

### View Connection Logs
```bash
adb logcat -s "LocalStreamingServer:E"
```

---

## 📝 Known Limitations

1. **WiFi Only**: Local streaming requires both devices on same WiFi
2. **Frame Rate**: ~10 FPS (optimized for battery)
3. **Resolution**: 640x480 (lower than recording)
4. **Latency**: 100-500ms depending on WiFi

---

## ✨ Next Steps After Testing

If all tests pass:
- ✅ Feature is production-ready
- ✅ Can ship with Phase 5 streaming
- ✅ Consider adding cloud backup later

If issues found:
- Debug using logs above
- Check WiFi connectivity
- Verify camera permissions
- Review firewall rules
