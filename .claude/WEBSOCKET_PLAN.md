# WebSocket 실시간 업데이트 구현 계획

## 목표
참가자가 방에 들어오거나 나갈 때, 모든 사용자의 화면에 실시간으로 반영

## 현재 문제
- 새 참가자가 QR 스캔으로 입장해도 기존 참가자 화면에 표시 안 됨
- 준비 버튼을 눌러도 다른 사람에게 보이지 않음
- 수동으로 새로고침 필요

## 해결책: WebSocket 실시간 통신

### 서버 → 클라이언트 이벤트
```kotlin
sealed class RoomEvent {
    data class ParticipantJoined(val participant: Participant)
    data class ParticipantLeft(val userId: String)
    data class ParticipantReady(val userId: String, val isReady: Boolean)
    data class RoomUpdated(val room: Room)
    data class GameStarted(val gameId: String)
}
```

### 클라이언트 → 서버 메시지
```kotlin
sealed class RoomAction {
    data class Subscribe(val roomId: String)
    data class Unsubscribe(val roomId: String)
    data class ToggleReady(val roomId: String)
}
```

---

## 구현 단계

### Phase 1: 서버 WebSocket 설정

#### 1.1 의존성 (이미 있음)
```kotlin
// server/build.gradle.kts
implementation(libs.ktor.server.websockets) // ✅ 이미 추가됨
```

#### 1.2 WebSocket 플러그인 설치
```kotlin
// server/Application.kt
install(WebSockets) {
    pingPeriod = Duration.ofSeconds(15)
    timeout = Duration.ofSeconds(15)
    maxFrameSize = Long.MAX_VALUE
    masking = false
}
```

#### 1.3 WebSocket 라우트
```kotlin
// server/routes/RoomWebSocketRoutes.kt
fun Route.roomWebSocketRoutes() {
    webSocket("/api/rooms/{roomId}/ws") {
        val roomId = call.parameters["roomId"] ?: return@webSocket
        val userId = call.principal<JWTPrincipal>()
            ?.payload?.getClaim("userId")?.asString()
            ?: return@webSocket

        // 구독 관리
        RoomConnectionManager.subscribe(roomId, userId, this)

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val action = Json.decodeFromString<RoomAction>(frame.readText())
                        handleAction(roomId, userId, action)
                    }
                    else -> {}
                }
            }
        } finally {
            RoomConnectionManager.unsubscribe(roomId, userId)
        }
    }
}
```

#### 1.4 연결 관리자
```kotlin
object RoomConnectionManager {
    private val rooms = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    fun subscribe(roomId: String, userId: String, session: WebSocketSession) {
        rooms.getOrPut(roomId) { ConcurrentHashMap.newKeySet() }.add(session)

        // 입장 이벤트 브로드캐스트
        broadcast(roomId, RoomEvent.ParticipantJoined(...))
    }

    fun unsubscribe(roomId: String, userId: String) {
        rooms[roomId]?.remove(session)

        // 퇴장 이벤트 브로드캐스트
        broadcast(roomId, RoomEvent.ParticipantLeft(userId))
    }

    suspend fun broadcast(roomId: String, event: RoomEvent) {
        val json = Json.encodeToString(event)
        rooms[roomId]?.forEach { session ->
            session.send(Frame.Text(json))
        }
    }
}
```

### Phase 2: 클라이언트 WebSocket 연결

#### 2.1 WebSocket 클라이언트 생성
```kotlin
// composeApp/.../network/RoomWebSocketClient.kt
class RoomWebSocketClient(private val baseUrl: String) {
    private var session: DefaultClientWebSocketSession? = null
    private val client = HttpClient {
        install(WebSockets)
    }

    private val _events = MutableSharedFlow<RoomEvent>()
    val events: SharedFlow<RoomEvent> = _events.asSharedFlow()

    suspend fun connect(roomId: String, accessToken: String) {
        session = client.webSocketSession {
            url("ws://10.0.2.2:8080/api/rooms/$roomId/ws")
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        session?.incoming?.consumeAsFlow()?.collect { frame ->
            if (frame is Frame.Text) {
                val event = Json.decodeFromString<RoomEvent>(frame.readText())
                _events.emit(event)
            }
        }
    }

    suspend fun disconnect() {
        session?.close()
        session = null
    }

    suspend fun send(action: RoomAction) {
        val json = Json.encodeToString(action)
        session?.send(Frame.Text(json))
    }
}
```

#### 2.2 ViewModel 통합
```kotlin
// RoomViewModel.kt
class RoomViewModel(...) {
    private val wsClient = RoomWebSocketClient(...)

    init {
        viewModelScope.launch {
            wsClient.events.collect { event ->
                when (event) {
                    is RoomEvent.ParticipantJoined -> {
                        // Update participants list
                        val updatedRoom = _detailState.value.room?.copy(
                            participants = _detailState.value.room.participants + event.participant
                        )
                        _detailState.value = _detailState.value.copy(room = updatedRoom)
                    }
                    is RoomEvent.ParticipantLeft -> {
                        // Remove participant
                    }
                    is RoomEvent.ParticipantReady -> {
                        // Update ready status
                    }
                    is RoomEvent.RoomUpdated -> {
                        _detailState.value = _detailState.value.copy(room = event.room)
                    }
                }
            }
        }
    }

    fun connectToRoom(roomId: String) {
        viewModelScope.launch {
            wsClient.connect(roomId, getAccessToken())
        }
    }

    fun disconnectFromRoom() {
        viewModelScope.launch {
            wsClient.disconnect()
        }
    }
}
```

#### 2.3 UI에서 연결
```kotlin
// RoomWaitingScreen
@Composable
fun RoomWaitingScreen(
    room: Room?,
    roomViewModel: RoomViewModel,
    onBack: () -> Unit
) {
    DisposableEffect(room?.id) {
        room?.id?.let { roomViewModel.connectToRoom(it) }
        onDispose {
            roomViewModel.disconnectFromRoom()
        }
    }

    // UI는 StateFlow를 관찰하므로 자동 업데이트됨
    // ...
}
```

### Phase 3: 이벤트 트리거 지점

#### 3.1 방 참여 시
```kotlin
// server/RoomRoutes.kt - POST /api/rooms/join
post("/join") {
    // ... 참가자 추가 로직

    // WebSocket 이벤트 전송
    RoomConnectionManager.broadcast(
        roomId,
        RoomEvent.ParticipantJoined(participant)
    )
}
```

#### 3.2 방 나가기
```kotlin
// server/RoomRoutes.kt - POST /api/rooms/{id}/leave
post("/{id}/leave") {
    // ... 참가자 제거 로직

    RoomConnectionManager.broadcast(
        roomId,
        RoomEvent.ParticipantLeft(userId)
    )
}
```

#### 3.3 준비 상태 변경
```kotlin
// server/RoomRoutes.kt - POST /api/rooms/{id}/ready
post("/{id}/ready") {
    // ... 준비 상태 토글

    RoomConnectionManager.broadcast(
        roomId,
        RoomEvent.ParticipantReady(userId, newReady)
    )
}
```

---

## 데이터 모델

### 공유 DTO (shared/dto/WebSocketDto.kt)
```kotlin
@Serializable
sealed class RoomEvent {
    @Serializable
    @SerialName("participant_joined")
    data class ParticipantJoined(val participant: Participant) : RoomEvent()

    @Serializable
    @SerialName("participant_left")
    data class ParticipantLeft(val userId: String) : RoomEvent()

    @Serializable
    @SerialName("participant_ready")
    data class ParticipantReady(val userId: String, val isReady: Boolean) : RoomEvent()

    @Serializable
    @SerialName("room_updated")
    data class RoomUpdated(val room: Room) : RoomEvent()

    @Serializable
    @SerialName("game_started")
    data class GameStarted(val gameId: String) : RoomEvent()
}

@Serializable
sealed class RoomAction {
    @Serializable
    @SerialName("subscribe")
    data class Subscribe(val roomId: String) : RoomAction()

    @Serializable
    @SerialName("toggle_ready")
    data class ToggleReady(val roomId: String) : RoomAction()
}
```

---

## 파일 구조

```
server/src/main/kotlin/com/heisthunt/server/
├── plugins/
│   └── WebSocketConfig.kt (NEW)
├── routes/
│   └── RoomWebSocketRoutes.kt (NEW)
└── websocket/
    └── RoomConnectionManager.kt (NEW)

shared/src/commonMain/kotlin/com/heisthunt/shared/
└── dto/
    └── WebSocketDto.kt (NEW)

composeApp/src/commonMain/kotlin/com/heisthunt/app/
├── network/
│   └── RoomWebSocketClient.kt (NEW)
└── viewmodel/
    └── RoomViewModel.kt (MODIFY)
```

---

## 구현 순서

1. ✅ **공유 DTO 정의** (RoomEvent, RoomAction)
2. ✅ **서버 WebSocket 설정**
   - Plugin 설치
   - ConnectionManager 생성
   - Routes 추가
3. ✅ **이벤트 브로드캐스트 통합**
   - join/leave/ready 엔드포인트 수정
4. ✅ **클라이언트 WebSocket 클라이언트**
   - RoomWebSocketClient 생성
   - 이벤트 수신 처리
5. ✅ **ViewModel 통합**
   - WebSocket 연결/해제
   - 이벤트 → StateFlow 업데이트
6. ✅ **UI 통합**
   - DisposableEffect로 연결 관리
   - 자동 업데이트 확인
7. ✅ **테스트**
   - 2개 기기로 동시 접속
   - 실시간 업데이트 확인

---

## 예상 이슈 및 해결

### 이슈 1: 재연결 처리
**문제:** 네트워크 끊김 시 WebSocket 연결 해제

**해결:**
```kotlin
var reconnectAttempts = 0
while (reconnectAttempts < 3) {
    try {
        connect(roomId, token)
        break
    } catch (e: Exception) {
        reconnectAttempts++
        delay(1000 * reconnectAttempts)
    }
}
```

### 이슈 2: 메모리 누수
**문제:** WebSocket 세션이 제대로 해제 안 됨

**해결:**
```kotlin
DisposableEffect(room?.id) {
    // 연결
    onDispose {
        // 반드시 해제
        viewModelScope.launch {
            wsClient.disconnect()
        }
    }
}
```

### 이슈 3: 중복 이벤트
**문제:** 같은 참가자가 두 번 추가됨

**해결:**
```kotlin
val updatedParticipants = _detailState.value.room?.participants
    ?.filter { it.userId != event.participant.userId }
    ?.plus(event.participant)
```

---

## 테스트 시나리오

### 시나리오 1: 참가자 입장
```
기기 A: 방 생성 → RoomWaitingScreen
기기 B: QR 스캔 → 방 참여
기기 A: 참가자 목록에 B 자동 추가됨 ✅
```

### 시나리오 2: 준비 상태
```
기기 A: 준비 버튼 클릭
기기 B: A의 준비 상태가 즉시 표시됨 ✅
```

### 시나리오 3: 참가자 퇴장
```
기기 B: Back 버튼 → 방 나가기
기기 A: 참가자 목록에서 B 자동 제거됨 ✅
```

---

## 성공 기준

- [x] 참가자 입장 시 모든 화면 자동 업데이트
- [x] 참가자 퇴장 시 자동 업데이트
- [x] 준비 상태 변경 실시간 반영
- [x] 네트워크 끊김 시 재연결
- [x] 메모리 누수 없음
- [ ] iOS 지원 (나중)

---

## 예상 소요 시간

- DTO 정의: 30분
- 서버 WebSocket 구현: 2시간
- 클라이언트 구현: 2시간
- ViewModel 통합: 1시간
- 테스트 및 디버깅: 1.5시간

**총 예상: 7시간 (1일)**
