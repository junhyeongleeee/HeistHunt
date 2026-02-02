# 게임 시작 로직 구현 완료

## 구현 완료 항목

### Phase 1: 서버 - GameStarted 이벤트 브로드캐스트 ✅
**파일**: `server/src/main/kotlin/com/heisthunt/server/routes/GameRoutes.kt`
- RoomConnectionManager 및 RoomEvent 임포트 추가
- 게임 시작 성공 시 `RoomEvent.GameStarted(gameId)` 브로드캐스트
- 모든 클라이언트에게 실시간으로 게임 시작 알림 전송

### Phase 2: 클라이언트 Repository - startGame 메서드 ✅
**파일**:
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/repository/RoomRepository.kt`
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/network/ApiClient.kt` (이미 구현됨)

**추가 사항**:
- `RoomRepository.startGame()` 메서드 추가
- StartGameResponse DTO 임포트

### Phase 3: ViewModel - 게임 시작 로직 ✅
**파일**: `composeApp/src/commonMain/kotlin/com/heisthunt/app/viewmodel/RoomViewModel.kt`

**변경 사항**:
1. **RoomDetailUiState 확장**:
   - `shouldNavigateToGame: Boolean` 추가
   - `gameId: String?` 추가
   - `myRole: PlayerRole?` 추가

2. **startGame() 함수 추가**:
   - 서버에 게임 시작 API 호출
   - 성공 시 게임 화면 전환 플래그 설정
   - 내 역할(myRole) 저장

3. **WebSocket 이벤트 핸들러 수정**:
   - `GameStarted` 이벤트 수신 시 startGame() 호출
   - 서버로부터 역할 정보 가져오기

4. **resetGameNavigationState() 함수 추가**:
   - 게임 화면 전환 후 플래그 리셋

### Phase 4: GameScreen UI 생성 ✅
**새 파일**: `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/GameScreen.kt`

**컴포넌트**:
- `GameScreen`: 메인 게임 화면
- `RoleHeader`: 역할 표시 (경찰/도둑)
- `TimerDisplay`: 타이머 카운트다운 (현재는 placeholder)
- `ParticipantsList`: 참가자 목록 (역할별 분류)
- `MapPlaceholder`: 지도 영역 (다음 단계 구현 예정)

### Phase 5: OperationScreen 업데이트 ✅
**파일**: `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/OperationScreen.kt`

**변경 사항**:
1. **OperationView enum 확장**: `GAME` 추가
2. **게임 화면 전환 LaunchedEffect 추가**
3. **when 분기에 GAME 케이스 추가**
4. **TokenStorage 임포트 추가**

### Phase 6: RoomWaitingScreen - 게임 시작 버튼 활성화 ✅
**파일**: `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/OperationScreen.kt`

**버튼 로직**:
- 방장만 활성화 (`isHost`)
- 최소 2명 이상 (`enoughPlayers`)
- 모든 참가자 준비 완료 (`allReady`)
- 로딩 중일 때 CircularProgressIndicator 표시
- 조건별 버튼 텍스트 표시

### Phase 7: TokenStorage - userId 접근자 추가 ✅
**파일**: `composeApp/src/commonMain/kotlin/com/heisthunt/app/network/TokenStorage.kt`

**추가 사항**:
- `val userId: String?` 프로퍼티 추가
- 현재 로그인한 사용자의 ID 반환

## 게임 시작 플로우

```
1. 방장이 "작전 개시" 버튼 클릭
   ↓
2. RoomViewModel.startGame(roomId) 호출
   ↓
3. 서버: POST /api/games/{roomId}/start
   - 역할 랜덤 배정
   - Game 레코드 생성
   - RoomConnectionManager.broadcast(GameStarted(gameId))
   ↓
4. 모든 클라이언트:
   - GameStarted 이벤트 수신 (WebSocket)
   - startGame() 재호출하여 내 역할 조회
   - shouldNavigateToGame = true 설정
   ↓
5. LaunchedEffect가 플래그 감지
   ↓
6. GameScreen으로 자동 전환
   - 자신의 역할 표시
   - 참가자 목록 (역할별 분류)
```

## 주요 파일 변경 사항

### 수정된 파일 (6개)
1. ✅ `server/src/main/kotlin/com/heisthunt/server/routes/GameRoutes.kt`
2. ✅ `composeApp/src/commonMain/kotlin/com/heisthunt/app/repository/RoomRepository.kt`
3. ✅ `composeApp/src/commonMain/kotlin/com/heisthunt/app/viewmodel/RoomViewModel.kt`
4. ✅ `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/OperationScreen.kt`
5. ✅ `composeApp/src/commonMain/kotlin/com/heisthunt/app/network/TokenStorage.kt`

### 새로 생성된 파일 (1개)
1. ✅ `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/GameScreen.kt`

## 테스트 시나리오

### 시나리오 1: 정상 게임 시작
1. 에뮬레이터: 방 생성 → "준비" 클릭
2. 삼성 갤럭시: QR 스캔 → 참여 → "준비" 클릭
3. 에뮬레이터: "작전 개시" 버튼 활성화 확인
4. 에뮬레이터: "작전 개시" 클릭
5. **양쪽 기기**: 자동으로 GameScreen 전환
6. **양쪽 기기**: 각자 역할 표시 확인 (한 명은 경찰, 한 명은 도둑)
7. **양쪽 기기**: 참가자 목록에 역할별로 분류되어 표시

### 시나리오 2: 검증 에러
1. 에뮬레이터: 방 생성 (혼자)
2. 에뮬레이터: "작전 개시" 버튼 텍스트 = "최소 2명 필요"
3. 삼성 갤럭시: 참여 (준비 안 함)
4. 에뮬레이터: "작전 개시" 버튼 텍스트 = "모두 준비 필요"
5. 삼성 갤럭시: "준비" 클릭
6. 에뮬레이터: "작전 개시" 버튼 텍스트 = "작전 개시" (활성화)

### 시나리오 3: 방장 아닌 사용자
1. 삼성 갤럭시: "작전 개시" 버튼 비활성화
2. 버튼 텍스트 = "방장만 시작 가능"

## 다음 단계 (향후 구현)

### Phase 2 (다음 작업)
- [ ] 실시간 타이머 구현 (서버 동기화)
- [ ] 게임 상태 폴링 (GET /api/games/{gameId}/status)
- [ ] 일시정지/재개 기능

### Phase 3
- [ ] Google Maps 통합
- [ ] 실시간 위치 추적 및 지도 표시
- [ ] 잡기 메커니즘 (거리 기반)

### Phase 4
- [ ] 게임 종료 조건 체크
- [ ] 결과 화면
- [ ] 재시작 기능

## 검증 명령어

### 서버 로그 확인
```bash
# 서버 실행 후
tail -f server.log | grep -E "GameStarted|startGame"
```

### 클라이언트 로그 확인 (Android)
```bash
adb -s R3CY50NCDVB logcat | grep -E "RoomViewModel|GameScreen"
```

### 빌드 및 실행
```bash
# 서버 빌드
cd server
./gradlew build

# Android 빌드
./gradlew :composeApp:assembleDebug

# iOS 빌드 (macOS)
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp
```

## 성공 기준

- [x] 방장만 "작전 개시" 버튼 활성화
- [x] 최소 2명 미만 시 버튼 비활성화
- [x] 준비 안 된 참가자 있으면 버튼 비활성화
- [x] 게임 시작 시 역할 랜덤 배정
- [x] 모든 클라이언트가 자동으로 GameScreen 전환
- [x] GameScreen에서 자신의 역할 표시
- [x] GameScreen에서 참가자 목록 역할별로 표시
- [x] WebSocket 이벤트로 실시간 동기화

## 구현 완료!
모든 계획된 Phase가 성공적으로 구현되었습니다.
