#!/bin/bash

echo "=== Checking WebSocket logs ==="
echo ""
echo "Run these commands in separate terminals:"
echo ""
echo "1. Server logs:"
echo "   cd server && ./gradlew run 2>&1 | grep -E 'WebSocket|broadcast|ParticipantJoined'"
echo ""
echo "2. Android logcat (on device):"
echo "   adb logcat | grep -E 'WebSocket|RoomViewModel|Received.*message|Parsed.*event'"
echo ""
