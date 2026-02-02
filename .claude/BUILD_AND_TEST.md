# Build and Test Instructions

## Prerequisites

Before testing the room creation feature, ensure:

1. **Server is Running**
   ```bash
   cd server
   ./gradlew run
   ```
   Server should be accessible at `http://10.0.2.2:8080` (Android emulator) or `http://localhost:8080`

2. **Database is Set Up**
   - PostgreSQL running
   - Database initialized with schema
   - Connection details in server configuration

3. **Firebase Configured**
   - Google Sign-In working
   - User authenticated

## Build Instructions

### Sync Gradle

After the implementation changes, you need to sync Gradle to download the QRose library:

```bash
# From project root
./gradlew --refresh-dependencies

# Or in Android Studio
# File → Sync Project with Gradle Files
```

### Build the App

```bash
# Build debug APK
./gradlew composeApp:assembleDebug

# Install on connected device/emulator
./gradlew composeApp:installDebug

# Or build and run
./gradlew composeApp:installDebugAndroidTest
```

### Common Build Issues

#### Issue: QRose dependency not found
**Solution:**
```bash
./gradlew --refresh-dependencies
./gradlew clean
./gradlew composeApp:assembleDebug
```

#### Issue: Compose compiler mismatch
**Solution:** Check that `compose-multiplatform` version matches Kotlin version in `gradle/libs.versions.toml`

#### Issue: Network permission denied
**Solution:** Ensure AndroidManifest.xml has:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Testing the Feature

### Test Scenario 1: Happy Path - Room Creation

1. **Start the app**
   - Launch app on Android emulator or device
   - Should land on Login screen

2. **Login**
   - Use Google Sign-In or email/password
   - Should navigate to Operation Center

3. **Create Room**
   - Tap "작전 설계하기" button
   - Should see loading indicator in button
   - Wait 1-2 seconds

4. **Verify Room Waiting Screen**
   - Screen should transition to RoomWaitingScreen
   - Header should show "MISSION BRIEFING"
   - Operation name should display (e.g., "OPERATION: NEW OPERATION ROOM")
   - QR code should be visible in white card
   - Participant count should show "1 / 10"
   - Your avatar should appear (first letter of nickname)

5. **Verify QR Code**
   - QR code should be visible and properly rendered
   - It should have blue accents (ball and frame)
   - Dark pixels should be dark slate color
   - Try scanning with another phone camera - should read the room code

### Test Scenario 2: Error Handling - Server Down

1. **Stop the server**
   ```bash
   # Kill the Ktor server process
   pkill -f "ktor"
   ```

2. **Try to create room**
   - Tap "작전 설계하기" button
   - Loading indicator should appear
   - After timeout, error dialog should appear

3. **Verify Error Dialog**
   - Dialog should show error message
   - "OK" button should be present
   - Tapping "OK" should dismiss dialog

4. **Retry**
   - Restart server
   - Tap button again
   - Should work normally

### Test Scenario 3: Multiple Rooms

1. **Create first room**
   - Follow Test Scenario 1
   - Note the QR code / room code

2. **Go back**
   - Tap back or navigate to main screen

3. **Create second room**
   - Tap "작전 설계하기" again
   - Should create a NEW room
   - QR code should be DIFFERENT from first room

4. **Verify uniqueness**
   - Each room should have unique code
   - Codes should be 8 characters (server-generated)

### Test Scenario 4: Navigation Flow

1. **From Operation Center → Waiting Screen**
   - Tap "작전 설계하기"
   - Should navigate to RoomWaitingScreen
   - Navigation should be smooth

2. **From Waiting Screen → Operation Center**
   - Tap back button (if available) or system back
   - Should return to Operation Center
   - State should be clean (no lingering room data)

3. **Create Another Room**
   - Tap "작전 설계하기" again
   - Should work without issues
   - No stale data from previous room

### Test Scenario 5: UI States

1. **Loading State**
   - Button should show CircularProgressIndicator
   - Button should be disabled
   - User cannot tap multiple times

2. **Error State**
   - Error dialog should be modal
   - Cannot interact with background
   - Dismissing clears error

3. **Success State**
   - QR code renders properly
   - All data displays correctly
   - UI is responsive

## Debugging

### View Network Logs

```bash
# Android Logcat filter for API calls
adb logcat | grep -i "ktor"
```

### View Room Creation Requests

In your server logs, you should see:
```
POST /api/rooms
Request body: {"name":"New Operation Room","settings":{...}}
Response: {"id":"...","code":"ABCD1234",...}
```

### Common Issues

#### QR Code Not Showing
**Possible causes:**
- Room code is null/empty
- QRose library not loaded
- Compose recomposition issue

**Debug steps:**
```kotlin
// Add logs in QRCodeDisplay
println("QRCodeDisplay: Generating QR for code: $roomCode")
```

#### Room Not Creating
**Possible causes:**
- Server not running
- Network configuration wrong
- Firebase auth token invalid

**Debug steps:**
```kotlin
// Add logs in RoomViewModel
println("Creating room: name=$name")
println("Result: $result")
```

#### Navigation Not Working
**Possible causes:**
- `shouldNavigateToWaiting` not set
- LaunchedEffect not triggering
- State not updating

**Debug steps:**
```kotlin
// Add logs in OperationScreen
LaunchedEffect(roomDetailState.shouldNavigateToWaiting) {
    println("Navigation flag: ${roomDetailState.shouldNavigateToWaiting}")
    // ...
}
```

## Performance Testing

### QR Code Generation Speed
- Should complete in < 100ms
- No UI freeze
- Smooth transition

### Network Request Time
- Room creation: < 2s (depends on server)
- Should show loading indicator during request
- Error handling for timeouts

### Memory Usage
- QR code bitmap should be released properly
- No memory leaks on multiple room creations
- Monitor with Android Profiler

## Automated Tests (Future)

### Unit Tests
```kotlin
class RoomViewModelTest {
    @Test
    fun `createRoom should update state on success`() { }

    @Test
    fun `createRoom should set error on failure`() { }

    @Test
    fun `resetNavigationState should clear flag`() { }
}
```

### UI Tests
```kotlin
@Test
fun createRoom_showsLoadingThenQRCode() {
    // Click button
    // Verify loading indicator
    // Verify navigation to waiting screen
    // Verify QR code displayed
}
```

## Checklist Before Merging

- [ ] All test scenarios pass
- [ ] QR code is scannable
- [ ] Error handling works
- [ ] No crashes or ANRs
- [ ] Logs are clean (no errors)
- [ ] UI matches design spec
- [ ] Navigation is smooth
- [ ] Back button works
- [ ] Multiple room creation works
- [ ] Code is properly commented
- [ ] No TODO comments left
- [ ] Git commit follows conventions

## Known Issues

None at this time. If you encounter issues during testing, document them here.

## Next Steps

After successful testing:

1. **Commit the changes**
   ```bash
   git add .
   git commit -m "feat: implement room creation and QR code generation

   - Add QRose library for QR code generation
   - Create QRCodeDisplay component
   - Integrate room creation with OperationScreen
   - Add loading and error states
   - Display room details and participants

   Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
   ```

2. **Push to remote**
   ```bash
   git push origin main
   ```

3. **Test on real device** (not just emulator)

4. **Prepare for Phase 2** (QR code scanning)
