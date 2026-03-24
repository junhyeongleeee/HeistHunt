import XCTest

/**
 * iOS UI 자동화 테스트 - 게임 생성 화면 (작전 화면)
 *
 * XCUITest를 사용한 iOS 화면 테스트
 *
 * 테스트 항목 (Android CreateGameFlowTest.kt와 동일):
 * 1. UI 완전성 - 모든 UI 요소 표시 확인
 * 2. 성능 - 화면 로딩 10초 이내
 * 3. QR 버튼 - 클릭 가능 상태 확인
 * 4. 코드 입력 다이얼로그 - 열기/닫기
 * 5. 로그아웃 버튼 - 표시 확인
 * 6. 레이아웃 무결성 - 오버플로우 없음
 * 7. Debug Settings - 버튼 클릭 가능
 * 8. 반복 테스트 - 다이얼로그 반복 동작
 */
class CreateGameFlowUITests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        // 테스트 실패 시 즉시 중단
        continueAfterFailure = false

        // 앱 초기화
        app = XCUIApplication()

        // UI Testing 모드 활성화 (Mock 로그인)
        app.launchArguments = ["UI_TESTING"]

        // 앱 데이터 초기화 (로그인 상태 삭제)
        app.launchArguments.append("RESET_APP_STATE")

        // 앱 실행
        app.launch()

        // 로그인 진행 (모든 테스트 전에 로그인 필요)
        performMockLogin()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    // MARK: - Helper Methods

    /// Mock 로그인 수행 (Google 로그인 버튼 클릭)
    private func performMockLogin() {
        // Debug: 현재 화면 상태 출력
        print("📱 현재 화면 요소 개수:")
        print("  - Buttons: \(app.buttons.count)")
        print("  - StaticTexts: \(app.staticTexts.count)")
        print("  - All elements: \(app.descendants(matching: .any).count)")

        // Debug: 버튼 목록 출력
        print("📱 버튼 목록:")
        for i in 0..<min(app.buttons.count, 10) {
            let button = app.buttons.element(boundBy: i)
            print("  - Button \(i): \(button.label)")
        }

        let googleButton = app.buttons["Google로 계속하기"]
        print("🔍 Google 버튼 존재: \(googleButton.exists)")

        if googleButton.waitForExistence(timeout: 5) {
            print("✅ Google 버튼 발견, 탭 시도...")
            googleButton.tap()

            // 작전 화면 로드 대기
            let operationTitle = app.staticTexts["HUNT"]
            XCTAssertTrue(operationTitle.waitForExistence(timeout: 10), "로그인 후 작전 화면으로 이동해야 함")
        } else {
            print("❌ Google 버튼을 찾을 수 없음 - 타임아웃 발생")
            XCTFail("Google로 계속하기 버튼을 찾을 수 없습니다")
        }
    }

    // MARK: - Test 1: UI 완전성

    func testOperationScreen_uiCompleteness() throws {
        // Given: 작전 화면이 로드됨

        // Then: 타이틀 텍스트 확인
        XCTAssertTrue(app.staticTexts["HUNT"].exists, "HUNT 텍스트가 표시되어야 함")
        XCTAssertTrue(app.staticTexts["OR"].exists, "OR 텍스트가 표시되어야 함")
        XCTAssertTrue(app.staticTexts["HEIST"].exists, "HEIST 텍스트가 표시되어야 함")
        XCTAssertTrue(app.staticTexts["OPERATION CENTER"].exists, "OPERATION CENTER 서브타이틀이 표시되어야 함")

        // Then: 카드 라벨 확인
        let leadMissionLabels = app.staticTexts.matching(identifier: "LEAD A MISSION")
        XCTAssertTrue(leadMissionLabels.firstMatch.exists, "LEAD A MISSION 라벨이 표시되어야 함")

        let joinOperationLabels = app.staticTexts.matching(identifier: "JOIN OPERATION")
        XCTAssertGreaterThanOrEqual(joinOperationLabels.count, 2, "JOIN OPERATION 라벨이 2개 이상 표시되어야 함")

        // Then: 버튼 텍스트 확인
        XCTAssertTrue(app.buttons["작전 설계하기"].exists, "작전 설계하기 버튼이 표시되어야 함")
        XCTAssertTrue(app.buttons["QR 코드로 참여"].exists, "QR 코드로 참여 버튼이 표시되어야 함")
        XCTAssertTrue(app.buttons["인증 코드로 참여"].exists, "인증 코드로 참여 버튼이 표시되어야 함")

        // Then: 하단 버튼들 확인
        XCTAssertTrue(app.buttons["🧪 Debug Settings"].exists, "Debug Settings 버튼이 표시되어야 함")
        XCTAssertTrue(app.buttons["🚪 로그아웃"].exists, "로그아웃 버튼이 표시되어야 함")

        // Then: 설명 텍스트 확인
        XCTAssertTrue(app.staticTexts["새로운 게임 방을 만들고 요원을 소집합니다."].exists, "작전 설계 설명이 표시되어야 함")
        XCTAssertTrue(app.staticTexts["방장의 QR 코드를 스캔하여 즉시 합류합니다."].exists, "QR 스캔 설명이 표시되어야 함")
        XCTAssertTrue(app.staticTexts["4자리 코드를 입력하여 게임에 합류합니다."].exists, "코드 입력 설명이 표시되어야 함")

        // Then: 버튼들이 클릭 가능 상태 확인
        XCTAssertTrue(app.buttons["작전 설계하기"].isEnabled, "작전 설계하기 버튼이 활성화되어야 함")
        XCTAssertTrue(app.buttons["QR 코드로 참여"].isEnabled, "QR 코드로 참여 버튼이 활성화되어야 함")
        XCTAssertTrue(app.buttons["인증 코드로 참여"].isEnabled, "인증 코드로 참여 버튼이 활성화되어야 함")
    }

    // MARK: - Test 2: 성능 테스트

    func testOperationScreen_loadingPerformance() throws {
        // Given: 앱 재시작
        app.terminate()

        let startTime = Date()
        app.launch()

        // When: 로그인 진행
        let googleButton = app.buttons["Google로 계속하기"]
        XCTAssertTrue(googleButton.waitForExistence(timeout: 5))
        googleButton.tap()

        // Then: 작전 화면이 10초 이내에 로드됨
        let operationButton = app.buttons["작전 설계하기"]
        XCTAssertTrue(operationButton.waitForExistence(timeout: 10), "작전 화면이 10초 이내에 로드되어야 함")

        let loadTime = Date().timeIntervalSince(startTime)

        // Verify: 주요 UI 요소가 표시됨
        XCTAssertTrue(app.staticTexts["HUNT"].exists, "HUNT 타이틀이 표시되어야 함")
        XCTAssertTrue(app.buttons["작전 설계하기"].exists, "작전 설계하기 버튼이 표시되어야 함")

        // Performance check: 10초 이내 로딩
        XCTAssertLessThan(loadTime, 10.0, "작전 화면이 10초 이내에 로드되어야 함 (실제: \(String(format: "%.2f", loadTime))초)")
    }

    // MARK: - Test 3: QR 버튼 표시 및 클릭

    func testQRScannerButton_displayed() throws {
        // Given: 작전 화면이 로드됨
        let qrButton = app.buttons["QR 코드로 참여"]

        // Then: QR 코드로 참여 버튼이 표시되고 클릭 가능
        XCTAssertTrue(qrButton.exists, "QR 코드로 참여 버튼이 존재해야 함")
        XCTAssertTrue(qrButton.isEnabled, "QR 코드로 참여 버튼이 활성화되어야 함")
        XCTAssertTrue(qrButton.isHittable, "QR 코드로 참여 버튼이 탭 가능해야 함")
    }

    // MARK: - Test 4: 인증 코드 다이얼로그

    func testCodeInputDialog_openAndClose() throws {
        // Given: 작전 화면이 로드됨
        let codeButton = app.buttons["인증 코드로 참여"]
        XCTAssertTrue(codeButton.exists)

        // When: "인증 코드로 참여" 버튼 클릭
        codeButton.tap()

        // Then: 코드 입력 다이얼로그가 표시됨
        let dialogTitle = app.staticTexts["인증 코드 입력"]
        XCTAssertTrue(dialogTitle.waitForExistence(timeout: 5), "인증 코드 입력 다이얼로그가 표시되어야 함")

        // Verify: 다이얼로그 UI 요소
        XCTAssertTrue(app.staticTexts["🔑"].exists, "이모지 아이콘이 표시되어야 함")
        XCTAssertTrue(app.staticTexts["인증 코드 입력"].exists, "다이얼로그 타이틀이 표시되어야 함")
        XCTAssertTrue(app.staticTexts["4자리 인증 코드를 입력하세요"].exists, "안내 텍스트가 표시되어야 함")
        XCTAssertTrue(app.buttons["참여하기"].exists, "참여하기 버튼이 표시되어야 함")
        XCTAssertTrue(app.buttons["취소"].exists, "취소 버튼이 표시되어야 함")

        // When: 취소 버튼 클릭
        app.buttons["취소"].tap()

        // Then: 다이얼로그가 닫히고 메인 화면 유지
        XCTAssertTrue(app.buttons["작전 설계하기"].waitForExistence(timeout: 3), "메인 화면으로 돌아와야 함")
        XCTAssertFalse(dialogTitle.exists, "다이얼로그가 닫혀야 함")
    }

    // MARK: - Test 5: 로그아웃 버튼 표시

    func testLogoutButton_displayed() throws {
        // Given: 작전 화면이 로드됨
        let logoutButton = app.buttons["🚪 로그아웃"]

        // Then: 로그아웃 버튼이 표시되고 클릭 가능
        XCTAssertTrue(logoutButton.exists, "로그아웃 버튼이 존재해야 함")
        XCTAssertTrue(logoutButton.isEnabled, "로그아웃 버튼이 활성화되어야 함")
        XCTAssertTrue(logoutButton.isHittable, "로그아웃 버튼이 탭 가능해야 함")
    }

    // MARK: - Test 6: 레이아웃 무결성

    func testOperationScreen_layoutIntegrity() throws {
        // Given: 작전 화면이 로드됨

        // Verify: 상단 - 타이틀
        XCTAssertTrue(app.staticTexts["HUNT"].exists, "HUNT 타이틀이 표시되어야 함")
        XCTAssertTrue(app.staticTexts["OR"].exists, "OR 타이틀이 표시되어야 함")
        XCTAssertTrue(app.staticTexts["HEIST"].exists, "HEIST 타이틀이 표시되어야 함")
        XCTAssertTrue(app.staticTexts["OPERATION CENTER"].exists, "서브타이틀이 표시되어야 함")

        // Verify: 중간 - 액션 버튼들
        XCTAssertTrue(app.buttons["작전 설계하기"].exists, "작전 설계하기 버튼이 표시되어야 함")
        XCTAssertTrue(app.buttons["QR 코드로 참여"].exists, "QR 코드로 참여 버튼이 표시되어야 함")
        XCTAssertTrue(app.buttons["인증 코드로 참여"].exists, "인증 코드로 참여 버튼이 표시되어야 함")

        // Verify: 하단 - 유틸리티 버튼들
        XCTAssertTrue(app.buttons["🧪 Debug Settings"].exists, "Debug Settings 버튼이 표시되어야 함")
        XCTAssertTrue(app.buttons["🚪 로그아웃"].exists, "로그아웃 버튼이 표시되어야 함")

        // Verify: 모든 요소가 화면 내에 표시됨 (스크롤 없이 접근 가능)
        XCTAssertTrue(app.staticTexts["HUNT"].isHittable, "상단 요소가 화면에 보여야 함")
        XCTAssertTrue(app.buttons["작전 설계하기"].isHittable, "중간 요소가 화면에 보여야 함")
        XCTAssertTrue(app.buttons["🚪 로그아웃"].isHittable, "하단 요소가 화면에 보여야 함")

        // All elements exist and are visible - no overflow issues
        print("✅ 레이아웃 무결성 테스트 통과: 모든 요소가 올바르게 표시됨")
    }

    // MARK: - Test 7: Debug Settings 네비게이션

    func testDebugSettings_navigation() throws {
        // Given: 작전 화면이 로드됨
        let debugButton = app.buttons["🧪 Debug Settings"]
        XCTAssertTrue(debugButton.exists, "Debug Settings 버튼이 존재해야 함")

        // Then: Debug Settings 버튼이 클릭 가능함
        XCTAssertTrue(debugButton.isEnabled, "Debug Settings 버튼이 활성화되어야 함")
        XCTAssertTrue(debugButton.isHittable, "Debug Settings 버튼이 탭 가능해야 함")

        // When: Debug Settings 버튼 클릭
        debugButton.tap()

        // Note: 디버그 화면의 구체적인 UI는 구현에 따라 다를 수 있음
        // 여기서는 버튼이 클릭 가능한지만 확인
        sleep(1) // 화면 전환 대기
        print("✅ Debug Settings 버튼 클릭 성공")
    }

    // MARK: - Test 8: 코드 다이얼로그 반복 테스트

    func testCodeDialog_repeated() throws {
        // Given: 작전 화면이 로드됨
        let codeButton = app.buttons["인증 코드로 참여"]

        // When/Then: 코드 입력 다이얼로그 반복 테스트 (2회)
        for iteration in 1...2 {
            print("📝 반복 테스트 \(iteration)/2")

            // 다이얼로그 열기
            XCTAssertTrue(codeButton.exists, "인증 코드로 참여 버튼이 존재해야 함")
            codeButton.tap()

            // 다이얼로그 표시 확인
            let dialogTitle = app.staticTexts["인증 코드 입력"]
            XCTAssertTrue(dialogTitle.waitForExistence(timeout: 5), "다이얼로그가 표시되어야 함")

            // 취소 버튼으로 닫기
            let cancelButton = app.buttons["취소"]
            XCTAssertTrue(cancelButton.exists, "취소 버튼이 존재해야 함")
            cancelButton.tap()

            // 메인 화면으로 복귀 확인
            XCTAssertTrue(codeButton.waitForExistence(timeout: 5), "메인 화면으로 돌아와야 함")
        }

        // Verify: 모든 작업 후에도 화면이 정상적으로 표시됨
        XCTAssertTrue(app.buttons["작전 설계하기"].exists, "작전 설계하기 버튼이 여전히 표시되어야 함")
        print("✅ 반복 테스트 통과: 다이얼로그가 정상적으로 동작함")
    }

    // MARK: - 추가 테스트: 코드 입력 필드 (보너스)

    func testCodeInputDialog_textFieldInteraction() throws {
        // Given: 인증 코드 다이얼로그 열기
        app.buttons["인증 코드로 참여"].tap()

        let dialogTitle = app.staticTexts["인증 코드 입력"]
        XCTAssertTrue(dialogTitle.waitForExistence(timeout: 5))

        // When: 텍스트 필드 찾기 및 입력
        let textField = app.textFields.firstMatch
        if textField.exists {
            textField.tap()
            textField.typeText("AB12")

            // Then: 입력된 텍스트 확인
            let fieldValue = textField.value as? String ?? ""
            XCTAssertTrue(fieldValue.contains("AB12") || fieldValue.count == 4, "텍스트가 입력되어야 함")
            print("✅ 텍스트 입력 성공: \(fieldValue)")
        }

        // Cleanup: 다이얼로그 닫기
        app.buttons["취소"].tap()
    }
}
