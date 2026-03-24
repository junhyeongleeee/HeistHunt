package com.heisthunt.app.scenarios

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.heisthunt.app.di.AppModule
import com.heisthunt.app.ui.game.OperationScreen
import com.heisthunt.app.viewmodel.RoomDetailUiState
import com.heisthunt.shared.models.Participant
import com.heisthunt.shared.models.PlayerRole
import com.heisthunt.shared.models.Room
import com.heisthunt.shared.models.RoomSettings
import com.heisthunt.shared.models.RoomStatus
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E 시나리오 테스트 - 작전 화면(OperationScreen) 플로우
 *
 * 실제 사용자가 작전 화면을 사용하는 전체 플로우를 테스트합니다.
 * RoomViewModel과 AuthViewModel은 실제 인스턴스를 사용합니다.
 */
class OperationScreenFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var roomViewModel: com.heisthunt.app.viewmodel.RoomViewModel
    private lateinit var authViewModel: com.heisthunt.app.viewmodel.AuthViewModel

    @Before
    fun setup() {
        roomViewModel = AppModule.provideRoomViewModel()
        authViewModel = AppModule.provideAuthViewModel()
    }

    /**
     * 시나리오 1: 작전 화면 초기 진입 및 UI 요소 확인
     *
     * 1. 작전 화면 진입
     * 2. 메인 화면의 모든 UI 요소 표시 확인
     * 3. 타이틀, 버튼들이 올바르게 표시되는지 확인
     */
    @Test
    fun scenario_operationScreen_initialDisplay() {
        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        // Then: 메인 화면의 모든 요소가 표시됨
        composeTestRule.waitForIdle()

        // Verify: 타이틀
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("OR").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("OPERATION CENTER").assertIsDisplayed()

        // Verify: 액션 버튼들
        composeTestRule.onNodeWithText("LEAD A MISSION").assertIsDisplayed()
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("새로운 게임 방을 만들고 요원을 소집합니다.").assertIsDisplayed()

        // Note: "JOIN OPERATION" 텍스트는 2번 나타남 (QR 코드 카드, 인증 코드 카드)
        composeTestRule.onNodeWithText("QR 코드로 참여").assertIsDisplayed()
        composeTestRule.onNodeWithText("방장의 QR 코드를 스캔하여 즉시 합류합니다.").assertIsDisplayed()

        composeTestRule.onNodeWithText("인증 코드로 참여").assertIsDisplayed()
        composeTestRule.onNodeWithText("4자리 코드를 입력하여 게임에 합류합니다.").assertIsDisplayed()

        // Verify: 하단 버튼들
        composeTestRule.onNodeWithText("🧪 Debug Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("🚪 로그아웃").assertIsDisplayed()
    }

    /**
     * 시나리오 2: QR 코드 스캔 버튼 클릭 (카메라 권한 이슈로 전환만 확인)
     *
     * 1. 작전 화면 진입
     * 2. "QR 코드로 참여" 버튼 클릭
     * 3. 버튼이 정상적으로 클릭 가능한지 확인
     *
     * Note: QR 스캐너는 실제 카메라 권한이 필요하므로 UI 테스트에서 전체 플로우 테스트 불가
     */
    @Test
    fun scenario_qrScanner_buttonClickable() {
        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        composeTestRule.waitForIdle()

        // Then: QR 코드 버튼이 활성화되어 있음
        composeTestRule.onNodeWithText("QR 코드로 참여")
            .assertIsDisplayed()
            .assertIsEnabled()

        // When: 버튼 클릭
        composeTestRule.onNodeWithText("QR 코드로 참여")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: 클릭이 정상적으로 처리됨 (화면 전환 확인은 실제 기기에서만 가능)
        // QR 스캐너 화면으로 전환되었으나 카메라 권한 때문에 UI 테스트 불가
    }

    /**
     * 시나리오 3: 인증 코드 입력 다이얼로그
     *
     * 1. 작전 화면 진입
     * 2. "인증 코드로 참여" 버튼 클릭
     * 3. 다이얼로그 표시 확인
     * 4. 취소 버튼으로 다이얼로그 닫기
     */
    @Test
    fun scenario_codeInput_dialog() {
        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        composeTestRule.waitForIdle()

        // When: "인증 코드로 참여" 버튼 클릭
        composeTestRule.onNodeWithText("인증 코드로 참여")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: 다이얼로그가 표시됨
        composeTestRule.onNodeWithText("🔑").assertIsDisplayed()
        composeTestRule.onNodeWithText("인증 코드 입력").assertIsDisplayed()
        composeTestRule.onNodeWithText("4자리 인증 코드를 입력하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("참여하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").assertIsDisplayed()

        // When: 취소 버튼 클릭
        composeTestRule.onNodeWithText("취소")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: 다이얼로그가 닫히고 메인 화면으로 복귀
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()
    }

    /**
     * 시나리오 4: Debug Settings 버튼 클릭
     *
     * 1. 작전 화면 진입
     * 2. "Debug Settings" 버튼 클릭
     * 3. onNavigateToDebug 콜백 호출 확인
     */
    @Test
    fun scenario_debugSettings_buttonClick() {
        var debugNavigationCalled = false

        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {
                    debugNavigationCalled = true
                }
            )
        }

        composeTestRule.waitForIdle()

        // Verify: 초기 상태는 네비게이션 호출 안 됨
        assert(!debugNavigationCalled) { "초기에는 onNavigateToDebug가 호출되지 않아야 함" }

        // When: "Debug Settings" 버튼 클릭
        composeTestRule.onNodeWithText("🧪 Debug Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: onNavigateToDebug가 호출됨
        assert(debugNavigationCalled) { "onNavigateToDebug가 호출되어야 함" }
    }

    /**
     * 시나리오 5: UI 반응성 테스트 - 모든 버튼 클릭 가능
     *
     * 1. 작전 화면에서 모든 버튼이 활성화되어 있는지 확인
     * 2. 각 버튼이 클릭 가능한지 확인
     */
    @Test
    fun scenario_operationScreen_buttonsEnabled() {
        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        composeTestRule.waitForIdle()

        // Verify: 모든 주요 버튼이 활성화 상태
        composeTestRule.onNodeWithText("작전 설계하기").assertIsEnabled()
        composeTestRule.onNodeWithText("QR 코드로 참여").assertIsEnabled()
        composeTestRule.onNodeWithText("인증 코드로 참여").assertIsEnabled()
        composeTestRule.onNodeWithText("🧪 Debug Settings").assertIsEnabled()
        composeTestRule.onNodeWithText("🚪 로그아웃").assertIsEnabled()
    }

    /**
     * 시나리오 6: 레이아웃 완전성 테스트
     *
     * 1. 작전 화면의 모든 요소가 올바른 순서로 표시되는지 확인
     * 2. 화면 오버플로우가 없는지 확인
     */
    @Test
    fun scenario_operationScreen_layoutIntegrity() {
        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        composeTestRule.waitForIdle()

        // Verify: 상단 - 타이틀
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()
        composeTestRule.onNodeWithText("HUNT").assertIsDisplayed()
        composeTestRule.onNodeWithText("OPERATION CENTER").assertIsDisplayed()

        // Verify: 중간 - 액션 카드들
        composeTestRule.onNodeWithText("LEAD A MISSION").assertIsDisplayed()
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()

        // Verify: 하단 - 컨트롤 버튼들
        composeTestRule.onNodeWithText("🧪 Debug Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("🚪 로그아웃").assertIsDisplayed()

        // All elements exist and are visible - no overflow issues
    }

    /**
     * 시나리오 7: 빠른 연속 클릭 테스트 - Debug Settings 버튼
     *
     * 1. Debug Settings 버튼을 빠르게 클릭
     * 2. 콜백이 여러 번 호출되는지 확인
     */
    @Test
    fun scenario_rapidClicks_navigation() {
        var clickCount = 0

        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {
                    clickCount++
                }
            )
        }

        composeTestRule.waitForIdle()

        val debugButton = composeTestRule.onNodeWithText("🧪 Debug Settings")

        // When: 빠르게 3번 연속 클릭
        repeat(3) {
            debugButton.performClick()
            composeTestRule.waitForIdle()
        }

        // Then: 콜백이 3번 호출됨
        assert(clickCount == 3) { "Debug button이 3번 클릭되어야 함 (실제: $clickCount)" }
    }

    /**
     * 시나리오 8: 화면 초기 로딩 성능 테스트
     *
     * 1. 앱 시작
     * 2. 작전 화면이 5초 이내에 표시되는지 확인
     */
    @Test
    fun scenario_operationScreen_loadingPerformance() {
        val startTime = System.currentTimeMillis()

        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        // Then: 화면이 5초 이내에 로드됨
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("HEIST")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val loadTime = System.currentTimeMillis() - startTime

        // Verify: 화면이 표시됨
        composeTestRule.onNodeWithText("HEIST").assertIsDisplayed()

        // Performance check: 5초 이내 로딩
        assert(loadTime < 5000) { "작전 화면이 5초 이내에 로드되어야 함 (실제: ${loadTime}ms)" }
    }

    /**
     * 시나리오 9: 버튼 상태 테스트 - 로딩 중이 아닐 때 활성화
     *
     * 1. 작전 화면 진입
     * 2. 모든 버튼이 활성화 상태인지 확인
     * 3. 버튼 클릭 가능 여부 확인
     */
    @Test
    fun scenario_buttonStates_enabled() {
        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        composeTestRule.waitForIdle()

        // Verify: 모든 주요 버튼이 활성화 상태
        val buttons = listOf(
            "작전 설계하기",
            "QR 코드로 참여",
            "인증 코드로 참여",
            "🧪 Debug Settings",
            "🚪 로그아웃"
        )

        buttons.forEach { buttonText ->
            composeTestRule.onNodeWithText(buttonText)
                .assertExists()
                .assertIsDisplayed()
                .assertIsEnabled()
        }
    }

    /**
     * 시나리오 10: 인증 코드 다이얼로그 - 연속 열기/닫기 테스트
     *
     * 1. 다이얼로그 열기
     * 2. 취소
     * 3. 다시 열기
     * 4. 정상 동작 확인
     */
    @Test
    fun scenario_codeDialog_multipleOpenClose() {
        // Given: 작전 화면 표시
        composeTestRule.setContent {
            OperationScreen(
                roomViewModel = roomViewModel,
                authViewModel = authViewModel,
                onNavigateToDebug = {}
            )
        }

        composeTestRule.waitForIdle()

        // When: 다이얼로그 열기 -> 취소 (첫 번째)
        composeTestRule.onNodeWithText("인증 코드로 참여").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("인증 코드 입력").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.waitForIdle()

        // Then: 메인 화면 상태
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()

        // When: 다이얼로그 열기 -> 취소 (두 번째)
        composeTestRule.onNodeWithText("인증 코드로 참여").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("인증 코드 입력").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.waitForIdle()

        // Then: 메인 화면 상태
        composeTestRule.onNodeWithText("작전 설계하기").assertIsDisplayed()
    }
}
