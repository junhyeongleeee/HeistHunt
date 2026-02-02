# Google Maps 빠른 시작 가이드

HeistHunt에 Google Maps를 설정하는 가장 빠른 방법입니다.

## 1단계: API 키 발급 (5분)

### Google Cloud Console 접속
1. https://console.cloud.google.com/ 접속
2. 새 프로젝트 생성: `HeistHunt`

### Maps SDK 활성화
1. https://console.cloud.google.com/apis/library
2. "Maps SDK for Android" 검색
3. "사용 설정" 클릭

### API 키 생성
1. https://console.cloud.google.com/apis/credentials
2. "사용자 인증 정보 만들기" → "API 키" 선택
3. **API 키 복사** (예: `AIzaSyD...`)

## 2단계: API 키 설정 (1분)

### AndroidManifest.xml 수정
**파일**: `composeApp/src/androidMain/AndroidManifest.xml`

**찾기**:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
```

**교체**:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSyD..." />  <!-- 실제 API 키 입력 -->
```

## 3단계: 실행 (1분)

```bash
# Gradle 동기화
./gradlew --refresh-dependencies

# 앱 실행
./gradlew installDebug
```

## 확인사항

### ✅ 성공
- 지도가 정상적으로 표시됨
- 내 위치 마커가 보임
- 안전 반경 원이 표시됨

### ❌ 실패 (지도가 회색)

**Logcat 확인**:
```bash
adb logcat | grep -i "google\|maps"
```

**에러**: "Authorization failure"
- API 키가 올바른지 확인
- Maps SDK가 활성화되었는지 확인

**에러**: "API key not found"
- AndroidManifest.xml 저장 확인
- Gradle 동기화 재실행

## 다음 단계

1. **API 키 보안 설정** (10분)
   - SHA-1 인증서 추가
   - 애플리케이션 제한 설정
   - 자세한 내용: `GOOGLE_MAPS_SETUP.md`

2. **체포 메커니즘 구현** (추천)
   - 플레이어 간 거리 계산
   - 체포 버튼 활성화
   - 자세한 내용: `NEXT_STEPS.md`

## 문제 발생 시

### 공식 문서
- [GOOGLE_MAPS_SETUP.md](./GOOGLE_MAPS_SETUP.md) - 상세 설정 가이드
- [GOOGLE_MAPS_IMPLEMENTATION.md](./GOOGLE_MAPS_IMPLEMENTATION.md) - 구현 상세

### 커뮤니티
- [Stack Overflow](https://stackoverflow.com/questions/tagged/google-maps-android)
- [GitHub Issues](https://github.com/googlemaps/android-maps-compose/issues)

## 전체 설정 시간
- ⚡ 빠른 설정: **7분**
- 🔒 보안 설정 포함: **17분**
