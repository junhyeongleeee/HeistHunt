# Google Maps API 설정 가이드

## 개요
HeistHunt는 실시간 플레이어 위치 추적을 위해 Google Maps SDK를 사용합니다. 이 가이드는 Google Maps API 키를 발급받고 앱에 설정하는 방법을 안내합니다.

## 1단계: Google Cloud 프로젝트 생성

### 1.1 Google Cloud Console 접속
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. Google 계정으로 로그인

### 1.2 새 프로젝트 생성
1. 상단의 프로젝트 선택 드롭다운 클릭
2. "새 프로젝트" 클릭
3. 프로젝트 이름 입력: `HeistHunt` (또는 원하는 이름)
4. "만들기" 클릭
5. 프로젝트가 생성되면 해당 프로젝트 선택

## 2단계: Maps SDK 활성화

### 2.1 API 라이브러리 접속
1. 왼쪽 메뉴에서 "API 및 서비스" → "라이브러리" 선택
2. 또는 직접 접속: https://console.cloud.google.com/apis/library

### 2.2 Maps SDK for Android 활성화
1. 검색창에 "Maps SDK for Android" 입력
2. "Maps SDK for Android" 선택
3. "사용 설정" 클릭
4. 활성화가 완료될 때까지 대기 (몇 초 소요)

## 3단계: API 키 생성

### 3.1 사용자 인증 정보 만들기
1. 왼쪽 메뉴에서 "API 및 서비스" → "사용자 인증 정보" 선택
2. 또는 직접 접속: https://console.cloud.google.com/apis/credentials
3. 상단의 "+ 사용자 인증 정보 만들기" 클릭
4. "API 키" 선택
5. API 키가 생성되면 **복사**해서 안전한 곳에 보관

### 3.2 API 키 제한 (보안 강화) - 권장사항
**중요**: API 키를 공개 저장소에 올리지 마세요!

1. 생성된 API 키 옆의 "편집" 아이콘 클릭
2. **애플리케이션 제한사항** 섹션:
   - "Android 앱" 선택
   - "+ 항목 추가" 클릭
   - 패키지 이름 입력: `com.heisthunt.app`
   - SHA-1 인증서 지문 입력 (아래 참조)
   - "완료" 클릭

3. **API 제한사항** 섹션:
   - "키 제한" 선택
   - "Maps SDK for Android" 체크
   - "저장" 클릭

### 3.3 SHA-1 인증서 지문 얻기

**디버그 키 (개발용):**
```bash
# macOS/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**릴리스 키 (프로덕션용):**
```bash
# 자신의 keystore 경로로 변경
keytool -list -v -keystore /path/to/my-release-key.keystore -alias my-key-alias
```

출력에서 `SHA1:` 로 시작하는 줄을 찾아 복사하세요.
예: `SHA1: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78`

## 4단계: 앱에 API 키 설정

### 방법 1: AndroidManifest.xml에 직접 설정 (간단하지만 비권장)

**파일**: `composeApp/src/androidMain/AndroidManifest.xml`

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_ACTUAL_API_KEY_HERE" />
```

**⚠️ 주의**: Git에 커밋하지 마세요! `.gitignore`에 추가하세요.

### 방법 2: local.properties 사용 (권장)

#### 4.1 local.properties에 API 키 저장

**파일**: `local.properties` (프로젝트 루트)

```properties
# Google Maps API Key
MAPS_API_KEY=YOUR_ACTUAL_API_KEY_HERE
```

**⚠️ 중요**: `local.properties`는 이미 `.gitignore`에 포함되어 있어 Git에 업로드되지 않습니다.

#### 4.2 build.gradle.kts에서 API 키 읽기

**파일**: `composeApp/build.gradle.kts`

```kotlin
import java.util.Properties

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    // ... existing config ...

    defaultConfig {
        // ... existing config ...

        // Add Maps API Key to BuildConfig
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: "YOUR_GOOGLE_MAPS_API_KEY_HERE"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
}
```

#### 4.3 AndroidManifest.xml 업데이트

**파일**: `composeApp/src/androidMain/AndroidManifest.xml`

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

## 5단계: 테스트

### 5.1 앱 실행
1. Android Studio에서 앱 실행
2. 게임 시작
3. 지도가 표시되는지 확인

### 5.2 오류 확인

**지도가 회색 화면으로 표시되는 경우:**
1. Logcat에서 에러 확인:
   ```
   Google Maps Android API: Authorization failure
   ```
2. 원인:
   - API 키가 올바르지 않음
   - SHA-1 인증서가 등록되지 않음
   - Maps SDK가 활성화되지 않음

3. 해결 방법:
   - API 키 재확인
   - SHA-1 재등록
   - Maps SDK 활성화 확인

**"API key not found" 에러:**
1. `local.properties`에 API 키가 있는지 확인
2. Gradle 동기화 실행: `File > Sync Project with Gradle Files`
3. 앱 재빌드: `Build > Rebuild Project`

## 6단계: 비용 관리

### 무료 사용량
Google Maps는 월 $200 크레딧을 제공합니다:
- Dynamic Maps: 28,000 로드/월 무료
- 대부분의 개발 및 소규모 프로젝트는 무료 범위 내

### 비용 초과 방지
1. [Google Cloud Console](https://console.cloud.google.com/billing) → 결제
2. "예산 및 알림" 설정
3. 월 $200 (무료 크레딧) 에서 알림 설정

### 할당량 모니터링
1. [API 대시보드](https://console.cloud.google.com/apis/dashboard) 접속
2. "Maps SDK for Android" 클릭
3. "할당량" 탭에서 사용량 확인

## 보안 권장사항

### ✅ 해야 할 것
- ✅ API 키를 `local.properties`에 저장
- ✅ 애플리케이션 제한 설정 (Android 앱 + SHA-1)
- ✅ API 제한 설정 (Maps SDK만 허용)
- ✅ `.gitignore`에 `local.properties` 포함 확인
- ✅ 프로덕션 빌드 시 별도의 API 키 사용

### ❌ 하지 말아야 할 것
- ❌ API 키를 코드에 하드코딩
- ❌ API 키를 Git에 커밋
- ❌ 제한 없는 API 키 사용
- ❌ 공개 저장소에 API 키 노출
- ❌ 동일한 API 키를 여러 앱에서 공유

## 문제 해결

### 문제: 지도가 로드되지 않음
**확인사항:**
1. 인터넷 연결 확인
2. API 키가 올바른지 확인
3. Maps SDK가 활성화되었는지 확인
4. 위치 권한이 승인되었는지 확인

**Logcat 확인:**
```bash
adb logcat | grep -i "maps\|google"
```

### 문제: "API key is invalid" 에러
**해결:**
1. Google Cloud Console에서 API 키 재생성
2. AndroidManifest.xml 업데이트
3. 앱 언인스톨 후 재설치
4. Gradle 클린 빌드: `./gradlew clean build`

### 문제: "This app has exceeded its request quota"
**해결:**
1. [할당량 확인](https://console.cloud.google.com/apis/api/maps-android-backend.googleapis.com/quotas)
2. 할당량 증가 요청 또는 결제 정보 추가
3. 캐싱 구현으로 API 호출 최소화

## 추가 리소스

### 공식 문서
- [Maps SDK for Android 시작하기](https://developers.google.com/maps/documentation/android-sdk/start)
- [API 키 관리 권장사항](https://developers.google.com/maps/api-security-best-practices)
- [Maps SDK 가격 정보](https://developers.google.com/maps/billing/gmp-billing)

### 샘플 코드
- [Google Maps Compose Samples](https://github.com/googlemaps/android-maps-compose)
- [Maps SDK Samples](https://github.com/googlemaps/android-samples)

### 커뮤니티
- [Stack Overflow - google-maps-android](https://stackoverflow.com/questions/tagged/google-maps-android)
- [Google Maps Platform Issue Tracker](https://issuetracker.google.com/issues?q=componentid:188838)

## 체크리스트

완료된 항목을 체크하세요:

- [ ] Google Cloud 프로젝트 생성
- [ ] Maps SDK for Android 활성화
- [ ] API 키 생성 및 복사
- [ ] API 키 제한 설정 (Android 앱 + SHA-1)
- [ ] `local.properties`에 API 키 저장
- [ ] `build.gradle.kts`에 API 키 읽기 코드 추가
- [ ] `AndroidManifest.xml` 업데이트
- [ ] 앱 실행 및 지도 표시 확인
- [ ] SHA-1 디버그 키 등록
- [ ] SHA-1 릴리스 키 등록 (프로덕션용)
- [ ] 비용 알림 설정
- [ ] `.gitignore`에 API 키 포함 확인

## 완료!

Google Maps가 성공적으로 설정되었습니다! 이제 HeistHunt에서 실시간 플레이어 위치를 지도에서 볼 수 있습니다.

문제가 발생하면 위의 "문제 해결" 섹션을 참조하거나, [이슈를 등록](https://github.com/your-repo/issues)해주세요.
