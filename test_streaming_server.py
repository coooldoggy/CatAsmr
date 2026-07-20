#!/usr/bin/env python3
"""
Test script for CatAsmr streaming server.
Simulates a remote viewer connecting to the streaming server.

Usage:
    python3 test_streaming_server.py <device_ip>
    Example: python3 test_streaming_server.py 192.168.1.100
"""

import socket
import sys
import time
from datetime import datetime

def test_streaming_connection(host, port=8888, timeout=10):
    """
    Test connection to streaming server and receive frames.
    """
    print(f"\n🎬 CatAsmr Streaming Server Test")
    print(f"{'=' * 50}")
    print(f"Target: {host}:{port}")
    print(f"Timeout: {timeout}s")
    print(f"{'=' * 50}\n")

    try:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Connecting to streaming server...")

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        sock.connect((host, port))

        print(f"✅ Connected successfully!")

        # Send HTTP GET request for stream
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Requesting stream...")
        request = "GET /stream HTTP/1.1\r\nHost: {}\r\nConnection: close\r\n\r\n".format(host)
        sock.sendall(request.encode())

        # Read HTTP response
        print(f"[{datetime.now().strftime('%H:%M:%S')}] Reading response header...")
        response = b""
        while b"\r\n\r\n" not in response:
            data = sock.recv(1024)
            if not data:
                break
            response += data

        response_str = response.decode('utf-8', errors='ignore')
        header_end = response_str.find("\r\n\r\n")
        header = response_str[:header_end]

        print("\n📊 Server Response:")
        for line in header.split("\r\n")[:5]:
            if line:
                print(f"  {line}")

        # Check for multipart JPEG stream
        if "multipart/x-mixed-replace" in header:
            print("\n✅ Server is streaming multipart JPEG frames")
        else:
            print("\n⚠️  Response header doesn't indicate multipart stream")

        # Try to receive frames
        print(f"\n[{datetime.now().strftime('%H:%M:%S')}] Listening for frames...")
        frame_count = 0
        frame_sizes = []
        start_time = time.time()

        while time.time() - start_time < 5:  # Listen for 5 seconds
            try:
                chunk = sock.recv(4096)
                if not chunk:
                    break

                # Look for JPEG frame markers
                if b"\xff\xd8" in chunk:  # JPEG SOI
                    frame_count += 1
                    frame_sizes.append(len(chunk))
                    if frame_count == 1:
                        print(f"  Frame {frame_count}: {len(chunk)} bytes")
                    elif frame_count % 3 == 0:
                        avg_size = sum(frame_sizes[-3:]) // 3
                        print(f"  Frame {frame_count}: ~{avg_size} bytes avg")

            except socket.timeout:
                break

        elapsed = time.time() - start_time

        print(f"\n📈 Frame Statistics:")
        print(f"  Frames received: {frame_count}")
        print(f"  Duration: {elapsed:.1f}s")
        if frame_count > 0:
            fps = frame_count / elapsed
            avg_size = sum(frame_sizes) // len(frame_sizes)
            print(f"  Frame rate: {fps:.1f} FPS")
            print(f"  Avg frame size: {avg_size} bytes")
            bandwidth = (sum(frame_sizes) / (1024 * 1024)) / elapsed
            print(f"  Bandwidth: {bandwidth:.2f} MB/s")

        sock.close()

        # Summary
        print(f"\n{'=' * 50}")
        if frame_count > 0:
            print(f"✅ Streaming test PASSED")
            print(f"   Server is actively broadcasting frames")
        else:
            print(f"⚠️  No frames received")
            print(f"   Check if recording is active on the device")
        print(f"{'=' * 50}\n")

        return frame_count > 0

    except socket.timeout:
        print(f"❌ Connection timeout - server not responding")
        print(f"   Make sure:")
        print(f"   1. Device is on same WiFi network")
        print(f"   2. Recording is active (tap 'Start watching')")
        print(f"   3. Firewall allows port 8888")
        return False
    except ConnectionRefusedError:
        print(f"❌ Connection refused - server not listening")
        print(f"   Make sure CatAsmr is running and recording")
        return False
    except OSError as e:
        print(f"❌ Connection error: {e}")
        print(f"   Check IP address and network connection")
        return False

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 test_streaming_server.py <device_ip>")
        print("Example: python3 test_streaming_server.py 192.168.1.100")
        print("\nTo find your device IP:")
        print("  adb shell ip addr show | grep 'inet '")
        sys.exit(1)

    device_ip = sys.argv[1]
    success = test_streaming_connection(device_ip)
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
