# QR 코드 스캔 기능 구현 계획

## 목표
사용자가 카메라로 QR 코드를 스캔하여 방에 자동으로 참여할 수 있게 함

## 기술 스택 선택

### Option 1: ML Kit (Google) ✅ **추천**
- **장점:**
  - Google에서 공식 지원
  - 빠르고 정확한 QR 인식
  - CameraX와 쉽게 통합
  - 무료
- **단점:**
  - Android만 지원 (iOS는 별도 구현 필요)

### Option 2: ZXing
- **장점:**
  - 오픈소스
  - 성숙한 라이브러리
- **단점:**
  - ML Kit보다 느림
  - KMP 통합이 복잡

### **결정: ML Kit (Android) + AVFoundation (iOS)**

## 아키텍처

### Expect/Actual 패턴 사용

```
commonMain/
  └── QRScanner.kt (expect interface)

androidMain/
  └── QRScanner.android.kt (actual - ML Kit + CameraX)

iosMain/
  └── QRScanner.ios.kt (actual - AVFoundation)
```

## 구현 단계

### Phase 1: Android QR 스캔 (우선)

#### 1.1 의존성 추가
```kotlin
// composeApp/build.gradle.kts
androidMain.dependencies {
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
}
```

#### 1.2 AndroidManifest 권한 추가
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
```

#### 1.3 공통 인터페이스 정의
```kotlin
// commonMain/kotlin/.../QRScanner.kt
expect class QRScanner {
    fun startScanning(onQRCodeDetected: (String) -> Unit)
    fun stopScanning()
}

@Composable
expect fun QRScannerView(
    onQRCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

#### 1.4 Android 구현
```kotlin
// androidMain/kotlin/.../QRScanner.android.kt
actual class QRScanner {
    private var imageAnalyzer: ImageAnalysis? = null

    actual fun startScanning(onQRCodeDetected: (String) -> Unit) {
        // CameraX + ML Kit 구현
    }

    actual fun stopScanning() {
        imageAnalyzer?.clearAnalyzer()
    }
}

@Composable
actual fun QRScannerView(
    onQRCodeDetected: (String) -> Unit,
    modifier: Modifier
) {
    // CameraX PreviewView를 AndroidView로 래핑
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                // CameraX 설정
            }
        },
        modifier = modifier
    )
}
```

#### 1.5 권한 요청 처리
```kotlin
// Android에서 런타임 권한 요청
@Composable
fun RequestCameraPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onPermissionGranted()
        else onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}
```

### Phase 2: iOS QR 스캔 (나중)

#### 2.1 iOS 구현 스텁
```kotlin
// iosMain/kotlin/.../QRScanner.ios.kt
actual class QRScanner {
    actual fun startScanning(onQRCodeDetected: (String) -> Unit) {
        // TODO: AVFoundation 구현
        println("iOS QR scanning not implemented yet")
    }

    actual fun stopScanning() {}
}

@Composable
actual fun QRScannerView(
    onQRCodeDetected: (String) -> Unit,
    modifier: Modifier
) {
    // 임시 플레이스홀더
    Box(modifier = modifier) {
        Text("iOS Camera - Coming Soon")
    }
}
```

### Phase 3: UI 통합

#### 3.1 OperationScreen에서 QRScannerScreen 업데이트
```kotlin
OperationView.QR_SCANNER -> QRScannerScreen(
    onCancel = { currentView = OperationView.MAIN },
    onQRScanned = { roomCode ->
        // 방 참여 API 호출
        roomViewModel.joinRoom(roomCode)
        currentView = OperationView.WAITING
    }
)
```

#### 3.2 QRScannerScreen 리팩토링
```kotlin
@Composable
private fun QRScannerScreen(
    onCancel: () -> Unit,
    onQRScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var hasPermission by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasPermission) {
            // 실제 카메라 뷰
            QRScannerView(
                onQRCodeDetected = { code ->
                    onQRScanned(code)
                },
                modifier = Modifier.fillMaxSize()
            )

            // 오버레이 UI (스캔 가이드, 취소 버튼)
            ScannerOverlay(onCancel)
        } else {
            // 권한 요청
            RequestCameraPermission(
                onPermissionGranted = { hasPermission = true },
                onPermissionDenied = { onCancel() }
            )
        }
    }
}
```

### Phase 4: 방 참여 로직

#### 4.1 RoomViewModel에 joinRoom 메서드 이미 존재 ✅
```kotlin
fun joinRoom(code: String, password: String? = null) {
    viewModelScope.launch {
        _detailState.value = _detailState.value.copy(isLoading = true, error = null)

        roomRepository.joinRoom(code, password)
            .onSuccess { room ->
                _detailState.value = RoomDetailUiState(
                    isLoading = false,
                    room = room,
                    shouldNavigateToWaiting = true  // 자동 네비게이션
                )
            }
            .onFailure { exception ->
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    error = exception.message
                )
            }
    }
}
```

#### 4.2 OperationScreen 플로우
```kotlin
1. QR 스캔 → roomCode 획득
2. roomViewModel.joinRoom(roomCode) 호출
3. shouldNavigateToWaiting = true
4. LaunchedEffect가 감지
5. currentView = WAITING
6. RoomWaitingScreen 표시 (같은 방, 다른 사람 목록 포함)
```

## 파일 구조

```
composeApp/src/
├── commonMain/kotlin/com/heisthunt/app/
│   └── camera/
│       └── QRScanner.kt (expect)
│
├── androidMain/kotlin/com/heisthunt/app/
│   ├── camera/
│   │   └── QRScanner.android.kt (actual)
│   └── permission/
│       └── CameraPermission.kt
│
├── iosMain/kotlin/com/heisthunt/app/
│   └── camera/
│       └── QRScanner.ios.kt (actual - stub)
│
└── commonMain/kotlin/com/heisthunt/app/ui/game/
    └── OperationScreen.kt (수정)
```

## 구현 순서

1. ✅ **의존성 추가** (CameraX, ML Kit)
2. ✅ **권한 추가** (AndroidManifest)
3. ✅ **Expect 인터페이스 정의**
4. ✅ **Android Actual 구현** (CameraX + ML Kit)
5. ✅ **권한 요청 Composable**
6. ✅ **QRScannerScreen 리팩토링**
7. ✅ **OperationScreen 통합**
8. ✅ **테스트** (실제 QR 코드로)
9. ⏳ **iOS 구현** (나중에)

## 테스트 시나리오

### 시나리오 1: 정상 플로우
```
1. 앱 실행 → 로그인
2. Operation Center
3. "QR 코드로 참여" 버튼 클릭
4. 카메라 권한 요청 → 허용
5. 카메라 화면 표시
6. 다른 기기의 QR 코드를 카메라로 스캔
7. QR 코드 인식 (예: "ABCD1234")
8. 자동으로 POST /api/rooms/join?code=ABCD1234 호출
9. 성공 → RoomWaitingScreen으로 이동
10. 방장과 내가 함께 목록에 표시됨
```

### 시나리오 2: 권한 거부
```
1. "QR 코드로 참여" 클릭
2. 카메라 권한 요청 → 거부
3. Operation Center로 돌아감
4. (옵션) 권한 필요 안내 다이얼로그
```

### 시나리오 3: 잘못된 QR 코드
```
1. QR 스캔
2. 방 코드가 아닌 다른 QR 코드 (예: URL)
3. API 호출 → 404 Not Found
4. 에러 다이얼로그 표시
5. 다시 스캔 가능
```

### 시나리오 4: 방이 가득 참
```
1. QR 스캔
2. API 호출 → 400 Bad Request (Room is full)
3. 에러 다이얼로그: "방이 가득 찼습니다"
4. Operation Center로 돌아감
```

## 예상 이슈 및 해결책

### 이슈 1: QR 코드가 계속 인식됨
**문제:** 한 번 스캔 후에도 계속 onQRCodeDetected 호출

**해결책:**
```kotlin
var scanned by remember { mutableStateOf(false) }

QRScannerView(
    onQRCodeDetected = { code ->
        if (!scanned) {
            scanned = true
            onQRScanned(code)
        }
    }
)
```

### 이슈 2: 카메라 릴리스 안 됨
**문제:** QRScannerScreen을 벗어나도 카메라가 계속 실행

**해결책:**
```kotlin
DisposableEffect(Unit) {
    onDispose {
        qrScanner.stopScanning()
    }
}
```

### 이슈 3: 어두운 환경에서 인식 불가
**문제:** 조명이 부족한 곳에서 QR 코드 인식 실패

**해결책:**
- 플래시 토글 버튼 추가
- "조명이 부족합니다" 안내 표시

### 이슈 4: iOS 미구현
**문제:** 현재 iOS는 스텁만 있음

**해결책:**
- Phase 1에서는 Android만 지원
- iOS는 Phase 2에서 AVFoundation으로 구현
- 또는 "iOS에서는 수동으로 코드 입력" 옵션 제공

## 성공 기준

- [x] Android에서 QR 코드 스캔 가능
- [x] 스캔한 코드로 방 참여 API 호출
- [x] 성공 시 RoomWaitingScreen으로 자동 이동
- [x] 실제 방 데이터 표시 (참가자 목록)
- [x] 권한 거부 시 적절한 처리
- [ ] iOS 지원 (나중)

## 다음 단계

QR 스캔 완료 후:
1. WebSocket 연결로 실시간 참가자 업데이트
2. 준비 버튼 구현
3. 게임 시작 기능
