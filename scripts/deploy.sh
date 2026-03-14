#!/bin/bash
# ===================================================
# HeistHunt Firebase App Distribution 배포
# 사용법:
#   ./scripts/deploy.sh           → Android + iOS 모두
#   ./scripts/deploy.sh android   → Android만
#   ./scripts/deploy.sh ios       → iOS만
#   ./scripts/deploy.sh android "릴리즈 노트"
# ===================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET="${1:-both}"
RELEASE_NOTES="${2:-$(date '+%Y-%m-%d %H:%M') 빌드}"

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║       HeistHunt Firebase 배포            ║"
echo "╚══════════════════════════════════════════╝"
echo "  대상: $TARGET"
echo "  릴리즈 노트: $RELEASE_NOTES"
echo ""

case "$TARGET" in
    android)
        bash "$SCRIPT_DIR/deploy-android.sh" "$RELEASE_NOTES"
        ;;
    ios)
        bash "$SCRIPT_DIR/deploy-ios.sh" "$RELEASE_NOTES"
        ;;
    both)
        bash "$SCRIPT_DIR/deploy-android.sh" "$RELEASE_NOTES"
        bash "$SCRIPT_DIR/deploy-ios.sh" "$RELEASE_NOTES"
        echo ""
        echo "╔══════════════════════════════════════════╗"
        echo "║  🎉 Android + iOS 배포 모두 완료!         ║"
        echo "╚══════════════════════════════════════════╝"
        ;;
    *)
        echo "❌ 알 수 없는 대상: $TARGET"
        echo "   사용법: ./scripts/deploy.sh [android|ios|both] [릴리즈 노트]"
        exit 1
        ;;
esac
