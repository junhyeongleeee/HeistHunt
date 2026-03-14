#!/bin/bash
# ===================================================
# HeistHunt Android 빌드 → Firebase App Distribution 업로드
# ===================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
source "$SCRIPT_DIR/deploy.config"

ANDROID_APP_ID="1:121910262869:android:de0ccb8b9c967bf31368dc"
RELEASE_NOTES="${1:-$(date '+%Y-%m-%d %H:%M') 빌드}"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   HeistHunt Android 배포                 ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# 1. 현재 IP 확인 및 업데이트
echo "📡 [1/4] 네트워크 IP 확인 중..."
CURRENT_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "")
if [ -z "$CURRENT_IP" ]; then
    echo "⚠️  IP 주소를 가져올 수 없습니다. 기존 설정을 유지합니다."
else
    echo "✅ 현재 IP: $CURRENT_IP"
    ANDROID_URL_FILE="$ROOT_DIR/composeApp/src/androidMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.android.kt"
    NETWORK_SEC_FILE="$ROOT_DIR/composeApp/src/androidMain/res/xml/network_security_config.xml"

    # PlatformBaseUrl 업데이트
    cat > "$ANDROID_URL_FILE" << EOF
package com.heisthunt.app.di

actual fun getPlatformBaseUrl(): String {
    val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
        android.os.Build.HARDWARE.contains("goldfish") ||
        android.os.Build.HARDWARE.contains("ranchu") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK")
    return if (isEmulator) {
        "http://10.0.2.2:8080"
    } else {
        "http://$CURRENT_IP:8080"
    }
}
EOF

    # network_security_config.xml에 IP 추가 (중복 없이)
    if ! grep -q "$CURRENT_IP" "$NETWORK_SEC_FILE"; then
        sed -i '' "s|<domain-config cleartextTrafficPermitted=\"true\">|<domain-config cleartextTrafficPermitted=\"true\">\n        <domain includeSubdomains=\"true\">$CURRENT_IP</domain>|" "$NETWORK_SEC_FILE"
        echo "✅ network_security_config.xml에 $CURRENT_IP 추가됨"
    else
        echo "✅ network_security_config.xml에 이미 $CURRENT_IP 존재"
    fi
fi

# 2. 서버 실행 확인
echo ""
echo "🖥️  [2/4] 서버 상태 확인 중..."
if lsof -i :8080 -sTCP:LISTEN -t > /dev/null 2>&1; then
    echo "✅ 서버 실행 중 (포트 8080)"
else
    echo "⚠️  서버가 실행되지 않았습니다. 배포는 계속하지만 앱이 서버에 연결되지 않을 수 있습니다."
fi

# 3. Android APK 빌드
echo ""
echo "🔨 [3/4] Android APK 빌드 중..."
cd "$ROOT_DIR"
./gradlew :composeApp:assembleDebug

APK_PATH="$ROOT_DIR/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK를 찾을 수 없습니다: $APK_PATH"
    exit 1
fi
echo "✅ APK 빌드 완료: $APK_PATH"

# 4. Firebase App Distribution 업로드
echo ""
echo "🚀 [4/4] Firebase App Distribution 업로드 중..."

FIREBASE_CMD="firebase appdistribution:distribute \"$APK_PATH\" \
    --app \"$ANDROID_APP_ID\" \
    --release-notes \"$RELEASE_NOTES\""

if [ -n "$TESTER_EMAILS" ]; then
    FIREBASE_CMD="$FIREBASE_CMD --testers \"$TESTER_EMAILS\""
fi

if [ -n "$TESTER_GROUP" ]; then
    FIREBASE_CMD="$FIREBASE_CMD --groups \"$TESTER_GROUP\""
fi

eval "$FIREBASE_CMD"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║  ✅ Android 배포 완료!                    ║"
echo "║  Firebase Console에서 다운로드 링크 확인   ║"
echo "╚══════════════════════════════════════════╝"
echo ""
