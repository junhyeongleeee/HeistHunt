#!/bin/bash
# ===================================================
# HeistHunt iOS 빌드 → Firebase App Distribution 업로드
# ===================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
source "$SCRIPT_DIR/deploy.config"

IOS_APP_ID="1:121910262869:ios:8767d0265b3fb4951368dc"
RELEASE_NOTES="${1:-$(date '+%Y-%m-%d %H:%M') 빌드}"
ARCHIVE_PATH="/tmp/HeistHunt.xcarchive"
EXPORT_PATH="/tmp/HeistHunt-ipa"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   HeistHunt iOS 배포                     ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# Apple Team ID 확인
if [ -z "$APPLE_TEAM_ID" ]; then
    echo "❌ deploy.config에 APPLE_TEAM_ID를 설정해주세요."
    echo "   Xcode > Signing & Capabilities > Team 옆 괄호 안 ID"
    exit 1
fi

# 1. 현재 IP 확인 및 업데이트
echo "📡 [1/5] 네트워크 IP 확인 중..."
CURRENT_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "")
if [ -z "$CURRENT_IP" ]; then
    echo "⚠️  IP 주소를 가져올 수 없습니다. 기존 설정을 유지합니다."
else
    echo "✅ 현재 IP: $CURRENT_IP"
    IOS_URL_FILE="$ROOT_DIR/composeApp/src/iosMain/kotlin/com/heisthunt/app/di/PlatformBaseUrl.ios.kt"
    cat > "$IOS_URL_FILE" << EOF
package com.heisthunt.app.di

actual fun getPlatformBaseUrl(): String {
    return "http://$CURRENT_IP:8080"
}
EOF
    echo "✅ iOS API URL 업데이트: http://$CURRENT_IP:8080"
fi

# 2. iOS 프레임워크 빌드
echo ""
echo "🔨 [2/5] iOS 프레임워크 빌드 중..."
cd "$ROOT_DIR"
./gradlew :composeApp:linkDebugFrameworkIosArm64
echo "✅ iOS 프레임워크 빌드 완료"

# 3. iOS 아카이브 생성
echo ""
echo "📦 [3/5] iOS 아카이브 생성 중..."
cd "$ROOT_DIR/iosApp"

# DEVELOPMENT_TEAM을 project.yml에 임시 설정
sed -i '' "s/DEVELOPMENT_TEAM: \"\"/DEVELOPMENT_TEAM: \"$APPLE_TEAM_ID\"/" project.yml
xcodegen generate --quiet 2>/dev/null || xcodegen generate

xcodebuild archive \
    -workspace iosApp.xcworkspace \
    -scheme "$IOS_SCHEME" \
    -configuration "$IOS_CONFIGURATION" \
    -destination "generic/platform=iOS" \
    -archivePath "$ARCHIVE_PATH" \
    DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
    CODE_SIGN_STYLE="Automatic" \
    -allowProvisioningUpdates \
    -quiet

# DEVELOPMENT_TEAM 다시 비워두기 (git 변경 최소화)
sed -i '' "s/DEVELOPMENT_TEAM: \"$APPLE_TEAM_ID\"/DEVELOPMENT_TEAM: \"\"/" project.yml
xcodegen generate --quiet 2>/dev/null || xcodegen generate

echo "✅ 아카이브 생성 완료: $ARCHIVE_PATH"

# 4. IPA 내보내기
echo ""
echo "📤 [4/5] IPA 내보내기 중..."
rm -rf "$EXPORT_PATH"

# ExportOptions에 TEAM_ID 주입
EXPORT_PLIST="/tmp/HeistHunt-ExportOptions.plist"
cat > "$EXPORT_PLIST" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>development</string>
    <key>teamID</key>
    <string>$APPLE_TEAM_ID</string>
    <key>signingStyle</key>
    <string>automatic</string>
    <key>compileBitcode</key>
    <false/>
    <key>stripSwiftSymbols</key>
    <true/>
    <key>thinning</key>
    <string>&lt;none&gt;</string>
</dict>
</plist>
EOF

xcodebuild -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportOptionsPlist "$EXPORT_PLIST" \
    -exportPath "$EXPORT_PATH" \
    -allowProvisioningUpdates \
    -quiet

IPA_PATH=$(find "$EXPORT_PATH" -name "*.ipa" | head -1)
if [ -z "$IPA_PATH" ]; then
    echo "❌ IPA를 찾을 수 없습니다."
    exit 1
fi
echo "✅ IPA 생성 완료: $IPA_PATH"

# 5. Firebase App Distribution 업로드
echo ""
echo "🚀 [5/5] Firebase App Distribution 업로드 중..."

FIREBASE_CMD="firebase appdistribution:distribute \"$IPA_PATH\" \
    --app \"$IOS_APP_ID\" \
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
echo "║  ✅ iOS 배포 완료!                        ║"
echo "║  Firebase Console에서 다운로드 링크 확인   ║"
echo "╚══════════════════════════════════════════╝"
echo ""
