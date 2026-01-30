# HeistHunt - 경찰과 도둑

Kotlin Multiplatform (KMP) 기반의 경찰과 도둑 게임 앱

## Tech Stack

- **Kotlin Multiplatform (KMP)** - 크로스 플랫폼 개발
- **Compose Multiplatform** - 공유 UI
- **Ktor** - 네트워크 통신
- **Kotlinx Serialization** - JSON 직렬화
- **Figma** - UI/UX 디자인

## Supported Platforms

- Android
- iOS
- Desktop (JVM)

## Project Structure

```
HeistHunt/
├── composeApp/           # Compose Multiplatform 앱
│   └── src/
│       ├── commonMain/   # 공통 코드
│       ├── androidMain/  # Android 전용
│       ├── iosMain/      # iOS 전용
│       └── desktopMain/  # Desktop 전용
├── iosApp/               # iOS 앱 진입점
└── gradle/               # Gradle 설정
```

## Getting Started

### Prerequisites

- JDK 17+
- Android Studio (for Android development)
- Xcode (for iOS development)

### Build & Run

**Android:**
```bash
./gradlew :composeApp:assembleDebug
```

**Desktop:**
```bash
./gradlew :composeApp:run
```

**iOS:**
Open `iosApp/iosApp.xcodeproj` in Xcode and run.
