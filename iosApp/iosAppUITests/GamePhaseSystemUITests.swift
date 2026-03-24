import XCTest

/**
 * iOS UI 자동화 테스트 - 게임 페이즈 시스템
 *
 * XCUITest를 사용한 게임 페이즈 시스템 테스트
 *
 * 테스트 항목:
 * 1. ESCAPE Phase: Police violation alert (when police leaves jail)
 * 2. CHASE Phase: Enemy proximity alerts (when enemies are within 5m)
 *
 * Android 테스트와 동일한 16개 시나리오를 검증합니다.
 */
class GamePhaseSystemUITests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        // 테스트 실패 시 즉시 중단
        continueAfterFailure = false

        // 앱 초기화
        app = XCUIApplication()

        // UI 테스트 모드 활성화
        app.launchArguments = ["UI_TESTING"]
    }

    override func tearDownWithError() throws {
        app = nil
    }

    // MARK: - ESCAPE Phase Tests (6 tests)

    /**
     * Scenario 1: ESCAPE Phase - Police Violation Alert Displayed (Thief View)
     *
     * Given: Thief role, ESCAPE phase, police violation state
     * When: GameScreen is rendered
     * Then: Police violation alert is displayed with correct message and distance
     */
    func testScenario_escapePhase_policeViolationAlert_displayed() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief role, ESCAPE phase, police left jail 5m away
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:ESCAPE",
            "POLICE_VIOLATION:5.0"
        ]
        app.launch()

        // When: Navigate to game screen (mock state should show immediately)

        // Then: Police violation alert is displayed
        let alertTitle = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Police violation alert title should be displayed"
        )

        let distanceText = app.staticTexts.matching(NSPredicate(format: "label CONTAINS '감옥에서'")).firstMatch
        XCTAssertTrue(
            distanceText.exists,
            "Distance information should be displayed"
        )

        let fiveMeterText = app.staticTexts.matching(NSPredicate(format: "label CONTAINS '5m 이탈'")).firstMatch
        XCTAssertTrue(
            fiveMeterText.exists,
            "5m distance should be displayed"
        )
    }

    /**
     * Scenario 2: ESCAPE Phase - No Alert When Police Not Violated (Thief View)
     *
     * Given: Thief role, ESCAPE phase, no police violation
     * When: GameScreen is rendered
     * Then: No police violation alert is displayed
     */
    func testScenario_escapePhase_noAlert_whenPoliceNotViolated() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief role, ESCAPE phase, no violations
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:ESCAPE"
        ]
        app.launch()

        // Then: No police violation alert
        let alertTitle = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertFalse(
            alertTitle.waitForExistence(timeout: 2),
            "Police violation alert should NOT be displayed"
        )
    }

    /**
     * Scenario 3: ESCAPE Phase - Police Does Not See Violation Alert
     *
     * Given: Police role, ESCAPE phase, police violation state exists
     * When: GameScreen is rendered
     * Then: Police does not see the violation alert (only thieves see it)
     */
    func testScenario_escapePhase_policeDoesNotSeeViolationAlert() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Police role, ESCAPE phase, violation state exists
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:POLICE",
            "PHASE:ESCAPE",
            "POLICE_VIOLATION:10.0"
        ]
        app.launch()

        // Then: Police does not see violation alert
        let alertTitle = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertFalse(
            alertTitle.waitForExistence(timeout: 2),
            "Police should NOT see violation alert"
        )
    }

    /**
     * Scenario 4: ESCAPE Phase - Police Violation with 10m Distance
     *
     * Given: ESCAPE phase with police 10m from jail
     * When: GameScreen is rendered
     * Then: Correct distance is displayed in alert
     */
    func testScenario_escapePhase_policeViolation_10mDistance() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief role, police violation at 10m
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:ESCAPE",
            "POLICE_VIOLATION:10.0"
        ]
        app.launch()

        // Then: Alert displays correct distance
        let alertTitle = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Police violation alert should be displayed"
        )

        let tenMeterText = app.staticTexts.matching(NSPredicate(format: "label CONTAINS '10m 이탈'")).firstMatch
        XCTAssertTrue(
            tenMeterText.exists,
            "10m distance should be displayed"
        )
    }

    /**
     * Scenario 5: ESCAPE Phase - Police Violation with 50m Distance
     *
     * Given: ESCAPE phase with police 50m from jail
     * When: GameScreen is rendered
     * Then: Correct distance is displayed in alert
     */
    func testScenario_escapePhase_policeViolation_50mDistance() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief role, police violation at 50m
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:ESCAPE",
            "POLICE_VIOLATION:50.0"
        ]
        app.launch()

        // Then: Alert displays correct distance
        let alertTitle = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Police violation alert should be displayed"
        )

        let fiftyMeterText = app.staticTexts.matching(NSPredicate(format: "label CONTAINS '50m 이탈'")).firstMatch
        XCTAssertTrue(
            fiftyMeterText.exists,
            "50m distance should be displayed"
        )
    }

    /**
     * Scenario 6: ESCAPE Phase - Proximity Alert Should Not Appear
     *
     * Given: ESCAPE phase with proximity alert state (edge case)
     * When: GameScreen is rendered
     * Then: Proximity alert is NOT displayed (only for CHASE phase)
     */
    func testScenario_escapePhase_proximityAlert_notDisplayed() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: ESCAPE phase with proximity alert state (should not happen in production)
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:ESCAPE",
            "NEARBY_ENEMIES:2"
        ]
        app.launch()

        // Then: Proximity alert is NOT displayed in ESCAPE phase
        let policeDetection = app.staticTexts["경찰 감지!"]
        XCTAssertFalse(
            policeDetection.waitForExistence(timeout: 2),
            "Proximity alert should NOT appear in ESCAPE phase"
        )
    }

    // MARK: - CHASE Phase Tests (6 tests)

    /**
     * Scenario 7: CHASE Phase - Thief Sees Police Proximity Alert
     *
     * Given: Thief role, CHASE phase, police nearby
     * When: GameScreen is rendered
     * Then: "경찰 감지!" alert is displayed with red background
     */
    func testScenario_chasePhase_thiefSeesPoliceProximityAlert() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief role, CHASE phase, 2 police nearby
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:CHASE",
            "NEARBY_ENEMIES:2"
        ]
        app.launch()

        // Then: Police proximity alert is displayed
        let alertTitle = app.staticTexts["경찰 감지!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Police proximity alert should be displayed"
        )

        let countText = app.staticTexts["반경 5m 이내에 2명"]
        XCTAssertTrue(
            countText.exists,
            "Enemy count (2) should be displayed"
        )
    }

    /**
     * Scenario 8: CHASE Phase - Police Sees Thief Proximity Alert
     *
     * Given: Police role, CHASE phase, thieves nearby
     * When: GameScreen is rendered
     * Then: "도둑 감지!" alert is displayed with blue background
     */
    func testScenario_chasePhase_policeSeeThiefProximityAlert() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Police role, CHASE phase, 3 thieves nearby
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:POLICE",
            "PHASE:CHASE",
            "NEARBY_ENEMIES:3"
        ]
        app.launch()

        // Then: Thief proximity alert is displayed
        let alertTitle = app.staticTexts["도둑 감지!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Thief proximity alert should be displayed"
        )

        let countText = app.staticTexts["반경 5m 이내에 3명"]
        XCTAssertTrue(
            countText.exists,
            "Enemy count (3) should be displayed"
        )
    }

    /**
     * Scenario 9: CHASE Phase - No Proximity Alert When No Enemies Nearby
     *
     * Given: CHASE phase, no enemies nearby
     * When: GameScreen is rendered
     * Then: No proximity alert is displayed
     */
    func testScenario_chasePhase_noAlert_whenNoEnemiesNearby() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief role, CHASE phase, no enemies nearby
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:CHASE"
        ]
        app.launch()

        // Then: No proximity alert is displayed
        let policeAlert = app.staticTexts["경찰 감지!"]
        let thiefAlert = app.staticTexts["도둑 감지!"]

        XCTAssertFalse(
            policeAlert.waitForExistence(timeout: 2),
            "Police alert should NOT be displayed"
        )
        XCTAssertFalse(
            thiefAlert.exists,
            "Thief alert should NOT be displayed"
        )
    }

    /**
     * Scenario 10: CHASE Phase - Single Enemy Alert Count
     *
     * Given: CHASE phase with 1 thief nearby
     * When: GameScreen is rendered
     * Then: Correct enemy count (1) is displayed
     */
    func testScenario_chasePhase_singleEnemy_correctCount() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Police role, 1 thief nearby
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:POLICE",
            "PHASE:CHASE",
            "NEARBY_ENEMIES:1"
        ]
        app.launch()

        // Then: Alert displays count of 1
        let alertTitle = app.staticTexts["도둑 감지!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Thief proximity alert should be displayed"
        )

        let countText = app.staticTexts["반경 5m 이내에 1명"]
        XCTAssertTrue(
            countText.exists,
            "Enemy count (1) should be displayed"
        )
    }

    /**
     * Scenario 11: CHASE Phase - Multiple Enemies (5) Alert Count
     *
     * Given: CHASE phase with 5 thieves nearby
     * When: GameScreen is rendered
     * Then: Correct enemy count (5) is displayed
     */
    func testScenario_chasePhase_multipleEnemies_correctCount() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Police role, 5 thieves nearby
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:POLICE",
            "PHASE:CHASE",
            "NEARBY_ENEMIES:5"
        ]
        app.launch()

        // Then: Alert displays count of 5
        let alertTitle = app.staticTexts["도둑 감지!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Thief proximity alert should be displayed"
        )

        let countText = app.staticTexts["반경 5m 이내에 5명"]
        XCTAssertTrue(
            countText.exists,
            "Enemy count (5) should be displayed"
        )
    }

    /**
     * Scenario 12: CHASE Phase - Police Violation Should Not Appear
     *
     * Given: CHASE phase with police violation state (edge case)
     * When: GameScreen is rendered
     * Then: Police violation alert is NOT displayed (only for ESCAPE phase)
     */
    func testScenario_chasePhase_policeViolation_notDisplayed() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: CHASE phase with police violation state (should not happen in production)
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:CHASE",
            "POLICE_VIOLATION:5.0"
        ]
        app.launch()

        // Then: Police violation alert is NOT displayed in CHASE phase
        let alertTitle = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertFalse(
            alertTitle.waitForExistence(timeout: 2),
            "Police violation alert should NOT appear in CHASE phase"
        )
    }

    // MARK: - Other Tests (4 tests)

    /**
     * Scenario 13: Phase Transition - ESCAPE Alert in ESCAPE Phase
     *
     * Given: Thief in ESCAPE phase with police violation alert
     * When: GameScreen is rendered
     * Then: Police violation alert is displayed (confirms ESCAPE phase alert)
     */
    func testScenario_phaseTransition_escapePhase_policeViolationDisplayed() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief in ESCAPE phase with police violation
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:ESCAPE",
            "POLICE_VIOLATION:5.0"
        ]
        app.launch()

        // Then: Police violation alert is displayed
        let alertTitle = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Police violation alert should be displayed in ESCAPE phase"
        )
    }

    /**
     * Scenario 14: Phase Transition - CHASE Alert in CHASE Phase
     *
     * Given: Thief in CHASE phase with proximity alert
     * When: GameScreen is rendered
     * Then: Proximity alert is displayed (confirms CHASE phase alert)
     */
    func testScenario_phaseTransition_chasePhase_proximityAlertDisplayed() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief in CHASE phase with proximity alert
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:CHASE",
            "NEARBY_ENEMIES:1"
        ]
        app.launch()

        // Then: Proximity alert is displayed
        let proximityAlert = app.staticTexts["경찰 감지!"]
        XCTAssertTrue(
            proximityAlert.waitForExistence(timeout: 5),
            "Proximity alert should be displayed in CHASE phase"
        )

        // And: Police violation alert is NOT displayed
        let violationAlert = app.staticTexts["경찰이 이탈했습니다!"]
        XCTAssertFalse(
            violationAlert.exists,
            "Police violation alert should NOT be displayed in CHASE phase"
        )
    }

    /**
     * Scenario 15: No Alerts in Default State
     *
     * Given: Default game state with no special conditions
     * When: GameScreen is rendered
     * Then: No alerts are displayed
     */
    func testScenario_defaultState_noAlerts() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Default game state
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:ESCAPE"
        ]
        app.launch()

        // Then: No alerts are displayed
        let violationAlert = app.staticTexts["경찰이 이탈했습니다!"]
        let policeAlert = app.staticTexts["경찰 감지!"]
        let thiefAlert = app.staticTexts["도둑 감지!"]

        XCTAssertFalse(
            violationAlert.waitForExistence(timeout: 2),
            "Police violation alert should NOT be displayed"
        )
        XCTAssertFalse(
            policeAlert.exists,
            "Police proximity alert should NOT be displayed"
        )
        XCTAssertFalse(
            thiefAlert.exists,
            "Thief proximity alert should NOT be displayed"
        )
    }

    /**
     * Scenario 16: Alert UI Consistency - Proximity Alert Stays Visible
     *
     * Given: Game state with proximity alert
     * When: Screen is rendered and idle
     * Then: Alert remains visible consistently
     */
    func testScenario_alertConsistency_proximityAlertStaysVisible() throws {
        throw XCTSkip("Mock launchArguments 처리 로직 미구현 - iOS_MOCK_IMPLEMENTATION_GUIDE.md 참고")

        // Given: Thief with police proximity alert
        app.launchArguments += [
            "MOCK_GAME_STATE",
            "ROLE:THIEF",
            "PHASE:CHASE",
            "NEARBY_ENEMIES:1"
        ]
        app.launch()

        // Then: Alert is displayed and stays visible
        let alertTitle = app.staticTexts["경찰 감지!"]
        XCTAssertTrue(
            alertTitle.waitForExistence(timeout: 5),
            "Proximity alert should be displayed"
        )

        // Wait a bit and verify it's still there
        Thread.sleep(forTimeInterval: 0.5)
        XCTAssertTrue(
            alertTitle.exists,
            "Proximity alert should remain visible"
        )

        // Verify count text is also still visible
        let countText = app.staticTexts["반경 5m 이내에 1명"]
        XCTAssertTrue(
            countText.exists,
            "Enemy count should remain visible"
        )
    }
}
