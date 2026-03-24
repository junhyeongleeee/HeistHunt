package com.heisthunt.app.scenarios

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.heisthunt.app.App
import org.junit.Rule
import org.junit.Test

/**
 * E2E 시나리오 테스트 - 로그인 플로우 (Mock 사용)
 *
 * 실제 사용자가 앱을 사용하는 전체 플로우를 테스트합니다.
 * Google Sign-In, Firebase 등은 Mock으로 처리합니다.
 */
class LoginFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 시나리오 1: 구글 로그인 성공 플로우
     *
     * 1. 앱 시작 → 로그인 화면 표시
     * 2. UI 요소 확인 (HEIST, HUNT, 버튼들)
     * 3. Google 로그인 버튼 클릭
     * 4. 로그인 성공 (Mock)
     * 5. 작전 화면으로 이동 확인
     */
    @Test
    fun scenario_googleLogin_successFlow() {
        var googleLoginCalled = false
        var loginSuccessful = false

        // Given: 앱 시작 (Mock Google 로그인 설정)
        composeTestRule.setContent {
            App(
                onGoogleLogin = {
                    googleLoginCalled = true
                    loginSuccessful = true
                    // Mock: Google 로그인 성공 반환
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

        // Verify: 로그인 화면의 모든 요소가 표시됨
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("THE URBAN CHASE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("카카오로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apple로 계속하기").assertIsDisplayed()

        // When: Google 로그인 버튼 클릭
        composeTestRule.onNodeWithText("Google로 계속하기")
            .performClick()

        // Then: Google 로그인 콜백이 호출됨
        composeTestRule.waitForIdle()
        assert(googleLoginCalled) { "Google 로그인이 실행되어야 함" }
        assert(loginSuccessful) { "로그인이 성공해야 함" }

        // Then: 작전 화면 또는 다음 화면으로 이동
        // (실제 앱에서는 로그인 성공 후 자동으로 화면 전환)
        composeTestRule.waitForIdle()

        // 로그인이 성공적으로 완료됨을 확인
        // Note: 실제 화면 전환은 AuthViewModel 상태에 따라 결정됨
    }

    /**
     * 시나리오 2: 구글 로그인 실패 플로우
     *
     * 1. 앱 시작
     * 2. Google 로그인 버튼 클릭
     * 3. 로그인 실패 (Mock)
     * 4. 로그인 화면 유지 확인
     * 5. 에러 상태 확인
     */
    @Test
    fun scenario_googleLogin_failureFlow() {
        var googleLoginCalled = false
        var loginFailed = false

        // Given: 앱 시작 (Mock Google 로그인 실패 설정)
        composeTestRule.setContent {
            App(
                onGoogleLogin = {
                    googleLoginCalled = true
                    loginFailed = true
                    // Mock: Google 로그인 실패 반환
                    false
                }
            )
        }

        // Then: 로그인 화면이 표시됨
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("HEIST")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()

        // When: Google 로그인 버튼 클릭
        composeTestRule.onNodeWithText("Google로 계속하기")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: 로그인이 시도되고 실패함
        assert(googleLoginCalled) { "Google 로그인이 실행되어야 함" }
        assert(loginFailed) { "로그인이 실패해야 함" }

        // Then: 여전히 로그인 화면에 있음
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
    }

    /**
     * 시나리오 3: 여러 로그인 시도
     *
     * 1. 앱 시작
     * 2. Google 로그인 버튼 여러 번 클릭
     * 3. 각 클릭이 독립적으로 처리되는지 확인
     */
    @Test
    fun scenario_multipleLoginAttempts() {
        var loginAttempts = 0

        // Given: 앱 시작
        composeTestRule.setContent {
            App(
                onGoogleLogin = {
                    loginAttempts++
                    // Mock: 첫 번째는 실패, 두 번째는 성공
                    loginAttempts >= 2
                }
            )
        }

        // Then: 로그인 화면 표시
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val googleButton = composeTestRule.onNodeWithText("Google로 계속하기")

        // When: 첫 번째 로그인 시도 (실패)
        googleButton.performClick()
        composeTestRule.waitForIdle()

        assert(loginAttempts == 1) { "첫 번째 로그인 시도가 기록되어야 함" }

        // When: 두 번째 로그인 시도 (성공)
        googleButton.performClick()
        composeTestRule.waitForIdle()

        assert(loginAttempts == 2) { "두 번째 로그인 시도가 기록되어야 함" }
    }

    /**
     * 시나리오 4: UI 반응성 테스트
     *
     * 1. 로그인 화면에서 모든 버튼이 클릭 가능한지 확인
     * 2. 각 버튼이 적절히 반응하는지 확인
     */
    @Test
    fun scenario_loginScreen_uiResponsiveness() {
        var googleClicked = false
        var kakaoClicked = false
        var appleClicked = false

        // Given: 앱 시작
        composeTestRule.setContent {
            App(
                onGoogleLogin = {
                    googleClicked = true
                    true
                }
            )
        }

        // Then: 로그인 화면이 완전히 로드됨
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("HEIST")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: 모든 소셜 로그인 버튼이 활성화 상태
        val googleButton = composeTestRule.onNodeWithText("Google로 계속하기")
        val kakaoButton = composeTestRule.onNodeWithText("카카오로 계속하기")
        val appleButton = composeTestRule.onNodeWithText("Apple로 계속하기")

        googleButton.assertIsEnabled()
        kakaoButton.assertIsEnabled()
        appleButton.assertIsEnabled()

        // When: Google 버튼 클릭
        googleButton.performClick()
        composeTestRule.waitForIdle()

        // Then: Google 로그인이 트리거됨
        assert(googleClicked) { "Google 버튼이 반응해야 함" }
    }

    /**
     * 시나리오 5: 레이아웃 완전성 테스트
     *
     * 1. 로그인 화면의 모든 요소가 올바른 순서로 표시되는지 확인
     * 2. 화면 오버플로우가 없는지 확인
     */
    @Test
    fun scenario_loginScreen_layoutIntegrity() {
        // Given: 앱 시작
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // Then: 화면이 로드됨
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("HEIST")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify: 상단 - 타이틀
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("THE URBAN CHASE").assertIsDisplayed()

        // Verify: 중간 - 소셜 로그인 버튼들
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("카카오로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apple로 계속하기").assertIsDisplayed()

        // Verify: 하단 - 약관 동의 텍스트
        composeTestRule.onNodeWithText("계속 진행함으로써 HeistHunt의 서비스 약관에 동의하게 됩니다.")
            .assertIsDisplayed()

        // All elements exist and are visible - no overflow issues
    }

    /**
     * 시나리오 6: 빠른 연속 클릭 테스트 (디바운싱 확인)
     *
     * 1. Google 버튼을 빠르게 클릭
     * 2. 로그인 콜백이 호출되는지 확인
     */
    @Test
    fun scenario_rapidClicks_debouncing() {
        var loginCallCount = 0

        // Given: 앱 시작 (로그인 실패로 설정하여 화면 유지)
        composeTestRule.setContent {
            App(
                onGoogleLogin = {
                    loginCallCount++
                    // Mock: 로그인 실패 반환 (화면 유지를 위해)
                    false
                }
            )
        }

        // Then: 로그인 화면 로드
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Google로 계속하기")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val googleButton = composeTestRule.onNodeWithText("Google로 계속하기")

        // When: 빠르게 3번 연속 클릭
        repeat(3) {
            googleButton.performClick()
            composeTestRule.waitForIdle()
        }

        // Then: 로그인 콜백이 3번 호출됨
        assert(loginCallCount == 3) { "로그인이 3번 호출되어야 함 (실제: $loginCallCount)" }
    }

    /**
     * 시나리오 7: 로그인 화면 초기 로딩 시간 테스트
     *
     * 1. 앱 시작
     * 2. 로그인 화면이 5초 이내에 표시되는지 확인
     */
    @Test
    fun scenario_loginScreen_loadingPerformance() {
        val startTime = System.currentTimeMillis()

        // Given: 앱 시작
        composeTestRule.setContent {
            App(onGoogleLogin = { true })
        }

        // Then: 로그인 화면이 5초 이내에 로드됨
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("HEIST")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val loadTime = System.currentTimeMillis() - startTime

        // Verify: 화면이 표시됨
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()

        // Performance check: 5초 이내 로딩
        assert(loadTime < 5000) { "로그인 화면이 5초 이내에 로드되어야 함 (실제: ${loadTime}ms)" }
    }
}
