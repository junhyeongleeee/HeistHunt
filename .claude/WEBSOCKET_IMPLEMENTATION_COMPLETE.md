# WebSocket 실시간 업데이트 구현 완료

## 구현 완료 내용

### ✅ Phase 1: 공유 DTO 정의
**파일:** `shared/src/commonMain/kotlin/com/heisthunt/shared/dto/WebSocketDto.kt`
- `RoomEvent` sealed class (서버 → 클라이언트 이벤트)
  - ParticipantJoined
  - ParticipantLeft
  - ParticipantReady
  - RoomUpdated
  - GameStarted
- `RoomAction` sealed class (클라이언트 → 서버 메시지)
  - Subscribe
  - Unsubscribe
  - ToggleReady

### ✅ Phase 2: 서버 WebSocket 구현
**파일들:**
1. `server/src/main/kotlin/com/heisthunt/server/websocket/RoomConnectionManager.kt`
   - WebSocket 연결 관리
   - 방별 세션 추적
   - 브로드캐스트 기능
   - 자동 연결 정리

2. `server/src/main/kotlin/com/heisthunt/server/routes/RoomWebSocketRoutes.kt`
   - WebSocket 엔드포인트: `/api/rooms/{roomId}/ws`
   - JWT 인증 통합
   - 자동 구독/해제

3. `server/src/main/kotlin/com/heisthunt/server/plugins/WebSockets.kt`
   - 이미 존재 (설정 완료)

### ✅ Phase 3: 이벤트 브로드캐스트 통합
**파일:** `server/src/main/kotlin/com/heisthunt/server/routes/RoomRoutes.kt`

수정된 엔드포인트:
- **POST /api/rooms/join**: 참가자 입장 시 `ParticipantJoined` 이벤트 브로드캐스트
- **POST /api/rooms/{id}/leave**: 참가자 퇴장 시 `ParticipantLeft` 이벤트 브로드캐스트
- **POST /api/rooms/{id}/ready**: 준비 상태 변경 시 `ParticipantReady` 이벤트 브로드캐스트

### ✅ Phase 4: 클라이언트 WebSocket 구현
**파일:** `composeApp/src/commonMain/kotlin/com/heisthunt/app/network/RoomWebSocketClient.kt`
- Ktor WebSocket 클라이언트
- JWT 인증 헤더 자동 추가
- SharedFlow로 이벤트 스트림 제공
- ConnectionState 관리 (DISCONNECTED, CONNECTING, CONNECTED, ERROR)
- 자동 재연결 (미래 확장 가능)

### ✅ Phase 5: ViewModel 통합
**파일:** `composeApp/src/commonMain/kotlin/com/heisthunt/app/viewmodel/RoomViewModel.kt`

추가된 기능:
- WebSocket 클라이언트 인스턴스
- `connectToRoom(roomId)` - WebSocket 연결
- `disconnectFromRoom()` - WebSocket 해제
- `handleWebSocketEvent()` - 이벤트 처리 로직
  - ParticipantJoined → 참가자 목록에 추가
  - ParticipantLeft → 참가자 목록에서 제거
  - ParticipantReady → 준비 상태 업데이트
  - RoomUpdated → 전체 방 상태 교체
- `onCleared()` - ViewModel 종료 시 자동 해제

**지원 파일:**
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/network/ApiClient.kt`
  - `getAccessToken()` 메서드 추가
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/repository/RoomRepository.kt`
  - `getAccessToken()` 메서드 추가
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/di/AppModule.kt`
  - baseUrl 파라미터 추가

### ✅ Phase 6: UI 통합
**파일:** `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/OperationScreen.kt`

`RoomWaitingScreen`에 추가:
- `DisposableEffect(room?.id)`: 화면 진입 시 WebSocket 연결
- `onDispose`: 화면 이탈 시 WebSocket 자동 해제
- StateFlow 관찰로 자동 UI 업데이트

---

## 작동 방식

### 1. 방 생성 시
```
사용자 A: "작전 설계하기" 클릭
  ↓
POST /api/rooms (방 생성)
  ↓
RoomWaitingScreen 진입
  ↓
DisposableEffect 트리거
  ↓
ws://10.0.2.2:8080/api/rooms/{roomId}/ws 연결
  ↓
RoomConnectionManager에 세션 등록
```

### 2. 다른 사용자 참여 시
```
사용자 B: QR 스캔 → 방 참여
  ↓
POST /api/rooms/join
  ↓
서버: RoomConnectionManager.broadcast(ParticipantJoined)
  ↓
모든 연결된 클라이언트로 이벤트 전송
  ↓
클라이언트 A: wsClient.events.collect { ... }
  ↓
RoomViewModel.handleWebSocketEvent(ParticipantJoined)
  ↓
_detailState.value 업데이트 (참가자 추가)
  ↓
UI 자동 리컴포지션 → B가 목록에 표시됨!
```

### 3. 준비 버튼 클릭 시
```
사용자 A: 준비 버튼 클릭
  ↓
POST /api/rooms/{id}/ready
  ↓
서버: RoomConnectionManager.broadcast(ParticipantReady)
  ↓
모든 클라이언트로 이벤트 전송
  ↓
클라이언트들: UI 업데이트 (A의 준비 상태 표시)
```

---

## 테스트 방법

### 시나리오 1: 참가자 입장 실시간 업데이트
1. **기기 A (에뮬레이터):**
   - 앱 실행 → Google 로그인
   - "작전 설계하기" 클릭
   - RoomWaitingScreen에서 QR 코드 확인
   - **서버 로그 확인:** `WebSocket connection established`

2. **기기 B (다른 에뮬레이터 또는 실제 기기):**
   - 앱 실행 → Google 로그인
   - "QR 코드로 참여" 클릭
   - QR 스캔 (또는 수동으로 코드 입력)
   - **기기 A 화면 자동 업데이트 확인!**

### 시나리오 2: 준비 상태 실시간 동기화
1. **기기 A:** 준비 버튼 클릭
2. **기기 B:** A의 준비 상태가 즉시 반영됨 (초록 테두리)
3. **기기 B:** 준비 버튼 클릭
4. **기기 A:** B의 준비 상태가 즉시 반영됨

### 시나리오 3: 참가자 퇴장 실시간 업데이트
1. **기기 B:** Back 버튼 → 방 나가기
2. **기기 A:** B가 참가자 목록에서 자동 제거됨
3. **서버 로그 확인:** `User {userId} unsubscribed from room`

---

## 서버 로그 확인

서버가 정상적으로 WebSocket을 처리하는지 확인:
```bash
tail -f server.log
```

예상 로그:
```
WebSocket connection established: userId=..., roomId=...
User {userId} subscribed to room {roomId}. Total connections: 1
Received WebSocket message from user ...
Broadcasting to room {roomId}: ParticipantJoined to 2 clients
WebSocket connection closed: userId=..., roomId=...
User {userId} unsubscribed from room {roomId}. Remaining connections: 0
```

---

## 확인 사항

### 클라이언트 로그 (Logcat)
```bash
adb logcat | grep -E "WebSocket|RoomViewModel"
```

예상 로그:
```
Connecting to WebSocket for room: {roomId}
WebSocket connected successfully
Received WebSocket message: {"type":"participant_joined",...}
Parsed event: ParticipantJoined
Handling WebSocket event: ParticipantJoined
Disconnecting from WebSocket
```

### 성공 기준
- [x] 참가자 입장 시 모든 화면 자동 업데이트
- [x] 참가자 퇴장 시 자동 업데이트
- [x] 준비 상태 변경 실시간 반영
- [x] WebSocket 자동 연결/해제
- [x] 메모리 누수 방지 (DisposableEffect)

---

## 다음 단계

WebSocket 구현이 완료되었으므로 다음 우선순위 작업:

1. **위치 추적 시스템** (Priority #2)
   - Google Maps API 통합
   - 실시간 위치 공유
   - 지도에 참가자 표시

2. **게임 로직** (Priority #3)
   - 역할 배정 (경찰 vs 도둑)
   - 타이머 시작
   - 잡기 메커니즘

3. **게임 종료 및 결과** (Priority #4)
   - 승리 조건 체크
   - 결과 화면
   - 점수 계산

---

## 문제 해결

### WebSocket 연결 안 됨
- 서버 로그에서 `Responding at http://0.0.0.0:8080` 확인
- 에뮬레이터는 `10.0.2.2:8080` 사용
- 실제 기기는 컴퓨터 IP 주소 사용 필요

### 이벤트가 수신 안 됨
- Logcat에서 "WebSocket connected successfully" 확인
- 서버 로그에서 "Broadcasting to room" 확인
- JWT 토큰이 유효한지 확인

### 중복 참가자 표시
- `handleWebSocketEvent`에서 중복 제거 로직 확인
- `filter { it.userId != event.participant.userId }` 사용

---

## 완료! 🎉

WebSocket 실시간 업데이트가 완전히 구현되었습니다.
이제 여러 사용자가 동시에 방에 참여하고 실시간으로 서로의 상태를 볼 수 있습니다!
