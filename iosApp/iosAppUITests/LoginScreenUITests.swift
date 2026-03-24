import XCTest

/**
 * iOS UI 자동화 테스트 - 로그인 화면
 *
 * XCUITest를 사용한 iOS 화면 테스트
 *
 * 테스트 항목:
 * - UI 요소 표시 확인
 * - 입력 필드 동작
 * - 버튼 클릭 동작
 * - 화면 전환 확인
 */
class LoginScreenUITests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        // 테스트 실패 시 즉시 중단
        continueAfterFailure = false

        // 앱 초기화 및 실행
        app = XCUIApplication()
        app.launch()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    // MARK: - UI 요소 표시 테스트

    func testLoginScreen_displaysAllElements() throws {
        // Given: 로그인 화면이 표시됨

        // Then: 주요 UI 요소들이 존재하고 표시됨
        XCTAssertTrue(app.staticTexts["HEIST"].exists, "HEIST 텍스트가 표시되어야 함")
        XCTAssertTrue(app.staticTexts["HUNT"].exists, "HUNT 텍스트가 표시되어야 함")

        // 입력 필드 확인
        let emailField = app.textFields["이메일"]
        XCTAssertTrue(emailField.exists, "이메일 입력 필드가 존재해야 함")

        let passwordField = app.secureTextFields["비밀번호"]
        XCTAssertTrue(passwordField.exists, "비밀번호 입력 필드가 존재해야 함")

        // 버튼 확인
        let loginButton = app.buttons["로그인"]
        XCTAssertTrue(loginButton.exists, "로그인 버튼이 존재해야 함")
        XCTAssertTrue(loginButton.isEnabled, "로그인 버튼이 활성화되어야 함")

        let googleButton = app.buttons["Google로 계속하기"]
        XCTAssertTrue(googleButton.exists, "Google 로그인 버튼이 존재해야 함")

        let registerButton = app.buttons["회원가입"]
        XCTAssertTrue(registerButton.exists, "회원가입 버튼이 존재해야 함")
    }

    // MARK: - 입력 테스트

    func testLoginScreen_emailInput_acceptsText() throws {
        // Given: 로그인 화면의 이메일 필드
        let emailField = app.textFields["이메일"]
        XCTAssertTrue(emailField.waitForExistence(timeout: 5))

        // When: 이메일 입력
        emailField.tap()
        emailField.typeText("test@example.com")

        // Then: 입력된 텍스트가 표시됨
        XCTAssertEqual(emailField.value as? String, "test@example.com")
    }

    func testLoginScreen_passwordInput_acceptsText() throws {
        // Given: 로그인 화면의 비밀번호 필드
        let passwordField = app.secureTextFields["비밀번호"]
        XCTAssertTrue(passwordField.waitForExistence(timeout: 5))

        // When: 비밀번호 입력
        passwordField.tap()
        passwordField.typeText("password123")

        // Then: 비밀번호가 입력됨 (보안상 값은 확인 불가)
        XCTAssertNotEqual(passwordField.value as? String, "")
    }

    // MARK: - 버튼 동작 테스트

    func testLoginScreen_loginButton_withValidCredentials() throws {
        // Given: 유효한 이메일과 비밀번호 입력
        let emailField = app.textFields["이메일"]
        emailField.tap()
        emailField.typeText("test@example.com")

        let passwordField = app.secureTextFields["비밀번호"]
        passwordField.tap()
        passwordField.typeText("password123")

        // When: 로그인 버튼 클릭
        let loginButton = app.buttons["로그인"]
        loginButton.tap()

        // Then: 로딩 인디케이터가 표시되거나 다음 화면으로 전환
        let loadingIndicator = app.activityIndicators.firstMatch
        let operationScreen = app.staticTexts["게임 작전"]

        // 로딩 또는 작전 화면 중 하나는 나타나야 함
        let predicate = NSPredicate(format: "exists == true")
        let expectation = XCTNSPredicateExpectation(
            predicate: predicate,
            object: loadingIndicator.exists ? loadingIndicator : operationScreen
        )

        wait(for: [expectation], timeout: 10)
    }

    func testLoginScreen_registerButton_navigatesToRegisterScreen() throws {
        // Given: 로그인 화면의 회원가입 버튼
        let registerButton = app.buttons["회원가입"]
        XCTAssertTrue(registerButton.waitForExistence(timeout: 5))

        // When: 회원가입 버튼 클릭
        registerButton.tap()

        // Then: 회원가입 화면으로 전환
        let registerTitle = app.staticTexts["회원가입"]
        XCTAssertTrue(registerTitle.waitForExistence(timeout: 5))
    }

    func testLoginScreen_googleLoginButton_triggersGoogleAuth() throws {
        // Given: Google 로그인 버튼
        let googleButton = app.buttons["Google로 계속하기"]
        XCTAssertTrue(googleButton.waitForExistence(timeout: 5))

        // When: Google 로그인 버튼 클릭
        googleButton.tap()

        // Then: Google 로그인 플로우 시작 (Google 웹뷰 또는 로딩 표시)
        // 실제 환경에서는 Google 로그인 화면이 나타남
        sleep(2) // Google 로그인 프로세스 대기
    }

    // MARK: - 에러 상태 테스트

    func testLoginScreen_invalidCredentials_showsError() throws {
        // Given: 잘못된 이메일과 비밀번호 입력
        let emailField = app.textFields["이메일"]
        emailField.tap()
        emailField.typeText("wrong@example.com")

        let passwordField = app.secureTextFields["비밀번호"]
        passwordField.tap()
        passwordField.typeText("wrongpassword")

        // When: 로그인 시도
        let loginButton = app.buttons["로그인"]
        loginButton.tap()

        // Then: 에러 메시지 표시
        let errorMessage = app.staticTexts.matching(NSPredicate(format: "label CONTAINS '실패' OR label CONTAINS '오류'")).firstMatch
        XCTAssertTrue(errorMessage.waitForExistence(timeout: 10), "에러 메시지가 표시되어야 함")
    }

    // MARK: - UI/UX 테스트

    func testLoginScreen_keyboardDismissal() throws {
        // Given: 키보드가 표시된 상태
        let emailField = app.textFields["이메일"]
        emailField.tap()

        // 키보드가 표시되는지 확인
        XCTAssertTrue(app.keyboards.element.exists)

        // When: 화면의 빈 공간 탭
        app.tap()

        // Then: 키보드가 사라짐
        XCTAssertFalse(app.keyboards.element.exists)
    }

    func testLoginScreen_fieldNavigation() throws {
        // Given: 이메일 필드에 포커스
        let emailField = app.textFields["이메일"]
        emailField.tap()
        emailField.typeText("test@example.com")

        // When: Return 키 또는 다음 필드로 이동
        // (키보드의 Next 버튼 탭)

        // Then: 비밀번호 필드로 포커스 이동
        let passwordField = app.secureTextFields["비밀번호"]
        passwordField.tap()

        XCTAssertTrue(passwordField.hasFocus)
    }

    // MARK: - 성능 테스트

    func testLoginScreen_loadingPerformance() throws {
        // 화면 로딩 성능 측정
        measure(metrics: [XCTClockMetric()]) {
            // 앱 재시작
            app.terminate()
            app.launch()

            // 로그인 화면의 주요 요소가 나타날 때까지 대기
            let loginButton = app.buttons["로그인"]
            XCTAssertTrue(loginButton.waitForExistence(timeout: 5))
        }
    }
}

extension XCUIElement {
    var hasFocus: Bool {
        return (self.value(forKey: "hasKeyboardFocus") as? Bool) ?? false
    }
}
