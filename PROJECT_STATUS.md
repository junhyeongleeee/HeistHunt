# HeistHunt 프로젝트 진행 상황

마지막 업데이트: 2026-01-31

---

## 📊 전체 진행률

### Phase 1 (MVP) 진행률: **40%**

```
■■■■□□□□□□ 4/10 완료
```

---

## ✅ 완료된 작업

### 1. 프로젝트 기반 구조 ✅
- [x] KMP (Kotlin Multiplatform) 프로젝트 셋업
- [x] Compose Multiplatform 설정
- [x] Android/iOS 타겟 설정
- [x] Gradle 빌드 구성

### 2. 서버 인프라 ✅
- [x] Ktor 서버 프로젝트 생성 (`server/`)
- [x] PostgreSQL 데이터베이스 연결
- [x] Exposed ORM 설정
- [x] 기본 서버 구조 (라우팅, 인증, CORS 등)

### 3. 공유 데이터 모델 ✅
- [x] `shared` 모듈 생성
- [x] Room 모델 (방 정보)
- [x] User 모델 (사용자 정보)
- [x] Participant 모델 (참가자 정보)
- [x] DTO 정의 (CreateRoomRequest, RoomSummary 등)

### 4. 인증 시스템 ✅
**클라이언트:**
- [x] Firebase Authentication 통합
- [x] Google Sign-In (Android)
- [x] 이메일/비밀번호 로그인
- [x] 회원가입 화면
- [x] AuthViewModel 구현
- [x] AuthRepository 구현
- [x] 로그인/회원가입 UI

**서버:**
- [x] JWT 기반 인증
- [x] POST /api/auth/register
- [x] POST /api/auth/login
- [x] 비밀번호 해싱 (BCrypt)

### 5. 방 생성 기능 ✅
**클라이언트:**
- [x] RoomViewModel 구현
- [x] RoomRepository 구현
- [x] OperationScreen UI (방 생성 버튼)
- [x] RoomWaitingScreen UI (대기 화면)
- [x] QRCodeDisplay 컴포넌트 (QRose 라이브러리)
- [x] 방 생성 플로우 (로딩/에러 처리)

**서버:**
- [x] POST /api/rooms (방 생성 API)
- [x] 방 코드 자동 생성 (8자리 랜덤)
- [x] 데이터베이스 저장

### 6. 네트워크 레이어 ✅
- [x] Ktor Client 설정
- [x] ApiClient 구현
- [x] JSON 직렬화 (kotlinx.serialization)
- [x] 에러 핸들링

### 7. DI (의존성 주입) ✅
- [x] AppModule (수동 DI)
- [x] ViewModel 프로바이더
- [x] Repository 프로바이더

### 8. UI 기초 ✅
- [x] Material3 테마
- [x] 네비게이션 시스템 (Screen enum)
- [x] Operation Center 화면

---

## 🚧 진행 중인 작업

### QR 코드 기능 (80% 완료)
- [x] QR 코드 생성 (QRose)
- [x] QR 코드 표시
- [ ] **QR 코드 스캔** ← 다음 작업
- [ ] 카메라 권한 처리
- [ ] QR 스캔 후 자동 방 참여

---

## ❌ 미완료 작업 (Phase 1 MVP)

### 1. 방 참여 기능 (우선순위: 🔴 최상)
- [ ] QR 코드 스캔 구현
  - [ ] Android 카메라 연동 (CameraX)
  - [ ] QR 코드 디코딩 (ZXing 또는 ML Kit)
  - [ ] iOS 카메라 연동 (AVFoundation)
  - [ ] 카메라 권한 요청
- [ ] 방 참여 API 호출
  - [x] POST /api/rooms/{roomId}/join (서버 구현됨)
  - [ ] 클라이언트 연동
- [ ] 참여 완료 후 RoomWaitingScreen으로 이동

### 2. 실시간 참가자 업데이트 (우선순위: 🔴 최상)
- [ ] WebSocket 연결
  - [ ] 서버 WebSocket 엔드포인트 구현
  - [ ] 클라이언트 WebSocket 연결
  - [ ] 재연결 로직
- [ ] 실시간 이벤트 처리
  - [ ] 참가자 입장 이벤트
  - [ ] 참가자 퇴장 이벤트
  - [ ] 준비 상태 변경 이벤트
- [ ] UI 자동 업데이트

### 3. 게임 시작 준비 (우선순위: 🟡 높음)
- [ ] 준비 버튼 구현
  - [ ] 준비 상태 토글 API
  - [ ] 준비 상태 UI 표시
- [ ] 게임 시작 조건 체크
  - [ ] 최소 인원 확인
  - [ ] 모든 참가자 준비 확인
- [ ] "작전 개시" 버튼 활성화 로직

### 4. 역할 배정 시스템 (우선순위: 🟡 높음)
- [ ] 역할 배정 알고리즘
  - [ ] 경찰/도둑 비율 계산
  - [ ] 랜덤 배정 로직
- [ ] 역할 배정 API
  - [ ] POST /api/rooms/{roomId}/assign-roles
- [ ] 역할 표시 UI
  - [ ] 배정된 역할 알림
  - [ ] 역할별 색상 구분

### 5. 위치 추적 시스템 (우선순위: 🔴 최상)
- [ ] 위치 권한 처리
  - [ ] Android (ACCESS_FINE_LOCATION)
  - [ ] iOS (Location When In Use)
- [ ] GPS 위치 가져오기
  - [ ] Android (FusedLocationProvider)
  - [ ] iOS (CoreLocation)
- [ ] 백그라운드 위치 추적
  - [ ] Android (Foreground Service)
  - [ ] iOS (Background Location Updates)
- [ ] 위치 데이터 전송
  - [ ] WebSocket으로 실시간 전송
  - [ ] 배터리 최적화 고려

### 6. 지도 표시 (우선순위: 🟡 높음)
- [ ] 지도 SDK 통합
  - [ ] Google Maps Android
  - [ ] Google Maps iOS
  - [ ] 대안: Naver Maps
- [ ] 플레이어 위치 마커
  - [ ] 경찰 마커 (파란색)
  - [ ] 도둑 마커 (빨간색)
  - [ ] 자신의 위치 (녹색)
- [ ] 실시간 위치 업데이트

### 7. 게임 진행 로직 (우선순위: 🟠 중간)
- [ ] 게임 타이머
  - [ ] 카운트다운 UI
  - [ ] 시간 종료 처리
- [ ] 잡기 메커니즘
  - [ ] 거리 계산 (경찰-도둑)
  - [ ] 잡기 범위 설정 (예: 10m 이내)
  - [ ] 잡기 액션 버튼
- [ ] 잡힌 플레이어 처리
  - [ ] 상태 변경 (isCaught = true)
  - [ ] UI 업데이트
  - [ ] 알림

### 8. 게임 종료 및 결과 (우선순위: 🟠 중간)
- [ ] 승리 조건 체크
  - [ ] 모든 도둑 잡힘 → 경찰 승리
  - [ ] 시간 종료 → 도둑 승리
- [ ] 게임 결과 화면
  - [ ] 승리/패배 표시
  - [ ] 게임 통계
  - [ ] MVP 선정 (옵션)
- [ ] 게임 기록 저장

---

## 📋 Phase 2 이후 기능 (낮은 우선순위)

### Phase 2: 핵심 기능 강화
- [ ] 알림 시스템 (FCM)
- [ ] 게임 통계 및 히스토리
- [ ] 프로필 관리 (사진, 닉네임 변경)
- [ ] 다양한 게임 모드
- [ ] 채팅 기능

### Phase 3: 고급 기능
- [ ] 친구 시스템
- [ ] 팀 기능
- [ ] 랭킹 시스템
- [ ] 게임 리플레이
- [ ] 업적/뱃지 시스템

---

## 🔥 즉시 해야 할 작업 (우선순위 순)

### 1순위: QR 코드 스캔 및 방 참여 (1-2일)
```
목표: 사용자가 QR 코드를 스캔하여 방에 참여할 수 있게 함

작업:
1. CameraX 또는 ML Kit로 QR 스캔 구현 (Android)
2. 스캔한 코드로 POST /api/rooms/{roomId}/join 호출
3. 성공 시 RoomWaitingScreen으로 이동
4. iOS 카메라 구현 (expect/actual 패턴)
```

### 2순위: WebSocket 실시간 업데이트 (2-3일)
```
목표: 방에 참가자가 들어오면 실시간으로 UI 업데이트

작업:
1. Ktor 서버에 WebSocket 라우팅 추가
2. 클라이언트 WebSocket 연결 (Ktor Client)
3. 참가자 입장/퇴장 이벤트 브로드캐스트
4. RoomViewModel에서 WebSocket 메시지 수신
5. StateFlow 업데이트로 UI 자동 갱신
```

### 3순위: 위치 추적 기본 구현 (3-4일)
```
목표: 게임 중 플레이어 위치를 실시간으로 가져옴

작업:
1. 위치 권한 요청 (Android/iOS)
2. FusedLocationProvider로 현재 위치 가져오기
3. 주기적으로 위치 업데이트 (예: 5초마다)
4. WebSocket으로 위치 데이터 전송
5. 서버에서 위치 저장 및 브로드캐스트
```

### 4순위: 지도에 플레이어 표시 (2-3일)
```
목표: 지도에 모든 플레이어 위치를 실시간으로 표시

작업:
1. Google Maps SDK 통합
2. 지도 화면 생성 (GameMapScreen)
3. 플레이어 위치를 마커로 표시
4. WebSocket으로 받은 위치 업데이트
5. 역할별 색상 구분 (경찰/도둑)
```

### 5순위: 게임 시작 및 역할 배정 (1-2일)
```
목표: 방장이 게임을 시작하면 자동으로 역할 배정

작업:
1. "작전 개시" 버튼 활성화 조건 체크
2. POST /api/rooms/{roomId}/start 구현
3. 역할 배정 알고리즘 (경찰/도둑 비율)
4. 각 참가자에게 역할 알림
5. 게임 화면으로 전환
```

---

## 🛠 기술적 도전 과제

### 1. 백그라운드 위치 추적
**문제:** Android/iOS에서 앱이 백그라운드일 때도 위치 추적 필요

**해결책:**
- Android: Foreground Service + Notification
- iOS: Background Location Updates + Always 권한

### 2. 배터리 소모 최적화
**문제:** 실시간 위치 추적은 배터리를 많이 소모

**해결책:**
- 게임 중에만 위치 추적 활성화
- 위치 업데이트 간격 조절 (5-10초)
- 저전력 모드 옵션 제공

### 3. 네트워크 안정성
**문제:** WebSocket 연결이 끊길 수 있음

**해결책:**
- 자동 재연결 로직
- 백오프 알고리즘
- 오프라인 상태 UI 표시

### 4. 멀티플랫폼 카메라 구현
**문제:** Android와 iOS 카메라 API가 다름

**해결책:**
- expect/actual 패턴 사용
- Android: CameraX
- iOS: AVFoundation (Swift)
- 공통 인터페이스 정의

---

## 📈 다음 마일스톤

### Milestone 1: 방 참여 완성 (목표: 1주)
- QR 스캔
- 방 참여
- 실시간 참가자 목록

### Milestone 2: 게임 시작 (목표: 2주)
- 역할 배정
- 게임 상태 관리
- 타이머

### Milestone 3: 실시간 위치 추적 (목표: 3-4주)
- GPS 연동
- WebSocket 위치 전송
- 지도 표시

### Milestone 4: 게임 완성 (목표: 6-8주)
- 잡기 메커니즘
- 게임 종료
- 결과 화면

---

## 📝 참고 사항

### 현재 작동하는 기능
1. Google 로그인
2. 이메일/비밀번호 로그인/회원가입
3. 방 생성 (POST /api/rooms)
4. QR 코드 생성 및 표시
5. 방 대기 화면

### 테스트 가능한 시나리오
1. ✅ 앱 실행 → Google 로그인 → Operation Center
2. ✅ "작전 설계하기" → QR 코드 표시
3. ❌ QR 코드 스캔 → 방 참여 (아직 불가)
4. ❌ 게임 시작 (아직 불가)

### 필요한 외부 서비스
- [x] Firebase (Authentication)
- [ ] Firebase Cloud Messaging (알림)
- [ ] Google Maps API (지도)
- [x] PostgreSQL (데이터베이스)

---

## 🎯 최종 목표 (MVP)

**"2명 이상이 QR로 방에 참여하여, 실시간 위치를 공유하며 경찰과 도둑 게임을 완료할 수 있다"**

현재 진행률: **40%**

남은 핵심 작업:
- QR 스캔 (10%)
- 실시간 업데이트 (15%)
- 위치 추적 (20%)
- 게임 로직 (15%)

---

## 📞 문의 및 이슈

현재 알려진 이슈:
- 없음 (신규 프로젝트)

다음 작업 시작 전 확인 필요:
1. Google Maps API 키 발급
2. Firebase 프로젝트 설정 완료 확인
3. 서버 배포 환경 준비 (현재 로컬만 가능)
