# Performance Testing Framework

## Overview
This document outlines automated and manual performance testing procedures for CatAsmr and PawWatch applications.

## Automated Performance Testing

### Android Performance Testing

#### Setup
```bash
# Install Android Profiler
# In Android Studio: View → Tool Windows → Profiler

# Install Firebase Performance Monitoring
# Already integrated in build.gradle
```

#### Memory Profiling

**Idle State Test**
```kotlin
// Expected: < 100 MB
// Steps:
1. Launch app
2. Open Profiler → Memory
3. Wait 30 seconds for GC
4. Record heap size
```

**Streaming Test**
```kotlin
// Expected: < 300 MB
// Duration: 1 hour continuous streaming
// Steps:
1. Start streaming
2. Monitor memory every 5 minutes
3. Check for memory leaks
4. Verify GC collections are normal

// Automated Test Script (Android Test)
@Test
fun memoryLeakTest() {
    // Start streaming
    recordingService.start(context)
    
    // Monitor memory for 1 hour
    for (i in 0..11) {
        Thread.sleep(300000) // 5 minutes
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Assert memory stays below 300MB
        Assert.assertTrue(usedMemory < 300 * 1024 * 1024)
    }
}
```

#### CPU Profiling

**Streaming CPU Usage**
```
Expected: < 40% average
Steps:
1. Start streaming
2. Open Profiler → CPU
3. Record for 5 minutes
4. Check average CPU % in threads tab
```

#### Battery Profiling

**30-Minute Stream Test**
```
Expected: < 15% drain
Steps:
1. Full charge device
2. Start streaming
3. Record battery % every 5 minutes
4. Calculate drain rate
5. Extrapolate to 1-hour usage
```

### iOS Performance Testing

#### Setup
```swift
// Install Instruments (part of Xcode)
// Xcode → Open Developer Tool → Instruments

// Use Xcode Metrics to monitor:
// - CPU
// - Memory
// - Energy Impact
// - Network
```

#### Memory Profiling

```swift
// Expected: < 100 MB idle, < 300 MB streaming
import os.log

func profileMemory() {
    let memoryFormatter = ByteCountFormatter()
    memoryFormatter.allowedUnits = [.MB]
    memoryFormatter.countStyle = .memory
    
    var info = task_vm_info_data_t()
    var count = mach_msg_type_number_t(MemoryLayout<task_vm_info>.size)/4
    
    let kerr = withUnsafeMutablePointer(to: &info) {
        task_info(mach_task_self_,
                  task_flavor_t(TASK_VM_INFO),
                  $0.withMemoryRebound(to: integer_t.self) { $0 },
                  &count)
    }
    
    if kerr == KERN_SUCCESS {
        let memory = Double(info.phys_footprint)
        let memoryString = memoryFormatter.string(fromByteCount: Int64(memory))
        os_log("Memory: %{public}@", log: .default, type: .info, memoryString)
    }
}
```

#### CPU Profiling

```swift
// Expected: < 40% during streaming
func measureCPU() {
    var info = task_basic_info()
    var count = mach_msg_type_number_t(MemoryLayout<task_basic_info>.size)/4
    
    let kerr = withUnsafeMutablePointer(to: &info) {
        task_info(mach_task_self_,
                  task_flavor_t(TASK_BASIC_INFO),
                  $0.withMemoryRebound(to: integer_t.self) { $0 },
                  &count)
    }
    
    if kerr == KERN_SUCCESS {
        let cpuUsage = Double(info.resident_size)
        print("CPU resident size: \(cpuUsage)")
    }
}
```

## Manual Performance Testing

### Test Scenarios

#### Scenario 1: Cold Start
**Objective:** Measure app launch time
**Steps:**
1. Close app completely
2. Open app
3. Time to reach home screen: ______ seconds
4. Target: < 3 seconds
5. Repeat 5 times, average: ______ seconds

#### Scenario 2: QR Code Scan Performance
**Objective:** Measure QR scan detection speed
**Steps:**
1. Open QR Scanner
2. Start timer when camera opens
3. Point at QR code
4. Stop timer on successful scan
5. Time to scan: ______ seconds
6. Target: < 2 seconds
7. Repeat with 10 different QR codes

**Environmental Conditions:**
- [ ] Bright sunlight
- [ ] Indoor artificial light
- [ ] Dim lighting
- [ ] Dark with flashlight
- [ ] Various angles (0°, 45°, 90°)
- [ ] Various distances (6", 12", 24")

#### Scenario 3: Connection Performance
**Objective:** Measure connection establishment time
**Steps:**
1. Start broadcaster app
2. Note time when broadcaster ready
3. Connect from viewer app
4. Time to show video: ______ seconds
5. Target: < 5 seconds
6. Repeat with 5 different connections

#### Scenario 4: Streaming Quality
**Objective:** Measure streaming performance
**Record:**
- [ ] Average FPS: ______ fps (Target: 30)
- [ ] Dropped frames per minute: ______ (Target: < 1)
- [ ] Average latency: ______ ms (Target: < 500ms)
- [ ] Jitter: ______ ms (Target: < 100ms)

#### Scenario 5: Network Resilience
**Objective:** Test handling of network issues
**Steps:**

Network Dropout Test:
1. Start streaming
2. Disconnect WiFi
3. Time to error message: ______ seconds
4. Message clarity: ______ (1-5 scale)
5. Reconnect WiFi
6. Auto-reconnect works: [ ] Yes [ ] No
7. Time to reconnect: ______ seconds

Slow Network Test:
1. Enable throttling (2G, 3G, 4G)
2. Attempt streaming
3. Quality degrades gracefully: [ ] Yes [ ] No
4. Buffering happens smoothly: [ ] Yes [ ] No
5. User can pause/resume: [ ] Yes [ ] No

#### Scenario 6: Extended Usage
**Objective:** Test app stability during long sessions
**Duration:** 2+ hours
**Monitor:**
- [ ] No crashes: [ ] Yes [ ] No
- [ ] No frame freezes: [ ] Yes [ ] No
- [ ] Memory remains stable: [ ] Yes [ ] No
- [ ] Battery drain rate: ______ % per 30 min
- [ ] Temperature: Normal [ ] Warm [ ] Hot
- [ ] App responsiveness: [ ] Good [ ] Fair [ ] Poor

## Benchmarking Results

### Android Results
| Test | Device | Date | Result | Target | Status |
|------|--------|------|--------|--------|--------|
| Cold Start | Pixel 6 | 2026-08-03 | | < 3s | [ ] |
| QR Scan | Pixel 6 | 2026-08-03 | | < 2s | [ ] |
| Connection | Pixel 6 | 2026-08-03 | | < 5s | [ ] |
| Memory (idle) | Pixel 6 | 2026-08-03 | | < 100MB | [ ] |
| Memory (stream) | Pixel 6 | 2026-08-03 | | < 300MB | [ ] |
| CPU (stream) | Pixel 6 | 2026-08-03 | | < 40% | [ ] |
| Battery (30min) | Pixel 6 | 2026-08-03 | | < 15% | [ ] |

### iOS Results
| Test | Device | Date | Result | Target | Status |
|------|--------|------|--------|--------|--------|
| Cold Start | iPhone 14 | 2026-08-03 | | < 3s | [ ] |
| QR Scan | iPhone 14 | 2026-08-03 | | < 2s | [ ] |
| Connection | iPhone 14 | 2026-08-03 | | < 5s | [ ] |
| Memory (idle) | iPhone 14 | 2026-08-03 | | < 100MB | [ ] |
| Memory (stream) | iPhone 14 | 2026-08-03 | | < 300MB | [ ] |
| CPU (stream) | iPhone 14 | 2026-08-03 | | < 40% | [ ] |
| Battery (30min) | iPhone 14 | 2026-08-03 | | < 15% | [ ] |

## Regression Testing

### After Each Build
```bash
# Run performance baseline
./gradlew test --tests "*Performance*"

# Compare against previous results
# Alert if > 10% regression
```

### Weekly Performance Report
- [ ] Compile all benchmark results
- [ ] Calculate trends
- [ ] Identify regressions
- [ ] Assign for investigation
- [ ] Document findings

## Performance Optimization Checklist

### Code Level
- [ ] Remove unused imports
- [ ] Eliminate redundant loops
- [ ] Cache expensive calculations
- [ ] Use appropriate data structures
- [ ] Profile hot spots
- [ ] Lazy load heavy resources

### Resource Level
- [ ] Compress images (WebP format)
- [ ] Lazy load videos
- [ ] Use appropriate codec settings
- [ ] Clean up temporary files
- [ ] Optimize database queries
- [ ] Use connection pooling

### Platform Level
- [ ] Android: Use coroutines over threads
- [ ] iOS: Use async/await
- [ ] Both: Use background threads appropriately
- [ ] Both: Avoid main thread blocking
- [ ] Both: Profile with official tools

## Tools & Resources

### Android
- Android Profiler (built-in to Android Studio)
- Firebase Performance Monitoring
- Perfetto (trace viewing)
- LeakCanary (memory leak detection)
- Benchmark Library

### iOS
- Instruments (Xcode)
- Xcode Metrics
- SwiftUI performance tools
- Network Link Conditioner

### Load Testing
- JMeter
- Apache Bench
- Locust

## Sign-Off

Performance testing completed: ______ (date)
Tested by: ______ (name)
Results reviewed by: ______ (name)
All targets met: [ ] Yes [ ] No
Issues documented: [ ] Yes [ ] No

