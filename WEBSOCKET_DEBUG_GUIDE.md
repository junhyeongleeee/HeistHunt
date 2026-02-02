# WebSocket 디버깅 가이드

## 문제: QR 코드로 방 입장 시 방장이 새 참가자를 인식하지 못함

### 개선 사항
- ✅ RoomWaitingScreen 상단에 WebSocket 연결 상태 표시 추가
  - 🟢 녹색 = 연결됨
  - 🟡 노란색 = 연결 중...
  - 🔴 빨간색 = 연결 오류
  - ⚫ 회색 = 연결 안됨

## 테스트 시나리오

### 1. 기본 테스트
1. **방장 (에뮬레이터)**
   - 방 생성
   - RoomWaitingScreen으로 이동
   - **WebSocket 연결 상태 확인**: 🟢 "연결됨"이어야 함

2. **참가자 (삼성 갤럭시)**
   - QR 코드 스캔하여 방 입장
   - **기대 결과**: 방장 화면에 새 참가자가 즉시 표시되어야 함

3. **확인 사항**
   - 방장 화면의 "투입된 요원" 숫자가 증가하는지
   - 참가자 아바타가 추가되는지

### 2. WebSocket 연결 상태별 테스트

#### 케이스 A: 연결 상태가 🟢 "연결됨"
- 정상 동작
- 새 참가자 입장 시 즉시 UI 업데이트

#### 케이스 B: 연결 상태가 🔴 "연결 오류"
- WebSocket 연결 실패
- 새 참가자 입장해도 UI 업데이트 안 됨
- **해결**: 방에서 나갔다가 다시 들어가기 (WebSocket 재연결)

#### 케이스 C: 연결 상태가 🟡 "연결 중..."
- 아직 연결 중
- 잠시 기다리면 "연결됨"으로 변경되어야 함
- 계속 "연결 중..."이면 네트워크 문제

## 로그 확인 방법

### 서버 로그 (터미널 1)
```bash
cd server
./gradlew run 2>&1 | grep -E "WebSocket|broadcast|ParticipantJoined|subscribe"
```

**기대 로그**:
```
WebSocket connection established: userId=<user-id>, roomId=<room-id>
User <user-id> subscribed to room <room-id>. Total connections: 1
Broadcasting to room <room-id>: ParticipantJoined to 1 clients
```

### 클라이언트 로그 - 에뮬레이터 (터미널 2)
```bash
adb logcat | grep -E "WebSocket|RoomViewModel|Received.*message|Parsed.*event|Handling"
```

**기대 로그**:
```
Connecting to WebSocket for room: <room-id>
WebSocket connected successfully
Received WebSocket message: {"type":"participant_joined",...}
Parsed event: ParticipantJoined
Handling WebSocket event: ParticipantJoined
```

### 클라이언트 로그 - 삼성 갤럭시 (터미널 3)
```bash
adb -s R3CY50NCDVB logcat | grep -E "WebSocket|RoomViewModel|Received.*message|Parsed.*event"
```

## 일반적인 문제와 해결책

### 문제 1: WebSocket 연결 상태가 계속 "연결 안됨"
**원인**:
- TokenStorage에 토큰이 없음
- 서버가 실행되지 않음

**해결**:
1. 로그아웃 후 다시 로그인
2. 서버가 실행 중인지 확인 (`cd server && ./gradlew run`)

### 문제 2: WebSocket 연결 상태가 "연결 오류"
**원인**:
- 네트워크 문제
- 서버 URL이 잘못됨 (현재: `10.0.2.2:8080`)
- JWT 토큰 만료

**해결**:
1. 에뮬레이터: `10.0.2.2:8080` 사용 확인
2. 실제 기기: 서버 IP 주소로 변경 필요 (예: `192.168.0.x:8080`)
3. 로그아웃 후 다시 로그인

### 문제 3: WebSocket은 "연결됨"이지만 참가자가 추가 안 됨
**원인**:
- 서버에서 이벤트 브로드캐스트가 안 됨
- 클라이언트에서 이벤트를 수신했지만 UI 업데이트 안 됨

**해결**:
1. 서버 로그에서 "Broadcasting to room" 메시지 확인
2. 클라이언트 로그에서 "Parsed event: ParticipantJoined" 확인
3. 클라이언트 로그에서 "Handling WebSocket event: ParticipantJoined" 확인

### 문제 4: 타이밍 문제 (드물게 발생)
**원인**:
- 방장이 WebSocket에 연결되기 전에 참가자가 입장함

**해결**:
- 방장이 QR 코드를 보여주고 몇 초 기다린 후 참가자가 스캔하도록 함
- 또는 방 화면에서 새로고침 버튼 추가 (향후 개선)

## 디버깅 체크리스트

- [ ] 서버가 실행 중인가? (`./gradlew run`)
- [ ] WebSocket 연결 상태가 🟢 "연결됨"인가?
- [ ] 서버 로그에 "WebSocket connection established" 메시지가 있는가?
- [ ] 새 참가자 입장 시 서버 로그에 "Broadcasting to room" 메시지가 있는가?
- [ ] 클라이언트 로그에 "Received WebSocket message" 메시지가 있는가?
- [ ] 클라이언트 로그에 "Handling WebSocket event: ParticipantJoined" 메시지가 있는가?

## 추가 개선 사항 (향후)

1. **자동 재연결**: WebSocket 연결이 끊어지면 자동으로 재연결
2. **새로고침 버튼**: 수동으로 방 정보 갱신
3. **연결 실패 알림**: 연결 오류 시 사용자에게 알림 표시
4. **하트비트**: 주기적으로 핑/퐁 메시지 전송하여 연결 유지

## 테스트 후 보고 양식

```
테스트 환경:
- 방장 기기: [에뮬레이터/실제 기기]
- 참가자 기기: [에뮬레이터/실제 기기]
- 서버: [실행 중/중지]

WebSocket 연결 상태:
- 방장: [연결됨/연결 중/연결 오류/연결 안됨]
- 참가자: [연결됨/연결 중/연결 오류/연결 안됨]

문제 발생 여부:
- [ ] 방장 화면에 새 참가자가 표시되지 않음
- [ ] 참가자 화면에 방 정보가 표시되지 않음
- [ ] 기타: _________________

서버 로그:
[로그 붙여넣기]

클라이언트 로그 (방장):
[로그 붙여넣기]

클라이언트 로그 (참가자):
[로그 붙여넣기]
```
