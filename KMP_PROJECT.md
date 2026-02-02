# KMP (Kotlin Multiplatform) Project Structure

Compose Multiplatform 기반 KMP 프로젝트의 기본 구조 및 설정 파일 레퍼런스

## Project Structure

```
HeistHunt/
├── composeApp/                              # Compose Multiplatform 앱 모듈
│   ├── build.gradle.kts                     # 앱 모듈 빌드 설정
│   └── src/
│       ├── commonMain/                      # 공통 코드 (모든 플랫폼 공유)
│       │   └── kotlin/com/heisthunt/app/
│       │       └── App.kt                   # 메인 Composable
│       ├── androidMain/                     # Android 전용 코드
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/heisthunt/app/
│       │   │   └── MainActivity.kt
│       │   └── res/values/strings.xml
│       ├── iosMain/                         # iOS 전용 코드
│       │   └── kotlin/com/heisthunt/app/
│       │       └── MainViewController.kt
│       └── desktopMain/                     # Desktop 전용 코드
│           └── kotlin/com/heisthunt/app/
│               └── Main.kt
├── iosApp/                                  # iOS 앱 진입점 (Swift)
│   ├── project.yml                          # XcodeGen 프로젝트 설정
│   └── iosApp/
│       ├── iOSApp.swift
│       ├── ContentView.swift
│       └── Info.plist                       # iOS 앱 설정 (ProMotion 지원 필수)
├── gradle/
│   ├── libs.versions.toml                   # 버전 카탈로그
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts                         # 루트 빌드 설정
├── settings.gradle.kts                      # 프로젝트 설정
├── gradle.properties                        # Gradle 속성
└── gradlew                                  # Gradle Wrapper 스크립트
```

---

## Configuration Files

### 1. settings.gradle.kts
```kotlin
rootProject.name = "HeistHunt"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
```

### 2. build.gradle.kts (Root)
```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
```

### 3. gradle.properties
```properties
# Gradle
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options\="-Xmx2048M"
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.parallel=true

# Kotlin
kotlin.code.style=official

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true

# Compose
org.jetbrains.compose.experimental.jscanvas.enabled=true
org.jetbrains.compose.experimental.macos.enabled=true
```

### 4. gradle/libs.versions.toml
```toml
[versions]
agp = "8.5.2"
android-compileSdk = "34"
android-minSdk = "24"
android-targetSdk = "34"
androidx-activityCompose = "1.9.3"
androidx-lifecycle = "2.8.4"
compose-multiplatform = "1.7.3"
kotlin = "2.1.0"
kotlinx-coroutines = "1.9.0"
kotlinx-serialization = "1.7.3"
ktor = "3.0.3"

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activityCompose" }
androidx-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
androidx-lifecycle-runtime-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinxSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 5. gradle/wrapper/gradle-wrapper.properties
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## composeApp Module

### composeApp/build.gradle.kts
```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.heisthunt.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.heisthunt.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.heisthunt.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "HeistHunt"
            packageVersion = "1.0.0"
        }
    }
}
```

---

## Source Files

### commonMain/kotlin/com/heisthunt/app/App.kt
```kotlin
package com.heisthunt.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HeistHunt - 경찰과 도둑",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
```

### androidMain/kotlin/com/heisthunt/app/MainActivity.kt
```kotlin
package com.heisthunt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
```

### androidMain/AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|mnc|colorMode|density|fontScale|fontWeightAdjustment|keyboard|layoutDirection|locale|mcc|navigation|smallestScreenSize|touchscreen|uiMode">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### androidMain/res/values/strings.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">HeistHunt</string>
</resources>
```

### iosMain/kotlin/com/heisthunt/app/MainViewController.kt
```kotlin
package com.heisthunt.app

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

### desktopMain/kotlin/com/heisthunt/app/Main.kt
```kotlin
package com.heisthunt.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HeistHunt - 경찰과 도둑"
    ) {
        App()
    }
}
```

---

## iOS App (Swift)

### iosApp/iosApp/iOSApp.swift
```swift
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### iosApp/iosApp/ContentView.swift
```swift
import SwiftUI
import ComposeApp

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

---

## iOS Project Configuration (Xcode)

> **Note:** iOS 프로젝트는 `xcodegen`을 사용하여 Xcode 프로젝트를 생성합니다.
> 설치: `brew install xcodegen`

### iosApp/project.yml (XcodeGen)
```yaml
name: iosApp
options:
  bundleIdPrefix: com.heisthunt
  deploymentTarget:
    iOS: "15.0"

settings:
  base:
    DEVELOPMENT_TEAM: ""

targets:
  iosApp:
    type: application
    platform: iOS
    sources:
      - path: iosApp
        type: group
    settings:
      base:
        INFOPLIST_FILE: iosApp/Info.plist
        PRODUCT_BUNDLE_IDENTIFIER: com.heisthunt.app
        PRODUCT_NAME: HeistHunt
        FRAMEWORK_SEARCH_PATHS:
          - "$(SRCROOT)/../composeApp/build/bin/iosSimulatorArm64/debugFramework"
          - "$(SRCROOT)/../composeApp/build/bin/iosArm64/releaseFramework"
        OTHER_LDFLAGS:
          - "-framework"
          - "ComposeApp"
    dependencies: []
```

### iosApp/iosApp/Info.plist
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>$(DEVELOPMENT_LANGUAGE)</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>$(PRODUCT_NAME)</string>
    <key>CFBundlePackageType</key>
    <string>$(PRODUCT_BUNDLE_PACKAGE_TYPE)</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSRequiresIPhoneOS</key>
    <true/>
    <key>UIApplicationSceneManifest</key>
    <dict>
        <key>UIApplicationSupportsMultipleScenes</key>
        <false/>
    </dict>
    <key>UILaunchScreen</key>
    <dict/>
    <key>UIRequiredDeviceCapabilities</key>
    <array>
        <string>armv7</string>
    </array>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationLandscapeLeft</string>
        <string>UIInterfaceOrientationLandscapeRight</string>
    </array>
    <!-- IMPORTANT: Required for Compose Multiplatform ProMotion display support -->
    <key>CADisableMinimumFrameDurationOnPhone</key>
    <true/>
</dict>
</plist>
```

> **Important:** `CADisableMinimumFrameDurationOnPhone` 키는 Compose Multiplatform이 iOS에서 ProMotion 디스플레이 (120Hz)를 지원하기 위해 필수입니다. 이 키가 없으면 앱이 크래시됩니다.

---

## Build Commands

### Android
```bash
# Android Debug APK 빌드
./gradlew :composeApp:assembleDebug

# Android Release APK 빌드
./gradlew :composeApp:assembleRelease

# 에뮬레이터에 설치 및 실행
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n com.heisthunt.app/.MainActivity
```

### Desktop
```bash
# Desktop 앱 실행
./gradlew :composeApp:run
```

### iOS
```bash
# 1. iOS Framework 빌드
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# 2. Xcode 프로젝트 생성 (xcodegen 필요)
cd iosApp && xcodegen generate

# 3. iOS 앱 빌드
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  -configuration Debug build

# 4. 시뮬레이터에 설치 및 실행
xcrun simctl install booted ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator/HeistHunt.app
xcrun simctl launch booted com.heisthunt.app
```

### Clean
```bash
# 전체 클린
./gradlew clean
```

---

## Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| Language | Kotlin | 2.1.0 |
| UI Framework | Compose Multiplatform | 1.7.3 |
| Build Tool | Gradle | 8.9 |
| Android Gradle Plugin | AGP | 8.5.2 |
| Networking | Ktor | 3.0.3 |
| Serialization | Kotlinx Serialization | 1.7.3 |
| Async | Kotlinx Coroutines | 1.9.0 |
| Min Android SDK | 24 | (Android 7.0) |
| Target Android SDK | 34 | (Android 14) |
| iOS Deployment Target | 15.0 | (iOS 15.0+) |
| JVM Target | 17 | |
| iOS Project Generator | XcodeGen | 2.44+ |

---

## Supported Platforms

- **Android** - Native Android app
- **iOS** - Native iOS app via Kotlin/Native (requires Xcode + XcodeGen)
- **Desktop** - JVM-based desktop app (macOS, Windows, Linux)

---

## Troubleshooting

### iOS 앱 크래시: PlistSanityCheck 오류
Compose Multiplatform은 ProMotion 디스플레이 지원을 위해 `Info.plist`에 `CADisableMinimumFrameDurationOnPhone` 키가 필수입니다.

```xml
<key>CADisableMinimumFrameDurationOnPhone</key>
<true/>
```

### iOS 빌드 시 Framework를 찾을 수 없음
Gradle로 먼저 iOS Framework를 빌드해야 합니다:
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

### Android 빌드 시 아이콘 리소스 오류
`AndroidManifest.xml`에서 `android:icon`과 `android:roundIcon` 속성을 제거하거나, 실제 아이콘 리소스를 추가하세요.
