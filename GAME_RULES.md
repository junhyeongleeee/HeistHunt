# HeistHunt 게임 룰

## 개요
경찰 vs 도둑의 실시간 위치 기반 술래잡기 게임

---

## 1. 게임 준비 단계

### 1.1 역할 선택
- ✅ **각 참가자가 경찰 또는 도둑 역할을 직접 선택**
- 현재 구현: ❌ 랜덤 배정 → **변경 필요**
- 경찰/도둑 비율은 방 설정에서 제한 가능

### 1.2 준비 시스템
- ✅ 방장 아닌 참가자들은 "준비" 버튼 클릭 필요
- ✅ 모든 참가자가 준비 완료해야 게임 시작 가능
- ✅ 방장만 "작전 개시" 버튼으로 게임 시작 가능
- 현재 구현: ✅ 완료

---

## 2. 게임 설정

### 2.1 게임 영역 설정
- **시작 위치 기준 반경 제한** (예: 500m, 1km 등)
- 방 생성 시 설정 가능
- 현재 구현: ❌ 없음 → **추가 필요**

### 2.2 시간 설정
1. **도주 시간** (기본값: 5분)
   - 게임 시작 후 도둑이 도망갈 수 있는 시간
   - 이 시간 동안 경찰은 도둑을 잡을 수 없음
   - 현재 구현: ❌ 없음 → **추가 필요**

2. **게임 제한 시간** (설정 가능)
   - 경찰이 도둑을 잡아야 하는 총 시간
   - 방 생성 시 설정 가능
   - 현재 구현: ✅ gameDurationMinutes (기본 30분)

---

## 3. 게임 진행

### 3.1 게임 시작
1. 모든 참가자 역할 선택 완료
2. 모든 참가자 준비 완료
3. 방장이 "작전 개시" 클릭
4. **도주 시간 시작** (5분)

### 3.2 도주 시간 (Phase 1: 0~5분)
**도둑의 행동**:
- 제한 시간 내에 도망
- 게임 영역 내에서만 이동 가능
- 경찰의 위치를 **볼 수 있음** (경찰이 룰을 지키는지 검증)

**경찰의 행동**:
- 도둑을 쫓아가지만 **아직 잡을 수 없음**
- 도둑의 위치는 볼 수 없음
- 도주 시간 동안 위치 파악 및 전략 수립

**화면 표시**:
- 도주 시간 남은 시간 카운트다운
- 도둑: 경찰 위치 + 도둑 위치 (아군만)
- 경찰: 경찰 위치만 (아군만)

### 3.3 추격 시간 (Phase 2: 5분~게임 종료)
**도둑의 행동**:
- 계속 도망
- 경찰 위치는 **더 이상 볼 수 없음**
- 도둑끼리만 위치 공유

**경찰의 행동**:
- 도둑 추적 및 체포
- 일정 거리 내 접근 시 "잡기" 버튼 활성화
- 경찰끼리만 위치 공유

**화면 표시**:
- 게임 남은 시간 카운트다운
- 역할에 따라 아군 위치만 표시
- 잡힌 도둑 수 / 전체 도둑 수

---

## 4. 위치 공유 규칙

### 4.1 도주 시간 (0~5분)
| 역할 | 볼 수 있는 위치 |
|------|----------------|
| 도둑 | 모든 경찰 위치 + 아군 도둑 위치 |
| 경찰 | 아군 경찰 위치만 |

### 4.2 추격 시간 (5분~종료)
| 역할 | 볼 수 있는 위치 |
|------|----------------|
| 도둑 | 아군 도둑 위치만 |
| 경찰 | 아군 경찰 위치만 |

**현재 구현**: ❌ 위치 공유 없음 → **추가 필요**

---

## 5. 게임 영역 제한

### 5.1 영역 설정
- **시작 위치**: 게임 시작 시점의 방장 위치 (또는 중심 좌표)
- **반경**: 방 생성 시 설정 (예: 500m, 1km, 2km)

### 5.2 영역 이탈 규칙
- **도둑만 해당**: 도둑이 반경 벗어나면 **자동 탈락**
- **경찰은 제한 없음**: 경찰은 영역 밖으로 나갈 수 있음 (도둑 추적용)

### 5.3 탈락 처리
1. 도둑이 반경 벗어남 감지
2. 해당 도둑 자동 탈락 처리
3. 모든 참가자에게 알림: "OOO님이 영역을 이탈하여 탈락했습니다"
4. 탈락한 도둑 수 업데이트

**현재 구현**: ❌ 없음 → **추가 필요**

---

## 6. 체포 메커니즘

### 6.1 체포 조건
- **거리**: 경찰과 도둑 사이 거리 ≤ 10m (설정 가능)
- **시간**: 도주 시간(5분) 이후에만 가능

### 6.2 체포 과정
1. 경찰이 도둑에게 접근 (10m 이내)
2. 경찰 화면에 "잡기" 버튼 활성화
3. 경찰이 "잡기" 버튼 클릭
4. 서버에서 거리 검증
5. 체포 성공 시:
   - 도둑 상태: `isCaught = true`
   - 모든 참가자에게 알림: "경찰 OOO님이 도둑 XXX님을 체포했습니다"
   - 잡힌 도둑은 더 이상 이동할 수 없음 (또는 관전 모드)

**현재 구현**: ❌ 없음 → **추가 필요**

---

## 7. 게임 종료 조건

### 7.1 경찰 승리 조건
다음 중 하나:
- **모든 도둑 체포**: 살아있는 도둑이 0명
- **모든 도둑 탈락**: 영역 이탈로 인한 탈락

### 7.2 도둑 승리 조건
- **제한 시간 초과**: 게임 시간 내에 살아남은 도둑이 1명 이상

### 7.3 종료 처리
1. 종료 조건 감지
2. 게임 상태 → `FINISHED`
3. 승리 팀 결정
4. 결과 화면 표시:
   - 승리 팀
   - 게임 통계 (잡힌 도둑 수, 탈락 도둑 수, 플레이 시간)
   - 각 플레이어별 결과

**현재 구현**: ❌ 없음 → **추가 필요**

---

## 8. 데이터 모델 변경 필요 사항

### 8.1 Room 설정 추가
```kotlin
data class RoomSettings(
    val maxPlayers: Int = 8,
    val policeRatio: Float = 0.3f,
    val gameDurationMinutes: Int = 30,
    val password: String? = null,
    // 추가 필요
    val gameAreaRadiusMeters: Int = 1000,  // 게임 영역 반경
    val escapeTimeMinutes: Int = 5,         // 도주 시간
    val captureDistanceMeters: Int = 10     // 체포 가능 거리
)
```

### 8.2 Participant 역할 선택
```kotlin
data class Participant(
    val userId: String,
    val nickname: String,
    val isReady: Boolean,
    val role: PlayerRole?,  // 현재: 게임 시작 시 할당
    // 변경: 참가자가 직접 선택
    val selectedRole: PlayerRole? = null  // 추가
)
```

### 8.3 Game 상태
```kotlin
enum class GamePhase {
    ESCAPE,   // 도주 시간 (0~5분)
    CHASE     // 추격 시간 (5분~종료)
}

data class Game(
    // ... 기존 필드
    val phase: GamePhase,  // 추가
    val escapeEndTime: Instant,  // 도주 시간 종료 시각
    val centerLat: Double,  // 게임 영역 중심 좌표
    val centerLng: Double
)
```

### 8.4 GamePlayer 상태
```kotlin
data class GamePlayer(
    // ... 기존 필드
    val isDisqualified: Boolean = false,  // 탈락 여부 (영역 이탈)
    val disqualifiedAt: Instant? = null   // 탈락 시각
)
```

---

## 9. 구현 우선순위

### Phase 1: 역할 선택 및 게임 설정 ✅ 다음 작업
1. 역할 선택 UI (RoomWaitingScreen)
2. 게임 설정 추가 (영역 반경, 도주 시간, 체포 거리)
3. 서버 DB 스키마 업데이트

### Phase 2: 위치 추적 및 지도
1. Google Maps 통합
2. 실시간 위치 공유
3. 게임 영역 표시 (원형 반경)
4. 역할/Phase에 따른 위치 필터링

### Phase 3: 게임 진행 로직
1. 도주 시간 / 추격 시간 구분
2. Phase별 타이머 및 UI 변경
3. 영역 이탈 감지 및 탈락 처리

### Phase 4: 체포 메커니즘
1. 거리 계산 (Haversine formula)
2. "잡기" 버튼 및 검증
3. 체포 알림 브로드캐스트

### Phase 5: 게임 종료 및 결과
1. 종료 조건 체크
2. 결과 화면
3. 통계 및 리플레이

---

## 10. WebSocket 이벤트 추가 필요

### 게임 중 이벤트
```kotlin
sealed class GameEvent {
    // 위치 업데이트
    data class LocationUpdate(
        val userId: String,
        val lat: Double,
        val lng: Double,
        val timestamp: Instant
    ) : GameEvent()

    // Phase 전환
    data class PhaseChanged(
        val newPhase: GamePhase,
        val remainingSeconds: Long
    ) : GameEvent()

    // 체포 알림
    data class PlayerCaptured(
        val policeId: String,
        val policeNickname: String,
        val thiefId: String,
        val thiefNickname: String
    ) : GameEvent()

    // 탈락 알림
    data class PlayerDisqualified(
        val userId: String,
        val nickname: String,
        val reason: String // "AREA_EXIT"
    ) : GameEvent()

    // 게임 종료
    data class GameEnded(
        val winner: GameWinner,
        val reason: String,
        val stats: GameStats
    ) : GameEvent()
}
```

---

## 11. API 엔드포인트 추가 필요

### 역할 선택
- `POST /api/rooms/{roomId}/select-role`
  - Body: `{ "role": "POLICE" | "THIEF" }`
  - Response: 업데이트된 Room

### 체포 요청
- `POST /api/games/{gameId}/capture`
  - Body: `{ "targetUserId": "..." }`
  - Response: 체포 성공/실패

### 위치 업데이트
- WebSocket을 통해 실시간 전송
- 또는 `POST /api/games/{gameId}/location`

---

## 현재 구현 상태 vs 요구사항

| 기능 | 현재 상태 | 필요 작업 |
|------|-----------|-----------|
| 역할 선택 | ❌ 랜덤 배정 | ✅ 선택 UI 추가 |
| 준비 시스템 | ✅ 완료 | - |
| 방장만 시작 | ✅ 완료 | - |
| 게임 영역 설정 | ❌ 없음 | ✅ 설정 UI + 검증 로직 |
| 도주 시간 | ❌ 없음 | ✅ Phase 시스템 추가 |
| 위치 공유 | ❌ 없음 | ✅ 지도 + WebSocket |
| 영역 이탈 감지 | ❌ 없음 | ✅ 위치 검증 로직 |
| 체포 메커니즘 | ❌ 없음 | ✅ 거리 계산 + UI |
| 게임 종료 | ❌ 없음 | ✅ 종료 조건 체크 |
| 결과 화면 | ❌ 없음 | ✅ 결과 UI |

---

## 다음 단계

사용자가 게임 중 화면을 디자인해서 가져올 예정.
그 전에 먼저 구현할 것:
1. **역할 선택 기능** (RoomWaitingScreen)
2. **게임 설정 확장** (영역 반경, 도주 시간 등)
