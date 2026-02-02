# HeistHunt - Next Steps

## Completed ✅
- ✅ Location tracking system (GPS + WebSocket)
- ✅ Role-based location visibility
- ✅ Permission handling for Android
- ✅ Real-time location broadcasting
- ✅ Database storage for location history
- ✅ GameViewModel and UI integration
- ✅ Error handling (GPS loss, network issues)

## Immediate Next Steps (Critical for Gameplay)

### 1. Google Maps Integration ⭐ HIGH PRIORITY
**Estimated Time:** 2-3 days

**Why:** Players need to see actual maps, not placeholder UI

**Tasks:**
1. Add Google Maps SDK dependencies
   ```kotlin
   // In composeApp/build.gradle.kts
   androidMain.dependencies {
       implementation("com.google.android.gms:play-services-maps:18.2.0")
       implementation("com.google.maps.android:maps-compose:4.3.0")
   }
   ```

2. Get Google Maps API key
   - Go to Google Cloud Console
   - Enable Maps SDK for Android
   - Create API key
   - Add to `local.properties`:
     ```
     MAPS_API_KEY=your_api_key_here
     ```

3. Update AndroidManifest.xml
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="${MAPS_API_KEY}" />
   ```

4. Replace MapSection in GameScreen.kt
   ```kotlin
   GoogleMap(
       modifier = Modifier.fillMaxSize(),
       cameraPositionState = cameraPositionState,
       properties = MapProperties(isMyLocationEnabled = true)
   ) {
       // Add markers for each player
       uiState.playerLocations.forEach { playerLocation ->
           Marker(
               state = MarkerState(
                   position = LatLng(
                       playerLocation.location.latitude,
                       playerLocation.location.longitude
                   )
               ),
               title = playerLocation.userId,
               icon = getMarkerIcon(playerLocation.role)
           )
       }

       // Add circle for safe radius
       Circle(
           center = gameCenterLatLng,
           radius = maxRadiusMeters,
           strokeColor = Color.Red,
           fillColor = Color.Red.copy(alpha = 0.1f)
       )
   }
   ```

5. Create custom marker icons
   - Police: Blue badge icon
   - Thief: Red mask icon
   - Me: Larger icon with pulsing animation

**Files to Create/Modify:**
- `composeApp/src/androidMain/kotlin/com/heisthunt/app/ui/game/MapView.android.kt`
- `composeApp/build.gradle.kts`
- `composeApp/src/androidMain/AndroidManifest.xml`
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/GameScreen.kt`

### 2. Catch Mechanism ⭐ HIGH PRIORITY
**Estimated Time:** 1-2 days

**Why:** Core gameplay mechanic - police need to catch thieves

**Tasks:**

1. Add distance calculation utility
   ```kotlin
   // In shared module
   object GeoUtils {
       fun calculateDistance(
           lat1: Double, lon1: Double,
           lat2: Double, lon2: Double
       ): Double {
           // Haversine formula
           val R = 6371000.0 // Earth radius in meters
           val dLat = Math.toRadians(lat2 - lat1)
           val dLon = Math.toRadians(lon2 - lon1)
           val a = sin(dLat / 2).pow(2) +
                   cos(Math.toRadians(lat1)) *
                   cos(Math.toRadians(lat2)) *
                   sin(dLon / 2).pow(2)
           val c = 2 * atan2(sqrt(a), sqrt(1 - a))
           return R * c
       }
   }
   ```

2. Add catch endpoint to server
   ```kotlin
   // In GameRoutes.kt
   post("/{gameId}/catch") {
       val gameId = call.parameters["gameId"]
       val request = call.receive<CatchRequest>()

       // Get both player locations
       val catcherLocation = getLastLocation(gameId, userId)
       val targetLocation = getLastLocation(gameId, request.targetUserId)

       // Calculate distance
       val distance = GeoUtils.calculateDistance(
           catcherLocation.latitude, catcherLocation.longitude,
           targetLocation.latitude, targetLocation.longitude
       )

       if (distance <= 10.0) { // 10 meters
           // Mark as caught
           transaction {
               GamePlayers.update({
                   (GamePlayers.gameId eq gameId) and
                   (GamePlayers.userId eq request.targetUserId)
               }) {
                   it[isCaught] = true
                   it[caughtAt] = Clock.System.now()
               }
           }

           // Broadcast to all players
           GameConnectionManager.broadcastMessage(
               gameId,
               WebSocketMessage.PlayerCaught(request.targetUserId, targetNickname)
           )

           call.respond(CatchResponse(true, "Catch successful!"))
       } else {
           call.respond(CatchResponse(false, "Target too far: ${distance}m"))
       }
   }
   ```

3. Update GameScreen police UI
   ```kotlin
   // Show list of nearby thieves
   val nearbyThieves = uiState.playerLocations
       .filter { it.role == PlayerRole.THIEF }
       .map { playerLoc ->
           val distance = GeoUtils.calculateDistance(
               uiState.myLocation.latitude,
               uiState.myLocation.longitude,
               playerLoc.location.latitude,
               playerLoc.location.longitude
           )
           playerLoc to distance
       }
       .filter { (_, distance) -> distance <= 10.0 }

   LazyColumn {
       items(nearbyThieves) { (player, distance) ->
           Button(
               onClick = { viewModel.catchPlayer(player.userId) }
           ) {
               Text("Catch ${player.userId} (${distance.roundToInt()}m)")
           }
       }
   }
   ```

**Files to Create/Modify:**
- `shared/src/commonMain/kotlin/com/heisthunt/shared/utils/GeoUtils.kt`
- `server/src/main/kotlin/com/heisthunt/server/routes/GameRoutes.kt`
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/GameScreen.kt`
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/viewmodel/GameViewModel.kt`

### 3. Game Victory Conditions
**Estimated Time:** 1 day

**Why:** Games need to end when conditions are met

**Victory Conditions:**
1. **Police Win:** All thieves caught
2. **Thieves Win:** Timer reaches 0:00 with at least 1 thief free

**Tasks:**

1. Add game end check in server
   ```kotlin
   fun checkGameEnd(gameId: String) {
       transaction {
           val thieves = GamePlayers.selectAll()
               .where { (GamePlayers.gameId eq gameId) and (GamePlayers.role eq "THIEF") }
               .toList()

           val allCaught = thieves.all { it[GamePlayers.isCaught] }

           if (allCaught) {
               // Police win
               Games.update({ Games.id eq gameId }) {
                   it[status] = "FINISHED"
                   it[winner] = "POLICE"
                   it[endedAt] = Clock.System.now()
               }

               GameConnectionManager.broadcastMessage(
                   gameId,
                   WebSocketMessage.GameEnded(GameWinner.POLICE)
               )
           }
       }
   }
   ```

2. Add timer check
   ```kotlin
   // Background job to check time
   launch {
       while (isActive) {
           delay(1000)
           checkGameTimers()
       }
   }

   fun checkGameTimers() {
       transaction {
           val activeGames = Games.selectAll()
               .where { Games.status eq "IN_PROGRESS" }
               .toList()

           activeGames.forEach { game ->
               val startTime = game[Games.startedAt]
               val duration = getRoomDuration(game[Games.roomId])
               val elapsed = (Clock.System.now() - startTime).inWholeSeconds

               if (elapsed >= duration) {
                   // Time's up, thieves win
                   Games.update({ Games.id eq game[Games.id] }) {
                       it[status] = "FINISHED"
                       it[winner] = "THIEF"
                       it[endedAt] = Clock.System.now()
                   }

                   GameConnectionManager.broadcastMessage(
                       game[Games.id],
                       WebSocketMessage.GameEnded(GameWinner.THIEF)
                   )
               }
           }
       }
   }
   ```

**Files to Modify:**
- `server/src/main/kotlin/com/heisthunt/server/routes/GameRoutes.kt`
- Add new file: `server/src/main/kotlin/com/heisthunt/server/jobs/GameTimerJob.kt`

## Secondary Features (Important but not blocking)

### 4. Safe Radius & Geofencing
**Estimated Time:** 2 days

**Tasks:**
- Store game center location when game starts
- Calculate distance from center for each player
- Show warning when approaching boundary (e.g., 450m of 500m)
- Auto-eliminate players who leave radius
- Optional: Shrinking zone over time (like battle royale)

### 5. Phase System (Preparation vs Pursuit)
**Estimated Time:** 2-3 days

**Phases:**
1. **Preparation** (first 5 minutes)
   - Police can see all players
   - Thieves can strategize

2. **Pursuit** (remaining time)
   - Police can't see thieves until close
   - Thieves revealed when within 50m of police

**Tasks:**
- Add phase field to Games table
- Add phase transition logic
- Update location filtering based on phase
- Update UI to show current phase

### 6. iOS Implementation
**Estimated Time:** 3-4 days

**Tasks:**
- Implement LocationService using CoreLocation
- Request "When In Use" location permission
- Add location usage description to Info.plist
- Test GPS accuracy on iOS devices
- Ensure parity with Android

### 7. Game Results & Statistics
**Estimated Time:** 2 days

**Tasks:**
- Create GameResultScreen showing:
  - Winner (Police/Thieves)
  - Individual performance
  - Map replay of movements
  - Time played
  - Distance traveled
- Update user statistics (wins/losses)
- Save game replay data

## Polish & UX Improvements

### 8. Sound Effects & Haptics
**Estimated Time:** 1 day

**Sounds:**
- Player caught (dramatic sound)
- Game start countdown
- Timer warning (last 60 seconds)
- Victory/defeat music

**Haptics:**
- Vibration when caught
- Pulse when near boundary
- Feedback on button press

### 9. Notifications
**Estimated Time:** 1 day

**Use Cases:**
- "Game is starting!" when host launches
- "You were caught!" notification
- "Your team won!" on victory
- Background location tracking reminder

### 10. Better Error Messages
**Estimated Time:** 0.5 day

**Current → Better:**
- "GPS signal lost" → "GPS signal lost. Move to an outdoor area or near a window for better reception."
- "Failed to catch player" → "Target too far away (15m). Get within 10 meters to catch."

## Testing & Deployment

### 11. Automated Tests
**Estimated Time:** 3-4 days

**Unit Tests:**
- GeoUtils.calculateDistance()
- GameConnectionManager role filtering
- LocationService callbacks
- GameViewModel state management

**Integration Tests:**
- WebSocket connection flow
- Location update round-trip
- Catch mechanism end-to-end
- Game lifecycle (start → play → end)

**UI Tests:**
- Permission flow
- Game screen updates
- Error dialogs

### 12. Performance Optimization
**Estimated Time:** 2 days

**Tasks:**
- Profile battery usage
- Optimize location update frequency (adaptive intervals)
- Reduce WebSocket message size (binary protocol?)
- Database query optimization (indexes, caching)
- Test with 20+ concurrent players

### 13. Production Deployment
**Estimated Time:** 2-3 days

**Tasks:**
- Set up production server (AWS, GCP, or DigitalOcean)
- Configure HTTPS for WebSocket
- Set up database backups
- Add monitoring (Sentry, Datadog)
- Create privacy policy (location data)
- Submit to Google Play Store
- Beta testing with real users

## Recommended Development Order

**Week 1: Core Gameplay**
1. Google Maps Integration (3 days)
2. Catch Mechanism (2 days)

**Week 2: Game Flow**
3. Victory Conditions (1 day)
4. Safe Radius & Geofencing (2 days)
5. Game Results Screen (2 days)

**Week 3: Polish & Testing**
6. Sound Effects & Haptics (1 day)
7. Better Error Messages (0.5 day)
8. Automated Tests (3 days)
9. Performance Optimization (1.5 days)

**Week 4: Deployment**
10. Production Server Setup (2 days)
11. Beta Testing (3 days)
12. Play Store Submission (2 days)

## Total Estimated Time
- **Critical Features:** 7-8 days
- **Secondary Features:** 9-11 days
- **Polish & Testing:** 8-9 days
- **Deployment:** 7 days

**Total:** 31-35 days (~7 weeks)

## Resources Needed
- Google Cloud account (for Maps API)
- Production server (AWS/GCP/DO)
- Android developer account ($25 one-time)
- Test devices (2+ Android phones)
- Optional: iOS developer account ($99/year) for iOS version

## Questions to Answer
1. What should the catch radius be? (Currently 10m)
2. Should there be a cooldown on catch attempts?
3. How long should games last? (Currently 30 minutes)
4. What happens if a player's app crashes mid-game?
5. Should we allow spectators to watch ongoing games?
6. How to handle cheating (GPS spoofing)?

## Support & Maintenance Plan
- Monitor server logs for errors
- Collect crash reports (Firebase Crashlytics)
- User feedback channel (in-app or Discord)
- Regular updates for bug fixes
- Balance adjustments based on gameplay data
