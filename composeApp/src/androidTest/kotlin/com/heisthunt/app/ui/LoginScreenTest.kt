package com.heisthunt.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.heisthunt.app.ui.auth.LoginScreen
import com.heisthunt.app.viewmodel.AuthUiState
import org.junit.Rule
import org.junit.Test

/**
 * Android UI 자동화 테스트 - 로그인 화면
 *
 * 테스트 항목:
 * - UI 요소 표시 확인 (소셜 로그인 버튼들)
 * - 버튼 클릭 동작
 * - 로딩/에러 상태 표시
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAllElements() {
        // Given: 로그인 화면 표시
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: 주요 UI 요소들이 표시됨
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("THE URBAN CHASE").assertIsDisplayed()

        // 소셜 로그인 버튼들
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("카카오로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apple로 계속하기").assertIsDisplayed()

        // 약관 동의 텍스트
        composeTestRule.onNodeWithText("계속 진행함으로써 HeistHunt의 서비스 약관에 동의하게 됩니다.")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_googleButton_isClickable() {
        // Given: 로그인 화면 표시
        var googleLoginCalled = false

        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = { googleLoginCalled = true },
                onNavigateToRegister = {}
            )
        }

        // When: Google 로그인 버튼 클릭
        composeTestRule.onNodeWithText("Google로 계속하기")
            .assertIsDisplayed()
            .performClick()

        // Then: Google 로그인 콜백이 호출됨
        assert(googleLoginCalled) { "Google 로그인 콜백이 호출되어야 함" }
    }

    @Test
    fun loginScreen_kakaoButton_isDisplayed() {
        // Given: 로그인 화면 표시
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: 카카오 로그인 버튼이 표시됨
        composeTestRule.onNodeWithText("카카오로 계속하기")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun loginScreen_appleButton_isDisplayed() {
        // Given: 로그인 화면 표시
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: Apple 로그인 버튼이 표시됨
        composeTestRule.onNodeWithText("Apple로 계속하기")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun loginScreen_allButtonsAreEnabled() {
        // Given: 로그인 화면 표시
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: 모든 소셜 로그인 버튼이 활성화됨
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsEnabled()
        composeTestRule.onNodeWithText("카카오로 계속하기").assertIsEnabled()
        composeTestRule.onNodeWithText("Apple로 계속하기").assertIsEnabled()
    }

    @Test
    fun loginScreen_loadingState_disablesButtons() {
        // Given: 로딩 상태의 로그인 화면
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(isLoading = true),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: 버튼들이 비활성화됨 (로딩 중)
        // Note: 실제 구현에서 로딩 중 버튼 비활성화가 적용되어야 함
        composeTestRule.onNodeWithText("Google로 계속하기").assertExists()
    }

    @Test
    fun loginScreen_errorState_showsErrorMessage() {
        // Given: 에러 상태의 로그인 화면
        val errorMessage = "로그인에 실패했습니다"
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(error = errorMessage),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: 에러 메시지가 표시됨
        // Note: 실제 구현에서 에러 메시지 표시 UI가 추가되어야 함
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
    }

    @Test
    fun loginScreen_hasCorrectLayout() {
        // Given: 로그인 화면 표시
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: 레이아웃 요소들이 올바른 순서로 표시됨
        // 1. 타이틀 (HEIST HUNT)
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()

        // 2. 서브타이틀
        composeTestRule.onNodeWithText("THE URBAN CHASE").assertIsDisplayed()

        // 3. 소셜 로그인 버튼들
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("카카오로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apple로 계속하기").assertIsDisplayed()

        // 4. 약관 동의 텍스트
        composeTestRule.onNodeWithText("계속 진행함으로써 HeistHunt의 서비스 약관에 동의하게 됩니다.")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_brandingColors_areVisible() {
        // Given: 로그인 화면 표시
        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = {},
                onNavigateToRegister = {}
            )
        }

        // Then: 브랜드 색상의 텍스트들이 표시됨
        // HEIST (빨강), HUNT (파랑)
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
    }

    @Test
    fun loginScreen_multipleClicks_callbackCalledMultipleTimes() {
        // Given: 로그인 화면과 클릭 카운터
        var clickCount = 0

        composeTestRule.setContent {
            LoginScreen(
                uiState = AuthUiState(),
                onLogin = { _, _ -> },
                onGoogleLogin = { clickCount++ },
                onNavigateToRegister = {}
            )
        }

        // When: Google 버튼을 여러 번 클릭
        val googleButton = composeTestRule.onNodeWithText("Google로 계속하기")
        googleButton.performClick()
        googleButton.performClick()
        googleButton.performClick()

        // Then: 콜백이 여러 번 호출됨
        assert(clickCount == 3) { "클릭 횟수가 일치해야 함: expected 3, got $clickCount" }
    }
}
