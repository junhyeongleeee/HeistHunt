# 체포 메커니즘 구현 완료

## 개요
HeistHunt 게임의 2단계 체포 확인 시스템이 완전히 구현되었습니다.

## 구현된 기능

### 1. 거리 감지 (5m 반경)
- **위치**: `shared/src/commonMain/kotlin/com/heisthunt/shared/utils/GeoUtils.kt`
- Haversine 공식을 사용한 정확한 거리 계산
- `isWithinRadius()` 함수로 5m 이내 판단

### 2. WebSocket 메시지 타입 (3가지)
- **위치**: `shared/src/commonMain/kotlin/com/heisthunt/shared/dto/GameDto.kt`

```kotlin
// 1. 경찰 → 도둑 체포 요청
CatchRequest(policeUserId, policeNickname, thiefUserId)

// 2. 도둑 → 전체 체포 확인
CatchConfirmed(thiefUserId, thiefNickname, jailLatitude, jailLongitude)

// 3. 도둑 → 전체 체포 거부
CatchRejected(thiefUserId, reason)
```

### 3. 서버 측 처리
- **위치**: `server/src/main/kotlin/com/heisthunt/server/routes/GameRoutes.kt`

**체포 흐름**:
1. 경찰이 `CatchRequest` 전송 → 특정 도둑에게만 전달 (`sendToUser`)
2. 도둑이 `CatchConfirmed` 전송 → DB 업데이트 + 전체 브로드캐스트
   - `GamePlayers.isCaught = true`
   - `GamePlayers.caughtAt = now()`
   - 모든 도둑이 잡혔는지 확인 → 게임 종료
3. 도둑이 `CatchRejected` 전송 → 전체 브로드캐스트

**감옥 위치 저장**:
- 도둑이 처음으로 위치 업데이트를 보낼 때 자동 저장
- `GamePlayers.jailLatitude`, `GamePlayers.jailLongitude`에 저장

### 4. 클라이언트 ViewModel
- **위치**: `composeApp/src/commonMain/kotlin/com/heisthunt/app/viewmodel/GameViewModel.kt`

**새로운 상태**:
```kotlin
data class GameUiState(
    // ... 기존 필드
    val nearbyThieves: List<PlayerLocation> = emptyList(), // 경찰: 5m 이내 도둑 목록
    val catchRequest: CatchRequestState? = null, // 도둑: 수신한 체포 요청
    val jailLocation: Location? = null, // 잡힌 도둑: 감옥 위치
    val isCaught: Boolean = false // 체포 여부
)
```

**새로운 함수**:
- `updateNearbyThieves()` - 경찰의 위치 업데이트마다 5m 이내 도둑 감지
- `requestCatch(thiefUserId, policeUserId, policeNickname)` - 경찰이 체포 요청
- `confirmCatch(thiefUserId, thiefNickname)` - 도둑이 체포 확인
- `rejectCatch(thiefUserId)` - 도둑이 체포 거부
- `dismissCatchRequest()` - 체포 요청 다이얼로그 닫기

### 5. UI 컴포넌트

#### A. 경찰용 감지 알림
```kotlin
@Composable
fun CatchDetectionAlert(nearbyThievesCount: Int)
```
- 5m 이내 도둑이 있을 때 지도 상단에 표시
- "🎯 도둑 감지! 반경 5m 이내에 도둑 N명"
- 깜빡이는 애니메이션

#### B. 경찰용 체포 버튼
```kotlin
@Composable
fun ActionFooter(..., nearbyThieves: List<PlayerLocation>)
```
- 도둑이 5m 이내에 있을 때만 활성화
- "I GOT 'EM!" 버튼 → 도둑 목록 다이얼로그

#### C. 경찰용 대상 선택 다이얼로그
```kotlin
@Composable
fun CatchTargetDialog(nearbyThieves, onSelectThief, onDismiss)
```
- 감지된 모든 도둑의 목록 표시
- 각 도둑의 userId와 위치 좌표 표시
- 선택 시 해당 도둑에게 체포 요청 전송

#### D. 도둑용 체포 확인 다이얼로그
```kotlin
@Composable
fun CatchRequestDialog(policeNickname, onConfirm, onReject, onDismiss)
```
- "🚨 체포 요청"
- "경찰 'XXX'이(가) 당신을 체포했다고 주장합니다. 정말 잡혔습니까?"
- **인정 (감옥으로 이동)** 버튼 - 빨간색
- **거부** 버튼 - 회색

## 데이터 흐름

### 경찰 시점
```
1. 위치 업데이트 (5초마다)
   ↓
2. updateNearbyThieves() - GeoUtils로 거리 계산
   ↓
3. nearbyThieves.isNotEmpty() → CatchDetectionAlert 표시
   ↓
4. 경찰이 "I GOT 'EM!" 버튼 클릭
   ↓
5. CatchTargetDialog 표시 - 도둑 목록
   ↓
6. 경찰이 도둑 선택
   ↓
7. requestCatch() → WebSocket 전송 (CatchRequest)
   ↓
8. 서버가 특정 도둑에게만 전달
```

### 도둑 시점
```
1. WebSocket으로 CatchRequest 수신
   ↓
2. catchRequest 상태 업데이트
   ↓
3. CatchRequestDialog 자동 표시
   ↓
4. 도둑이 "인정" 또는 "거부" 선택
   ↓
5-A. 인정 → confirmCatch() → CatchConfirmed 전송
   - isCaught = true
   - jailLocation 설정
   - 서버: DB 업데이트, 모든 플레이어에게 브로드캐스트
   - 모든 도둑 체포 시 게임 종료
   ↓
5-B. 거부 → rejectCatch() → CatchRejected 전송
   - 서버: 모든 플레이어에게 브로드캐스트
   - 경찰에게 "도둑이 체포를 거부했습니다" 메시지
```

## 게임 종료 조건

### 경찰 승리
```kotlin
// GameRoutes.kt: 267-306
val allCaught = thieves.all { it[GamePlayers.isCaught] }

if (allCaught) {
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
```

## 테스트 방법

### 필요 조건
- 최소 2대의 기기 (경찰 1명, 도둑 1명)
- 같은 게임 세션에 참여
- GPS 활성화

### 시나리오 1: 정상 체포
1. 두 플레이어가 5m 이내로 접근
2. 경찰 화면에 "🎯 도둑 감지!" 알림 표시 확인
3. 경찰이 "I GOT 'EM!" 버튼 클릭
4. 도둑 목록 다이얼로그 확인
5. 도둑 선택
6. 도둑 화면에 체포 요청 다이얼로그 표시 확인
7. 도둑이 "인정" 버튼 클릭
8. 모든 플레이어에게 "XXX가 체포되었습니다!" 메시지 표시 확인
9. 도둑 화면에 jailLocation 표시 (감옥으로 돌아가야 함)

### 시나리오 2: 체포 거부
1. 시나리오 1의 1~6 단계 동일
2. 도둑이 "거부" 버튼 클릭
3. 모든 플레이어에게 "도둑이 체포를 거부했습니다" 메시지 표시 확인
4. 게임 계속 진행

### 시나리오 3: 게임 종료
1. 모든 도둑이 체포 확인
2. "Game Over! POLICE wins!" 메시지 표시
3. 위치 추적 자동 중지 확인

## 디버깅

### 로그 확인 (서버)
```bash
# 서버 로그 모니터링
tail -f server/logs/application.log

# 주요 로그 메시지:
# - "Catch request from police X to thief Y"
# - "Catch confirmed by thief X"
# - "Game X ended - POLICE wins (all thieves caught)"
```

### 로그 확인 (Android)
```bash
# Android 로그캣 필터링
adb logcat | grep -E "(GameViewModel|GameConnectionManager|WebSocket)"

# 주요 로그 메시지:
# - "Nearby thieves: N"
# - "Police X requested to catch thief Y"
# - "Thief X confirmed caught, jail: lat, lon"
```

## 알려진 제약사항

### TODO 항목
1. **사용자 정보 가져오기**: 현재 userId와 nickname이 플레이스홀더
   - GameScreenContainer.android.kt:70-71
   - 인증 상태에서 실제 값 가져오기 필요

2. **감옥 위치 표시**: 지도에 감옥 위치 마커 추가
   - 잡힌 도둑이 감옥으로 돌아가는 경로 표시

3. **거리 표시**: 경찰 화면에 각 도둑까지의 거리 표시
   - CatchTargetDialog에서 "약 3.2m 떨어짐" 같은 정보 추가

4. **진동/소리 효과**: 체포 요청 시 햅틱 피드백
   - 도둑이 체포 요청받을 때 진동

5. **재체포 방지**: 이미 잡힌 도둑은 nearbyThieves에서 제외
   - updateNearbyThieves()에 필터 추가

## 구현 파일 목록

### 서버 (2개 수정)
- `server/src/main/kotlin/com/heisthunt/server/database/Tables.kt` - jail 위치 필드 추가
- `server/src/main/kotlin/com/heisthunt/server/routes/GameRoutes.kt` - 체포 메시지 핸들러

### Shared (2개 수정)
- `shared/src/commonMain/kotlin/com/heisthunt/shared/dto/GameDto.kt` - 체포 메시지 타입
- `shared/src/commonMain/kotlin/com/heisthunt/shared/utils/GeoUtils.kt` - 거리 계산

### 클라이언트 (4개 수정)
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/viewmodel/GameViewModel.kt` - 체포 로직
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/network/GameWebSocketClient.kt` - 체포 메시지 전송
- `composeApp/src/commonMain/kotlin/com/heisthunt/app/ui/game/GameScreen.kt` - UI 컴포넌트
- `composeApp/src/androidMain/kotlin/com/heisthunt/app/ui/game/GameScreenContainer.android.kt` - 콜백 연결
- `composeApp/src/iosMain/kotlin/com/heisthunt/app/ui/game/GameScreenContainer.ios.kt` - 플레이스홀더

## 다음 단계

1. **감옥 내비게이션 시스템**
   - 잡힌 도둑이 감옥으로 돌아가는 경로 안내
   - 감옥 도착 확인 (10m 이내)
   - 도착 시 상태 업데이트

2. **승리 조건 UI**
   - 게임 종료 화면 개선
   - 경찰/도둑 승리 애니메이션
   - 게임 결과 통계 표시

3. **성능 최적화**
   - 거리 계산 캐싱
   - WebSocket 메시지 배치 처리
   - UI 리컴포지션 최소화

## 성공 기준 ✅

- [x] 경찰이 5m 이내 도둑을 감지할 수 있음
- [x] 경찰이 도둑에게 체포 요청을 보낼 수 있음
- [x] 도둑이 체포 요청을 받고 인정/거부할 수 있음
- [x] 체포 확인 시 도둑의 감옥 위치가 저장됨
- [x] 모든 도둑 체포 시 게임이 종료됨
- [x] WebSocket을 통한 실시간 동기화
- [x] 역할별 UI 차별화 (경찰/도둑)
