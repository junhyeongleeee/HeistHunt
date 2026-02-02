# Location Tracking System - Testing Guide

## Quick Start Testing

### Prerequisites
1. At least 2 Android devices (or 1 device + 1 emulator)
2. Server running on your local network
3. Both devices on the same network
4. Location services enabled on both devices

### Test Scenario 1: Basic Location Tracking (Single Device)

**Steps:**
1. Start the server
2. Launch the app on Device A
3. Create a room
4. Start the game (minimum 2 players needed - use Device B to join first)
5. **Observe:**
   - Location permission dialog appears
   - Grant permission
   - Check logcat for location updates every 5 seconds:
     ```
     Location updated: lat=37.7749, lon=-122.4194
     Sent location update: lat=37.7749, lon=-122.4194
     ```

**Server Logs to Check:**
```
WebSocket connected: gameId=xxx, userId=yyy, role=POLICE
Location updated: gameId=xxx, userId=yyy, lat=37.7749, lon=-122.4194
Broadcasted 1 locations to user zzz (role: POLICE)
```

**Database Verification:**
```sql
SELECT * FROM location_updates WHERE game_id = 'xxx' ORDER BY timestamp DESC LIMIT 10;
```

### Test Scenario 2: Multi-Device Location Sharing

**Setup:**
- Device A: Police
- Device B: Thief

**Steps:**
1. Device A creates room
2. Device B scans QR code to join
3. Both select roles and ready up
4. Host starts game
5. **Expected Behavior:**

**On Device A (Police):**
- See own location marker
- See Device B's location marker (thief)
- Both markers update in real-time

**On Device B (Thief):**
- See own location marker
- DO NOT see Device A's location marker (police are hidden)

**Logs to Verify:**
```
# Device A (Police)
Received Game WebSocket message: {"type":"PlayerLocations","locations":[...]}
Received 2 player locations  # Both police and thief

# Device B (Thief)
Received Game WebSocket message: {"type":"PlayerLocations","locations":[...]}
Received 1 player locations  # Only thief (self)
```

### Test Scenario 3: Three Players (Role Filtering)

**Setup:**
- Device A: Police
- Device B: Thief 1
- Device C: Thief 2

**Expected Results:**

| Device | Can See |
|--------|---------|
| A (Police) | A, B, C (all players) |
| B (Thief 1) | B, C (only thieves) |
| C (Thief 2) | B, C (only thieves) |

**Test Steps:**
1. All three join same game
2. A selects Police, B and C select Thief
3. Start game
4. Move Device B physically
5. **Observe:**
   - Device A sees B's marker move
   - Device C sees B's marker move
   - Devices B and C do NOT see A's position

## Testing Permission Handling

### Test 1: Permission Denial
1. Start game
2. Deny location permission
3. **Expected:** Rationale dialog appears
4. Click "Cancel"
5. **Expected:** Game shows error "Location services are disabled"

### Test 2: Open Settings
1. Deny permission (see Test 1)
2. Click "Open Settings" in rationale dialog
3. **Expected:** Android Settings app opens to app permissions
4. Grant location permission
5. Return to app
6. **Expected:** Permission re-requested on next game start

### Test 3: Permission While App Backgrounded
1. Start game with location tracking active
2. Press Home button (background app)
3. Wait 30 seconds
4. Return to app
5. **Expected:** Location tracking continues (check logs for continuous updates)

## Testing Error Conditions

### Test 1: GPS Disabled
**Steps:**
1. Start game with permission granted
2. Open Quick Settings
3. Disable Location services
4. **Expected:**
   - Error message: "GPS signal lost - move to an outdoor area"
   - Last known location retained
5. Re-enable Location services
6. **Expected:** Tracking resumes within 5-10 seconds

### Test 2: Network Disconnection
**Steps:**
1. Start game with active location tracking
2. Enable Airplane mode
3. **Expected:**
   - WebSocket disconnects
   - Connection state shows "ERROR" or "DISCONNECTED"
   - Locations buffered locally
4. Disable Airplane mode
5. **Expected:**
   - WebSocket reconnects
   - Buffered locations sent to server

### Test 3: Server Crash
**Steps:**
1. Start game with multiple devices
2. Stop the server (Ctrl+C)
3. **Expected:**
   - All clients show disconnected state
   - UI remains responsive
   - No crashes
4. Restart server
5. **Expected:**
   - Clients attempt reconnection
   - Game state may be lost (expected behavior)

## Testing Game Lifecycle

### Test 1: Game End
**Steps:**
1. Start game
2. Wait for timer to reach 0:00 (or manually end game via database)
3. **Expected:**
   - `GameEnded` event received
   - Location tracking stops automatically
   - "Game Over!" message displayed

### Test 2: Leave Game Early
**Steps:**
1. Start game
2. Press back button
3. Confirm leave
4. **Expected:**
   - Location tracking stops
   - WebSocket disconnects
   - User returned to main menu
   - Server removes user from game session

### Test 3: App Force Close
**Steps:**
1. Start game with location tracking
2. Force close app (swipe away from recents)
3. **Expected:**
   - Server detects disconnection
   - User removed from active game sessions
4. Reopen app
5. **Expected:**
   - Clean state, no lingering game session

## Performance Testing

### Battery Impact Test
**Duration:** 1 hour

**Steps:**
1. Fully charge device
2. Start game with location tracking
3. Note battery percentage
4. Wait 1 hour
5. Note battery percentage
6. **Expected:** ~5-10% battery drain

### Network Usage Test
**Tools:** Android Profiler or NetMonitor

**Steps:**
1. Start game
2. Monitor network traffic for 5 minutes
3. **Expected:**
   - ~12 requests per minute (5-second intervals)
   - ~6 KB per minute
   - No memory leaks in Network Profiler

### CPU/Memory Test
**Tools:** Android Studio Profiler

**Steps:**
1. Start game
2. Run for 10 minutes
3. Monitor CPU and Memory
4. **Expected:**
   - CPU: <5% average
   - Memory: Stable (no continuous growth)
   - No ANR (Application Not Responding)

## Debugging Tips

### Enable Verbose Logging
Search logcat for these tags:
```
Game WebSocket
Location updated
Received WebSocket message
WebSocket connected
Broadcasted
```

### Filter by Package
```
adb logcat | grep "com.heisthunt.app"
```

### Check Server Logs
Watch for these patterns:
```
WebSocket connected: gameId=xxx, userId=yyy, role=POLICE
Location updated: gameId=xxx, userId=yyy, lat=x.xxx, lon=y.yyy
Broadcasted N locations to user zzz (role: ROLE)
```

### Verify Database
```sql
-- Check recent locations
SELECT
    game_id,
    user_id,
    latitude,
    longitude,
    timestamp
FROM location_updates
WHERE game_id = 'YOUR_GAME_ID'
ORDER BY timestamp DESC
LIMIT 20;

-- Count updates per user
SELECT
    user_id,
    COUNT(*) as update_count,
    MIN(timestamp) as first_update,
    MAX(timestamp) as last_update
FROM location_updates
WHERE game_id = 'YOUR_GAME_ID'
GROUP BY user_id;
```

### Common Issues

#### "Location permission denied"
- **Cause:** User denied permission
- **Fix:** Go to Settings > Apps > HeistHunt > Permissions > Location > Allow

#### "GPS signal lost"
- **Cause:** Indoors or poor GPS reception
- **Fix:** Move to outdoor area or near window

#### "WebSocket not connecting"
- **Cause:** Server not running or wrong IP
- **Fix:** Check server is running, verify IP in AppModule.kt matches server IP

#### "Not receiving other players' locations"
- **Cause:** WebSocket not properly connected or role filtering issue
- **Fix:** Check logs for "Broadcasted N locations", verify roles in database

## Success Indicators

✅ **System Working Correctly:**
- Location updates appear in logs every 5 seconds
- Server logs show "Broadcasted N locations"
- Police see all players, thieves see only thieves
- No crashes or ANRs
- Battery drain within expected range (5-10%/hour)
- WebSocket reconnects automatically after network issues

❌ **System Issues:**
- No location updates in logs
- Server not receiving WebSocket messages
- All players see all locations (role filtering broken)
- App crashes on game start
- Battery drains >20% per hour
- WebSocket disconnects repeatedly

## Automated Testing (Future)

### Unit Tests to Write
1. `GameConnectionManager` - Role-based filtering logic
2. `LocationService` - Location update callbacks
3. `GameViewModel` - WebSocket event handling
4. `GameRepository` - API response parsing

### Integration Tests to Write
1. WebSocket connection and authentication
2. Location update round-trip (client → server → clients)
3. Role filtering correctness
4. Game lifecycle (start → update → end)

### UI Tests to Write
1. Permission flow (grant/deny)
2. Location updates reflected in UI
3. Error messages displayed correctly
4. Game end stops tracking

## Notes
- Test in real-world conditions (outdoors, moving) for best results
- Indoor GPS accuracy can be poor (50-100m)
- Emulator location can be mocked via Extended Controls
- Use "Send Location" in emulator for testing without physical movement
