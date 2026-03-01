# HeistHunt 프로젝트 개발 가이드

## 프로젝트 개요
경찰과 도둑 게임 - 위치 기반 실시간 멀티플레이어 게임

## 필수 개발 규칙

### 1. 멀티 플랫폼 개발
- **모든 작업은 Android, iOS, Server를 모두 고려하여 개발**
- Kotlin Multiplatform 프로젝트 구조 유지
- 플랫폼별 특성을 고려한 구현

#### ⚠️ 필수: 문제 확인 시 양쪽 플랫폼 모두 체크
**어떤 문제든 발견하면 Android와 iOS 모두 확인 필수!**

- ❌ **잘못된 방식**: Android만 확인하고 수정
- ✅ **올바른 방식**: Android **그리고** iOS 모두 확인 후 수정

**적용 시점:**
1. 버그 발견 및 수정 시
2. 설정 변경 시 (API URL, 네트워크 설정 등)
3. 기능 추가 시
4. 라이브러리 업데이트 시
5. 빌드 에러 해결 시
6. 로그인/인증 문제 확인 시
7. **모든 문제 해결 시**

**예시:**
```
사용자: "로그인이 안 돼요"

❌ 나쁜 응답:
- Android 로그만 확인
- Android API URL만 수정
- Android 앱만 재설치

✅ 좋은 응답:
- Android 로그 확인
- iOS 로그도 확인
- Android API URL 수정
- iOS API URL도 수정
- Android 재설치
- iOS도 재설치
```

**규칙:**
- 하나의 플랫폼만 확인하는 것은 **작업 미완료**로 간주
- 수정 후 반드시 "Android와 iOS 모두 수정했습니다" 명시

### 2. 테스트 및 배포

#### 자동화된 테스트 프로세스
개발 완료 후 **테스트 에이전트를 통해 자동화**:
1. 개발 완료 즉시 E2E 테스트 에이전트 실행 (백그라운드)
2. 에이전트가 **Android + iOS 모두** 테스트 시나리오 작성, 코드 작성, 실행
3. 테스트 완료 후 결과 리포트 확인
4. 필요시 버그 수정 및 재테스트

#### 테스트 에이전트 실행 방법
사용자가 다음과 같이 요청:
```
"[기능명] E2E 테스트 에이전트 실행해줘"
```

예시:
- "게임 생성 화면 E2E 테스트 에이전트 실행해줘"
- "QR 스캔 기능 E2E 테스트 에이전트 실행해줘"
- "게임 플레이 화면 E2E 테스트 에이전트 실행해줘"

#### 테스트 에이전트 구현 방법
```
Task(
  subagent_type: "general-purpose",
  model: "sonnet",
  description: "E2E 테스트 작성 및 실행",
  run_in_background: true,
  prompt: """
  당신은 E2E 테스트 전문 에이전트입니다.
  [기능명] 기능에 대한 E2E 테스트를 작성하고 실행하세요.

  **중요: Android와 iOS 모두 테스트해야 합니다.**

  참고: .claude/test-agent-template.md
  스타일:
  - Android: LoginFlowTest.kt 따를 것
  - iOS: LoginFlowUITests.swift 따를 것
  """
)
```

#### iOS UI 테스트의 기술적 한계

**중요**: iOS XCUITest는 Compose Multiplatform UI를 테스트할 수 없습니다.

**이유:**
- Compose Multiplatform iOS는 UIKit View가 아닌 **Skia Canvas**에 직접 렌더링
- XCUITest는 UIKit View Hierarchy만 검사 가능
- Compose의 Button, Text 등은 XCUITest가 인식할 수 없는 픽셀일 뿐

**테스트 전략:**
- ✅ **Android**: Compose UI Testing API 사용 (정상 작동)
- ❌ **iOS**: XCUITest 사용 불가
- ✅ **공유 코드**: commonTest에서 ViewModel/비즈니스 로직 테스트

**검증 방식:**
1. **Android E2E 테스트로 UI 검증** (자동화)
   - Compose UI Testing API로 전체 UI 플로우 검증
   - Android 테스트 통과 = 공유 코드 검증 완료

2. **iOS 수동 테스트** (최종 확인)
   - iPhone 17 Pro 시뮬레이터에서 수동 검증
   - 플랫폼별 렌더링 이슈만 확인

3. **공유 코드 유닛 테스트** (자동화)
   - ViewModel, UseCase, Repository 테스트
   - 비즈니스 로직 검증

#### 수동 검증 (자동화 테스트 통과 후 최종 확인)
자동화된 E2E 테스트가 모두 통과한 후, 최종 확인용으로만 수행:
1. Android (Pixel 9 에뮬레이터)에서 실제 사용자 플로우 확인
2. iOS (iPhone 17 Pro 시뮬레이터)에서 실제 사용자 플로우 확인
3. 양쪽 플랫폼에서 UI/UX 최종 검증

### 3. UI/UX 개발 원칙
- 다양한 화면 비율 대응
- 화면 오버플로우 방지
- 반응형 레이아웃 구현

### 4. 로깅 가이드라인 (디버깅 효율화)

⚠️ **핵심 원칙**: 개발 시 충분한 로그를 남겨서 **문제 발생 시 즉시 원인을 파악**할 수 있도록 한다.

#### 4.1 로그를 반드시 추가해야 하는 시점

**1. 데이터 흐름의 주요 지점**
```kotlin
// ViewModel에서 상태 변경 시
_uiState.update { state ->
    println("🔄 [ViewModel] State updating:")
    println("  Previous: ${state.previousValue}")
    println("  New: $newValue")
    state.copy(value = newValue)
}
```

**2. 네트워크 통신**
```kotlin
// API 호출 전
println("📡 [API] Calling endpoint: POST /api/auth/login")
println("  Request: email=$email")

// API 응답 수신
println("✅ [API] Response received:")
println("  Status: ${response.status}")
println("  Body: ${response.body}")

// WebSocket 메시지
println("📥 [WebSocket] Received message: $messageType")
println("  Full JSON: $jsonString")
```

**3. 데이터 파싱/변환**
```kotlin
try {
    val event = json.decodeFromString<RoomEvent>(text)
    println("✅ [Parser] Successfully parsed: ${event::class.simpleName}")
    println("  Data: $event")
} catch (e: Exception) {
    println("❌ [Parser] Failed to parse JSON")
    println("  Error: ${e.message}")
    println("  JSON: $text")
}
```

**4. 네비게이션/화면 전환**
```kotlin
LaunchedEffect(shouldNavigate) {
    println("🧭 [Navigation] shouldNavigate changed: $shouldNavigate")
    println("  From: ${currentScreen.name}")
    println("  To: ${targetScreen.name}")
    println("  With data: gameId=$gameId, role=$role")
    if (shouldNavigate) {
        currentScreen = targetScreen
    }
}
```

**5. ViewModel/Repository 생성 및 초기화**
```kotlin
init {
    println("🎮 [GameViewModel] Initializing:")
    println("  gameId: $gameId")
    println("  myRole: $myRole")
    println("  startTime: $startTime")
    println("  startTime is null: ${startTime == null}")
}
```

**6. 비즈니스 로직의 중요 분기점**
```kotlin
if (myRole == null) {
    println("❌ [Critical] myRole is NULL!")
    println("  userId: $userId")
    println("  roleAssignments: $roleAssignments")
    println("  This will cause incorrect display!")
    return
}
```

#### 4.2 로그 포맷 규칙

**이모지 활용으로 로그 종류 구분:**
- 🎮 ViewModel/비즈니스 로직
- 📡 네트워크 요청
- 📥 데이터 수신
- ✅ 성공
- ❌ 에러
- 🔄 상태 변경
- 🧭 네비게이션
- 🎯 중요 체크포인트
- 🚀 액션 시작
- 🔌 연결/연결 해제

**로그 구조:**
```kotlin
println("[컴포넌트명] 동작: 상세정보")

// 좋은 예
println("🎮 [GameViewModel] Starting location tracking")
println("📡 [RoomRepository] Creating room: name=$name")
println("❌ [WebSocket] Connection failed: ${error.message}")

// 나쁜 예
println("Starting")  // 어디서? 무엇을?
println("Error")     // 무슨 에러? 어디서?
```

**중요 데이터는 여러 줄로 출력:**
```kotlin
println("═══════════════════════════════════════════")
println("🎮 [iOS] Creating NEW GameViewModel")
println("═══════════════════════════════════════════")
println("  viewModelKey: $viewModelKey")
println("  gameId: $gameId")
println("  myRole: $myRole (${myRole.name})")
println("  startTime: $startTime")
println("  startTime is null: ${startTime == null}")
println("  escapeDurationSeconds: $escapeDurationSeconds")
println("  totalDurationSeconds: $totalDurationSeconds")
println("═══════════════════════════════════════════")
```

#### 4.3 플랫폼별 로깅 고려사항

**iOS:**
- `println()`은 Xcode 콘솔 또는 `xcrun simctl spawn` 명령으로만 확인 가능
- 시뮬레이터 로그 캡처: `xcrun simctl spawn "iPhone 17 Pro" log stream --process HeistHunt`

**Android:**
- `println()`은 Logcat에 자동 출력
- Android Studio에서 필터 활용: `tag:System.out`

**공통:**
- 프로덕션 빌드에서는 로그 제거하지 않음 (문제 추적용)
- 민감 정보 (토큰, 비밀번호) 로그 시 마스킹: `token=***`

#### 4.4 로그로 문제를 빠르게 찾는 방법

**1. 이벤트 체인 추적**
```kotlin
// 사용자 액션 → API 호출 → 응답 → 상태 변경 → UI 업데이트
println("🚀 [Button] User clicked: Start Game")
// ... API 호출 ...
println("📡 [API] Calling: POST /rooms/$roomId/start")
// ... 응답 대기 ...
println("✅ [API] Response: gameId=$gameId, startTime=$startTime")
// ... 상태 업데이트 ...
println("🔄 [ViewModel] Updating state: shouldNavigateToGame=true")
// ... 네비게이션 ...
println("🧭 [Navigation] Navigating to Game screen")
```

**2. null 값 추적**
```kotlin
// 값이 null이 되는 지점을 모두 로그
println("📦 [Data] Value at point A: $value (is null: ${value == null})")
// ... 여러 단계 ...
println("📦 [Data] Value at point B: $value (is null: ${value == null})")
```

**3. 상태 변화 추적**
```kotlin
LaunchedEffect(uiState) {
    println("🔄 [StateObserver] UI State changed:")
    println("  myRole: ${uiState.myRole}")
    println("  gameId: ${uiState.gameId}")
    println("  startTime: ${uiState.startTime}")
}
```

#### 4.5 실전 예시: 오늘 해결한 버그

**문제:** iOS에서 경찰인데 도둑으로 표시, 타이머 작동 안 함

**로그 덕분에 빠르게 발견한 것들:**
```kotlin
// 1. WebSocket 파싱 실패 발견
println("❌ Error parsing WebSocket message: ${e.message}")
println("  JSON: $jsonString")
// → roleAssignments 필드 파싱 실패 확인

// 2. startTime이 null인 시점 확인
println("🎮 [GameViewModel] startTime: $startTime")
println("  startTime is null: ${startTime == null}")
// → API 응답에서 startedAt을 사용하지 않음 발견

// 3. roomDetailState가 초기화되는 시점 확인
println("🎯 [OperationScreen] roomDetailState.myRole: ${roomDetailState.myRole}")
// → null이 되는 시점 추적 가능
```

**교훈:** 로그가 없었다면 문제 찾는데 몇 시간 걸렸을 것!

---

### 5. 빌드 및 배포 전 필수 체크사항

### ⚠️ 앱 설치 시 필수 규칙 (매번 자동 적용)

**앱을 설치/재설치할 때마다 아래 3단계를 자동으로 수행:**

1. **현재 IP 확인**
   ```bash
   ipconfig getifaddr en0 || ipconfig getifaddr en1
   ```

2. **IP가 변경된 경우 3개 파일 모두 업데이트**
   - `composeApp/src/androidMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.android.kt`
   - `composeApp/src/iosMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.ios.kt`
   - `composeApp/src/androidMain/res/xml/network_security_config.xml`

3. **서버 실행 확인**
   ```bash
   lsof -i :8080 | grep LISTEN
   ```

> 사용자가 별도로 요청하지 않아도 설치 전 항상 자동 수행

---

⚠️ **중요**: 빌드 전 **반드시** API Endpoint URL이 올바르게 설정되어 있는지 확인하세요!

#### 5.1 빌드 전 필수 확인: 테스트 환경에 맞는 URL 설정

**로그인 오류를 방지하려면 빌드 전 항상 다음을 확인:**

**시뮬레이터/에뮬레이터에서 테스트하는 경우:**
- ✅ Android Emulator: `http://10.0.2.2:8080` 사용 필수
- ✅ iOS Simulator: `http://localhost:8080` 사용 필수
- ❌ 실제 IP 주소 (예: 192.168.x.x) 사용하면 연결 실패!

**실제 기기에서 테스트하는 경우:**
- ✅ Android 기기: `http://[컴퓨터_IP]:8080` 사용 필수
- ✅ iOS 기기: `http://[컴퓨터_IP]:8080` 사용 필수
- ❌ localhost나 10.0.2.2 사용하면 연결 실패!

**확인해야 할 파일 2개:**
```
composeApp/src/androidMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.android.kt
composeApp/src/iosMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.ios.kt
```

#### 5.2 시뮬레이터/에뮬레이터용 설정 (기본값)

**1. Android Emulator**
- 파일: `composeApp/src/androidMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.android.kt`
```kotlin
actual fun getPlatformBaseUrl(): String {
    // Android Emulator에서는 10.0.2.2를 사용하여 호스트의 localhost에 접근
    return "http://10.0.2.2:8080"
}
```

**2. iOS Simulator**
- 파일: `composeApp/src/iosMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.ios.kt`
```kotlin
actual fun getPlatformBaseUrl(): String {
    // iOS Simulator는 localhost를 직접 사용
    return "http://localhost:8080"
}
```

#### 5.3 실제 기기 테스트용 설정

**현재 IP 주소 확인:**
```bash
ipconfig getifaddr en0 || ipconfig getifaddr en1
```

**설정 파일 수정 (3개 파일 모두 업데이트 필수):**

**1. Android 기기**
- 파일: `composeApp/src/androidMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.android.kt`
```kotlin
actual fun getPlatformBaseUrl(): String {
    return "http://[현재_IP_주소]:8080"  // 예: http://192.168.45.41:8080
}
```

**2. iOS 기기**
- 파일: `composeApp/src/iosMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.ios.kt`
```kotlin
actual fun getPlatformBaseUrl(): String {
    return "http://[현재_IP_주소]:8080"  // 예: http://192.168.45.41:8080
}
```

**3. Android Network Security Config (HTTP 허용)**
- 파일: `composeApp/src/androidMain/res/xml/network_security_config.xml`
- 현재 IP 주소를 **맨 위에** 추가:
```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">[현재_IP_주소]</domain>  <!-- 새로 추가 -->
    <!-- 기존 IP 주소들... -->
</domain-config>
```

#### 5.4 서버 재시작 (shared 모듈 변경 시)
shared 모듈의 DTO나 모델을 수정한 경우 서버 재시작 필수:
```bash
# 1. shared 모듈 빌드
./gradlew :shared:build

# 2. 기존 서버 종료 (포트 확인 후)
lsof -i :8080  # PID 확인
kill [PID]

# 3. 서버 재시작
./gradlew :server:run &
```

#### 5.5 앱 빌드 및 설치

**Android:**
```bash
./gradlew :composeApp:installDebug
```

**iOS:**
```bash
cd iosApp
xcodebuild -workspace iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  clean build
```

#### 5.6 일반적인 오류 및 해결법

**"Connection timeout" 또는 "Connect timeout has expired" (로그인 실패)**
- 원인: API endpoint URL이 잘못됨
- 해결:
  1. **테스트 환경 확인**: 에뮬레이터/시뮬레이터인가? 실제 기기인가?
  2. **에뮬레이터/시뮬레이터**: 위 5.2 설정 확인 (Android: 10.0.2.2, iOS: localhost)
  3. **실제 기기**: 위 5.3 설정 확인 (현재 IP 주소 사용)
  4. 서버 실행 확인 (`lsof -i :8080`)
  5. **Android와 iOS 모두 확인!**

**"CLEARTEXT communication not permitted"**
- 원인: network_security_config.xml에 IP 주소 누락
- 해결: 위 5.3의 3번 파일 수정 후 재빌드

**서버 "GoogleLoginRequest 클래스 없음" 오류**
- 원인: shared 모듈이 서버에 반영되지 않음
- 해결: 위 5.4 서버 재시작 절차 실행

---

## 인증 시스템

### 로그인 / 회원가입
- **회원가입은 별도로 존재하지 않음**
- 소셜 로그인 = 로그인 + 회원가입
- 처음 로그인하는 사용자는 소셜 로그인 정보로 자동 회원가입 처리

### 지원하는 소셜 로그인
1. Google 로그인
2. 카카오 로그인 (개발 예정)
3. Apple 로그인 (개발 예정)

### 자동 로그인
- **자동 로그인은 항상 활성화**
- 별도 설정 기능 없음
- 한 번 로그인하면 앱 종료 후에도 로그인 상태 유지
- 다른 계정으로 로그인하려면 반드시 로그아웃 필요

### 로그아웃
- 로그아웃을 통해서만 계정 전환 가능
- 로그아웃 시 자동 로그인 정보 삭제

---

## 게임 기획 명세

### 게임 개요
- **게임 타입**: 경찰과 도둑 추격전
- **게임 진행**: 도망치는 시간 → 잡는 시간 (순차 진행)

### 알림(Notification) 정의
알림은 다음과 같은 형태로 구현 (우선순위 순):
1. **알럿(Alert) 표시** - 최우선 개발
2. 핸드폰 진동
3. Push Notification

---

## 게임 페이즈

### 1. 도망치는 시간 (Escape Phase)
**시작 조건:**
- 게임 시작 즉시 활성화
- 기본 시간: **5분** (추후 설정 기능 추가 예정)

**경찰 (Police) 규칙:**
- 시작점(감옥)에서 이동 불가
- **반경 1m 이탈 시**: 모든 도둑에게 알림 발송
- 위치 정보: 볼 수 없음

**도둑 (Thief) 규칙:**
- 정해진 반경 내 자유 이동
- **위치 정보**: 모든 게임 유저의 위치를 지도에서 실시간 확인 가능

### 2. 추격하는 시간 (Chase Phase)
**시작 조건:**
- 도망치는 시간 종료 후 자동 전환

**위치 정보 공유 규칙:**
- 도둑: 도둑만의 위치 확인 가능
- 경찰: 경찰만의 위치 확인 가능

**근접 감지 시스템:**
- **반경 5m 이내** 상대 진영 감지 시 알림 발송
  - 도둑 → "경찰 감지!" 알림
  - 경찰 → "도둑 감지!" 알림

---

## 체포 메커니즘

### 체포 프로세스
1. **경찰의 체포 선언**
   - 실제로 도둑을 잡았을 경우
   - 앱에서 해당 도둑을 선택하여 체포 신청

2. **도둑의 확인**
   - 체포 알럿 팝업 표시
   - "정말 잡혔습니까?" 확인 요청

3. **체포 승인 시**
   - 모든 유저에게 체포 알림 발송
   - 잡힌 도둑은 감옥(시작점)으로 이동해야 함
   - 경찰은 계속 다른 도둑 추격

### 감옥 탈출
- 잡힌 도둑의 감옥 탈출 기능은 **추후 개발 예정**

---

## 게임 종료 조건

### 경찰 승리
- 제한 시간 내 **모든 도둑 체포** 완료

### 도둑 승리
- 제한 시간 내 **1명 이상의 도둑이 생존**

---

## 실시간 위치 공유

### 지도 시스템
- 모든 유저의 위치를 지도에 실시간 표시
- 페이즈 및 역할에 따른 가시성 제어
- 공유 가능한 유저만 표시

### 위치 업데이트
- 실시간 위치 동기화
- 서버와의 지속적인 통신
- 정확한 거리 계산 (1m, 5m 반경 감지)

---

## 개발 우선순위

### 현재 구현 완료
- 기본 게임 플로우
- 역할 할당 (경찰/도둑)
- 실시간 위치 공유

### 개발 예정 기능
1. ~~알럿 기반 알림 시스템~~ (최우선)
2. 진동 알림
3. Push Notification
4. 도망치는 시간 설정 기능
5. 감옥 탈출 메커니즘

---

## 기술 스택
- **Frontend**: Kotlin Multiplatform, Compose Multiplatform
- **Backend**: Firebase (Realtime Database, Authentication)
- **Maps**: Google Maps (Android), Apple Maps (iOS)
- **Location**: 위치 기반 서비스

---

## 주의사항
- 위치 권한 처리 필수
- 백그라운드 위치 추적 최적화
- 배터리 소모 최소화
- 네트워크 오류 처리
- 동시성 이슈 고려 (실시간 멀티플레이어)

---

## CLAUDE.md 파일 수정 규칙

⚠️ **중요**: 이 파일의 내용은 자동으로 수정하지 않는다.
- 사용자가 명시적으로 "추가해줘", "수정해줘" 등을 요청할 때만 수정
- 수정 전 반드시 변경 내용을 미리 보여주고 확인 받기
- 임의로 내용 추가/삭제/변경 금지
