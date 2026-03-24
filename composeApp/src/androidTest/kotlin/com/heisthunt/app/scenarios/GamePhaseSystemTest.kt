package com.heisthunt.app.scenarios

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.heisthunt.app.ui.game.GameScreen
import com.heisthunt.app.viewmodel.*
import com.heisthunt.shared.models.*
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

/**
 * E2E Test - Game Phase System
 *
 * Tests the core game phase alert system:
 * 1. ESCAPE Phase: Police violation alert (when police leaves jail)
 * 2. CHASE Phase: Enemy proximity alerts (when enemies are within 5m)
 *
 * All tests use mock data to avoid external dependencies.
 */
class GamePhaseSystemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Scenario 1: ESCAPE Phase - Police Violation Alert Displayed (Thief View)
     *
     * Given: Thief role, ESCAPE phase, police violation state
     * When: GameScreen is rendered
     * Then: Police violation alert is displayed with correct message and distance
     */
    @Test
    fun scenario_escapePhase_policeViolationAlert_displayed() {
        // Given: Thief role, ESCAPE phase, police left jail 5m away
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "ESCAPE",
            localEscapeRemainingSeconds = 250,
            policeViolation = PoliceViolationState(
                policeUserId = "police-1",
                distanceFromJail = 5.0,
                timestamp = Clock.System.now()
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Police violation alert is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("감옥에서 5m 이탈")
            .assertIsDisplayed()
    }

    /**
     * Scenario 2: ESCAPE Phase - No Alert When Police Not Violated (Thief View)
     *
     * Given: Thief role, ESCAPE phase, no police violation
     * When: GameScreen is rendered
     * Then: No police violation alert is displayed
     */
    @Test
    fun scenario_escapePhase_noAlert_whenPoliceNotViolated() {
        // Given: Thief role, ESCAPE phase, no violations
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "ESCAPE",
            localEscapeRemainingSeconds = 250,
            policeViolation = null
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: No police violation alert
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertDoesNotExist()
    }

    /**
     * Scenario 3: ESCAPE Phase - Police Does Not See Violation Alert
     *
     * Given: Police role, ESCAPE phase, police violation state exists
     * When: GameScreen is rendered
     * Then: Police does not see the violation alert (only thieves see it)
     */
    @Test
    fun scenario_escapePhase_policeDoesNotSeeViolationAlert() {
        // Given: Police role, ESCAPE phase, violation state exists
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.POLICE,
            currentPhase = "ESCAPE",
            localEscapeRemainingSeconds = 250,
            policeViolation = PoliceViolationState(
                policeUserId = "police-1",
                distanceFromJail = 10.0,
                timestamp = Clock.System.now()
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.POLICE,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Police does not see violation alert
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertDoesNotExist()
    }

    /**
     * Scenario 4: CHASE Phase - Thief Sees Police Proximity Alert
     *
     * Given: Thief role, CHASE phase, police nearby
     * When: GameScreen is rendered
     * Then: "경찰 감지!" alert is displayed with red background
     */
    @Test
    fun scenario_chasePhase_thiefSeesPoliceProximityAlert() {
        // Given: Thief role, CHASE phase, 2 police nearby
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 0,
            nearbyEnemies = ProximityAlertState(
                enemyRole = PlayerRole.POLICE,
                count = 2
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Police proximity alert is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰 감지!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("반경 5m 이내에 2명")
            .assertIsDisplayed()
    }

    /**
     * Scenario 5: CHASE Phase - Police Sees Thief Proximity Alert
     *
     * Given: Police role, CHASE phase, thieves nearby
     * When: GameScreen is rendered
     * Then: "도둑 감지!" alert is displayed with blue background
     */
    @Test
    fun scenario_chasePhase_policeSeeThiefProximityAlert() {
        // Given: Police role, CHASE phase, 3 thieves nearby
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.POLICE,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 0,
            nearbyEnemies = ProximityAlertState(
                enemyRole = PlayerRole.THIEF,
                count = 3
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.POLICE,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Thief proximity alert is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("도둑 감지!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("반경 5m 이내에 3명")
            .assertIsDisplayed()
    }

    /**
     * Scenario 6: CHASE Phase - No Proximity Alert When No Enemies Nearby
     *
     * Given: CHASE phase, no enemies nearby
     * When: GameScreen is rendered
     * Then: No proximity alert is displayed
     */
    @Test
    fun scenario_chasePhase_noAlert_whenNoEnemiesNearby() {
        // Given: Thief role, CHASE phase, no enemies nearby
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 0,
            nearbyEnemies = null
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: No proximity alert is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰 감지!")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("도둑 감지!")
            .assertDoesNotExist()
    }

    /**
     * Scenario 7: Phase Transition - ESCAPE Alert in ESCAPE Phase
     *
     * Given: Thief in ESCAPE phase with police violation alert
     * When: GameScreen is rendered
     * Then: Police violation alert is displayed (confirms ESCAPE phase alert)
     */
    @Test
    fun scenario_phaseTransition_escapePhase_policeViolationDisplayed() {
        // Given: Thief in ESCAPE phase with police violation
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "ESCAPE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 250,
            policeViolation = PoliceViolationState(
                policeUserId = "police-1",
                distanceFromJail = 5.0,
                timestamp = Clock.System.now()
            ),
            nearbyEnemies = null
        )

        // When: Render ESCAPE phase
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Police violation alert is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertIsDisplayed()
    }

    /**
     * Scenario 7b: Phase Transition - CHASE Alert in CHASE Phase
     *
     * Given: Thief in CHASE phase with proximity alert
     * When: GameScreen is rendered
     * Then: Proximity alert is displayed (confirms CHASE phase alert)
     */
    @Test
    fun scenario_phaseTransition_chasePhase_proximityAlertDisplayed() {
        // Given: Thief in CHASE phase with proximity alert
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 0,
            policeViolation = null,
            nearbyEnemies = ProximityAlertState(
                enemyRole = PlayerRole.POLICE,
                count = 1
            )
        )

        // When: Render CHASE phase
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Proximity alert is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰 감지!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertDoesNotExist()
    }

    /**
     * Scenario 8: ESCAPE Phase - Police Violation with 10m Distance
     *
     * Given: ESCAPE phase with police 10m from jail
     * When: GameScreen is rendered
     * Then: Correct distance is displayed in alert
     */
    @Test
    fun scenario_escapePhase_policeViolation_10mDistance() {
        // Given: Thief role, police violation at 10m
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "ESCAPE",
            localEscapeRemainingSeconds = 250,
            policeViolation = PoliceViolationState(
                policeUserId = "police-1",
                distanceFromJail = 10.0,
                timestamp = Clock.System.now()
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Alert displays correct distance
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("감옥에서 10m 이탈")
            .assertIsDisplayed()
    }

    /**
     * Scenario 8b: ESCAPE Phase - Police Violation with 50m Distance
     *
     * Given: ESCAPE phase with police 50m from jail
     * When: GameScreen is rendered
     * Then: Correct distance is displayed in alert
     */
    @Test
    fun scenario_escapePhase_policeViolation_50mDistance() {
        // Given: Thief role, police violation at 50m
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "ESCAPE",
            localEscapeRemainingSeconds = 250,
            policeViolation = PoliceViolationState(
                policeUserId = "police-1",
                distanceFromJail = 50.0,
                timestamp = Clock.System.now()
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Alert displays correct distance
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("감옥에서 50m 이탈")
            .assertIsDisplayed()
    }

    /**
     * Scenario 9: CHASE Phase - Single Enemy Alert Count
     *
     * Given: CHASE phase with 1 thief nearby
     * When: GameScreen is rendered
     * Then: Correct enemy count (1) is displayed
     */
    @Test
    fun scenario_chasePhase_singleEnemy_correctCount() {
        // Given: Police role, 1 thief nearby
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.POLICE,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 0,
            nearbyEnemies = ProximityAlertState(
                enemyRole = PlayerRole.THIEF,
                count = 1
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.POLICE,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Alert displays count of 1
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("도둑 감지!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("반경 5m 이내에 1명")
            .assertIsDisplayed()
    }

    /**
     * Scenario 9b: CHASE Phase - Multiple Enemies (5) Alert Count
     *
     * Given: CHASE phase with 5 thieves nearby
     * When: GameScreen is rendered
     * Then: Correct enemy count (5) is displayed
     */
    @Test
    fun scenario_chasePhase_multipleEnemies_correctCount() {
        // Given: Police role, 5 thieves nearby
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.POLICE,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 0,
            nearbyEnemies = ProximityAlertState(
                enemyRole = PlayerRole.THIEF,
                count = 5
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.POLICE,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Alert displays count of 5
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("도둑 감지!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("반경 5m 이내에 5명")
            .assertIsDisplayed()
    }

    /**
     * Scenario 10: No Alerts in Default State
     *
     * Given: Default game state with no special conditions
     * When: GameScreen is rendered
     * Then: No alerts are displayed
     */
    @Test
    fun scenario_defaultState_noAlerts() {
        // Given: Default game state
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "ESCAPE",
            localEscapeRemainingSeconds = 250,
            policeViolation = null,
            nearbyEnemies = null
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: No alerts are displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("경찰 감지!")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("도둑 감지!")
            .assertDoesNotExist()
    }

    /**
     * Scenario 11: CHASE Phase - Proximity Alert Should Not Appear in ESCAPE
     *
     * Given: ESCAPE phase with proximity alert state (edge case)
     * When: GameScreen is rendered
     * Then: Proximity alert is NOT displayed (only for CHASE phase)
     */
    @Test
    fun scenario_escapePhase_proximityAlert_notDisplayed() {
        // Given: ESCAPE phase with proximity alert state (should not happen in production)
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "ESCAPE",
            localEscapeRemainingSeconds = 250,
            nearbyEnemies = ProximityAlertState(
                enemyRole = PlayerRole.POLICE,
                count = 2
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Proximity alert is NOT displayed in ESCAPE phase
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰 감지!")
            .assertDoesNotExist()
    }

    /**
     * Scenario 12: CHASE Phase - Police Violation Should Not Appear
     *
     * Given: CHASE phase with police violation state (edge case)
     * When: GameScreen is rendered
     * Then: Police violation alert is NOT displayed (only for ESCAPE phase)
     */
    @Test
    fun scenario_chasePhase_policeViolation_notDisplayed() {
        // Given: CHASE phase with police violation state (should not happen in production)
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            localEscapeRemainingSeconds = 0,
            policeViolation = PoliceViolationState(
                policeUserId = "police-1",
                distanceFromJail = 5.0,
                timestamp = Clock.System.now()
            )
        )

        // When: GameScreen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Police violation alert is NOT displayed in CHASE phase
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰이 이탈했습니다!")
            .assertDoesNotExist()
    }

    /**
     * Scenario 13: Alert UI Consistency - Proximity Alert Stays Visible
     *
     * Given: Game state with proximity alert
     * When: Screen is rendered and idle
     * Then: Alert remains visible consistently
     */
    @Test
    fun scenario_alertConsistency_proximityAlertStaysVisible() {
        // Given: Thief with police proximity alert
        val uiState = GameUiState(
            gameId = "test-game",
            myRole = PlayerRole.THIEF,
            currentPhase = "CHASE",
            localRemainingSeconds = 600,
            nearbyEnemies = ProximityAlertState(
                enemyRole = PlayerRole.POLICE,
                count = 1
            )
        )

        // When: Screen is rendered
        composeTestRule.setContent {
            GameScreen(
                gameId = "test-game",
                myRole = PlayerRole.THIEF,
                room = null,
                onBack = {},
                uiState = uiState,
                onRequestCatch = { _, _, _ -> },
                onConfirmCatch = { _, _ -> },
                onRejectCatch = {},
                onDismissCatchRequest = {}
            )
        }

        // Then: Alert is displayed and stays visible
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("경찰 감지!")
            .assertIsDisplayed()

        // Wait a bit and verify it's still there
        Thread.sleep(500)
        composeTestRule.onNodeWithText("경찰 감지!")
            .assertIsDisplayed()
    }
}
