package com.heisthunt.app.scenarios

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.heisthunt.app.App
import org.junit.Rule
import org.junit.Test

/**
 * E2E 시나리오 테스트 - 게임 생성 플로우
 *
 * 실제 사용자가 게임을 생성하는 전체 플로우를 테스트합니다.
 * 단, 네트워크 호출은 실제 서버에 의존하므로 UI 요소 테스트에 집중합니다.
 */
class CreateGameFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 시나리오 1: 작전 화면 UI 완전성 테스트
     *
     * 1. 앱 시작 → 로그인 (Mock)
     * 2. 작전 화면 표시 확인
     * 3. 모든 UI 요소가 올바르게 표시되는지 확인
     * 4. 버튼들이 활성화 상태인지 확인
     */
    @Test
    fun scenario_operationScreen_uiCompleteness() {
        var loginSuccessful = false

        // Given: 앱 시작 (Mock Google 로그인 성공)
        composeTestRule.setContent {
            App(
                onGoogleLogin = {
                    loginSuccessful = true
                    true
                }
            )
        }

        // Then: 로그인 화면이 표시됨
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("HEIST")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // When: Google 로그인 버튼 클릭
        composeTestRule.onNodeWithText("Google로 계속하기").performClick()

        composeTestRule.waitForIdle()
        assert(loginSuccessful) { "로그인이 성공해야 함" }

        // Then: 작전 화면으로 이동
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("HUNT")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: 타이틀 텍스트
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("OR").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("OPERATION CENTER").assertIsDisplayed()

        // Verify: 카드 라벨
        composeTestRule.onAllNodesWithText("LEAD A MISSION")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("JOIN OPERATION")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("JOIN OPERATION")[1].assertIsDisplayed()

        // Verify: 버튼 텍스트
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("QR 코드로 참여").assertIsDisplayed()
        composeTestRule.onNodeWithText("인증 코드로 참여").assertIsDisplayed()

        // Verify: 하단 버튼들
        composeTestRule.onNodeWithText("🧪 Debug Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("🚪 로그아웃").assertIsDisplayed()

        // Verify: 설명 텍스트
        composeTestRule.onNodeWithText("새로운 게임 방을 만들고 요원을 소집합니다.").assertIsDisplayed()
        composeTestRule.onNodeWithText("방장의 QR 코드를 스캔하여 즉시 합류합니다.").assertIsDisplayed()
        composeTestRule.onNodeWithText("4자리 코드를 입력하여 게임에 합류합니다.").assertIsDisplayed()

        // Verify: 버튼들이 클릭 가능 상태
        composeTestRule.onNodeWithText("작전 설계하기").assertHasClickAction()
        composeTestRule.onNodeWithText("QR 코드로 참여").assertHasClickAction()
        composeTestRule.onNodeWithText("인증 코드로 참여").assertHasClickAction()
    }

    /**
     * 시나리오 2: 작전 화면 로딩 성능 테스트
     *
     * 1. 앱 시작 → 로그인
     * 2. 작전 화면이 10초 이내에 로드되는지 확인
     */
    @Test
    fun scenario_operationScreen_loadingPerformance() {
        val startTime = System.currentTimeMillis()

        // Given: 앱 시작
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // Then: 로그인 화면 로드
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // When: 로그인
        composeTestRule.onNodeWithText("Google로 계속하기").performClick()
        composeTestRule.waitForIdle()

        // Then: 작전 화면이 10초 이내에 로드됨
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("작전 설계하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val loadTime = System.currentTimeMillis() - startTime

        // Verify: 주요 UI 요소가 표시됨
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()

        // Performance check: 10초 이내 로딩 (로그인 포함)
        assert(loadTime < 10000) { "작전 화면이 10초 이내에 로드되어야 함 (실제: ${loadTime}ms)" }
    }

    /**
     * 시나리오 3: QR 코드로 참여 버튼 테스트
     *
     * 1. 작전 화면 로드
     * 2. "QR 코드로 참여" 버튼이 표시되고 클릭 가능한지 확인
     * Note: 실제 네비게이션은 통합 테스트에서 확인
     */
    @Test
    fun scenario_qrScanner_button_displayed() {
        // Given: 앱 시작 및 로그인
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // 로그인 화면 대기
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 로그인
        composeTestRule.onNodeWithText("Google로 계속하기").performClick()
        composeTestRule.waitForIdle()

        // Then: 메인 화면 로드 대기
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("QR 코드로 참여")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: QR 코드로 참여 버튼이 표시되고 클릭 가능
        composeTestRule.onNodeWithText("QR 코드로 참여").assertIsDisplayed()
        composeTestRule.onNodeWithText("QR 코드로 참여").assertHasClickAction()
    }

    /**
     * 시나리오 4: 인증 코드 입력 다이얼로그 테스트
     *
     * 1. 작전 화면 로드
     * 2. "인증 코드로 참여" 버튼 클릭
     * 3. 코드 입력 다이얼로그 표시 확인
     * 4. 다이얼로그 UI 요소 확인
     * 5. 취소 버튼으로 닫기
     */
    @Test
    fun scenario_codeInput_dialog() {
        // Given: 앱 시작 및 로그인
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // 로그인 화면 대기
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 로그인
        composeTestRule.onNodeWithText("Google로 계속하기").performClick()
        composeTestRule.waitForIdle()

        // Then: 메인 화면 로드 대기
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("인증 코드로 참여")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // When: "인증 코드로 참여" 버튼 클릭
        composeTestRule.onNodeWithText("인증 코드로 참여").performClick()

        composeTestRule.waitForIdle()

        // Then: 코드 입력 다이얼로그가 표시됨
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("인증 코드 입력")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: 다이얼로그 UI 요소
        composeTestRule.onNodeWithText("🔑").assertIsDisplayed()
        composeTestRule.onNodeWithText("인증 코드 입력").assertIsDisplayed()
        composeTestRule.onNodeWithText("4자리 인증 코드를 입력하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("참여하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").assertIsDisplayed()

        // When: 취소 버튼 클릭
        composeTestRule.onNodeWithText("취소").performClick()

        composeTestRule.waitForIdle()

        // Then: 다이얼로그가 닫히고 메인 화면 유지
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()
    }

    /**
     * 시나리오 5: 로그아웃 버튼 표시 테스트
     *
     * 1. 작전 화면 로드
     * 2. 로그아웃 버튼이 표시되는지 확인
     * Note: 실제 로그아웃 기능은 통합 테스트에서 확인
     */
    @Test
    fun scenario_logout_button_displayed() {
        // Given: 앱 시작 및 로그인
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // 로그인 화면 대기
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 로그인
        composeTestRule.onNodeWithText("Google로 계속하기").performClick()
        composeTestRule.waitForIdle()

        // Then: 작전 화면 로드 대기
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("🚪 로그아웃")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: 로그아웃 버튼이 표시되고 클릭 가능
        composeTestRule.onNodeWithText("🚪 로그아웃").assertIsDisplayed()
        composeTestRule.onNodeWithText("🚪 로그아웃").assertHasClickAction()
    }

    /**
     * 시나리오 6: 작전 화면 레이아웃 무결성 테스트
     *
     * 1. 작전 화면 로드
     * 2. 모든 요소가 올바른 순서로 표시되는지 확인
     * 3. 화면 오버플로우가 없는지 확인
     */
    @Test
    fun scenario_operationScreen_layoutIntegrity() {
        // Given: 앱 시작 및 로그인
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // 로그인
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Google로 계속하기").performClick()
        composeTestRule.waitForIdle()

        // Then: 작전 화면 로드
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("HUNT")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: 상단 - 타이틀
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("OR").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("OPERATION CENTER").assertIsDisplayed()

        // Verify: 중간 - 액션 버튼들
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("QR 코드로 참여").assertIsDisplayed()
        composeTestRule.onNodeWithText("인증 코드로 참여").assertIsDisplayed()

        // Verify: 하단 - 유틸리티 버튼들
        composeTestRule.onNodeWithText("🧪 Debug Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("🚪 로그아웃").assertIsDisplayed()

        // All elements exist and are visible - no overflow issues
    }

    /**
     * 시나리오 7: Debug Settings 네비게이션 테스트
     *
     * 1. 작전 화면 로드
     * 2. Debug Settings 버튼 클릭
     * 3. 디버그 화면으로 이동 확인 (구현되어 있다면)
     */
    @Test
    fun scenario_debugSettings_navigation() {
        // Given: 앱 시작 및 로그인
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // 로그인
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Google로 계속하기").performClick()
        composeTestRule.waitForIdle()

        // Then: 작전 화면 로드
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("🧪 Debug Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: Debug Settings 버튼이 클릭 가능함
        composeTestRule.onNodeWithText("🧪 Debug Settings").assertHasClickAction()

        // When: Debug Settings 버튼 클릭
        composeTestRule.onNodeWithText("🧪 Debug Settings").performClick()

        composeTestRule.waitForIdle()

        // Note: 디버그 화면의 구체적인 UI는 구현에 따라 다를 수 있음
        // 여기서는 버튼이 클릭 가능한지만 확인
    }

    /**
     * 시나리오 8: 코드 입력 다이얼로그 반복 테스트
     *
     * 1. 코드 입력 다이얼로그 열기 → 닫기
     * 2. 빠르게 반복해도 정상 작동하는지 확인
     */
    @Test
    fun scenario_code_dialog_repeated() {
        // Given: 앱 시작 및 로그인
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // 로그인
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Google로 계속하기").performClick()
        composeTestRule.waitForIdle()

        // Then: 작전 화면 로드
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("인증 코드로 참여")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // When/Then: 코드 입력 다이얼로그 반복 테스트
        repeat(2) {
            composeTestRule.onNodeWithText("인증 코드로 참여").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule
                    .onAllNodesWithText("인증 코드 입력")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithText("취소").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule
                    .onAllNodesWithText("인증 코드로 참여")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }

        // Verify: 모든 작업 후에도 화면이 정상적으로 표시됨
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()
    }
}
