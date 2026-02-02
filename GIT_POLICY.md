# Git Policy - HeistHunt

HeistHunt 프로젝트의 형상 관리 정책

---

## Branch Strategy: Git Flow

```
main ─────────────────────────────────────────────────────────► (Production)
  │
  └── develop ────────────────────────────────────────────────► (Development)
        │
        ├── feature/login ──────┐
        │                       ▼ (merge)
        ├── feature/map ────────┐
        │                       ▼ (merge)
        ├── feature/game-logic ─┘
        │
        └── release/1.0.0 ────► main (tag: v1.0.0)
                              │
                              └── hotfix/critical-bug ──► main & develop
```

### Branch Types

| Branch | 용도 | 생성 위치 | 병합 대상 |
|--------|------|----------|----------|
| `main` | 프로덕션 릴리즈 | - | - |
| `develop` | 개발 통합 브랜치 | main | main (release 통해) |
| `feature/*` | 기능 개발 | develop | develop |
| `release/*` | 릴리즈 준비 | develop | main & develop |
| `hotfix/*` | 긴급 버그 수정 | main | main & develop |

---

## Branch Naming Convention

### Feature Branch
```
feature/<기능명>
feature/<이슈번호>-<기능명>
```

**Examples:**
```
feature/login
feature/google-map-integration
feature/42-user-authentication
feature/real-time-location
```

### Release Branch
```
release/<버전>
```

**Examples:**
```
release/1.0.0
release/1.1.0
```

### Hotfix Branch
```
hotfix/<버그명>
hotfix/<이슈번호>-<버그명>
```

**Examples:**
```
hotfix/crash-on-startup
hotfix/99-login-failure
```

---

## Commit Message Convention

### Format
```
<type>: "<title>"
- <description>
- <description>
- ...
```

### Types

| Type | 설명 | Example |
|------|------|---------|
| `feat` | 새로운 기능 추가 | feat: "Add login screen" |
| `fix` | 버그 수정 | fix: "Resolve crash on map load" |
| `refactor` | 코드 리팩토링 (기능 변경 없음) | refactor: "Extract location service" |
| `style` | 코드 포맷팅, 세미콜론 누락 등 | style: "Apply ktlint formatting" |
| `docs` | 문서 수정 | docs: "Update README" |
| `test` | 테스트 코드 추가/수정 | test: "Add login unit tests" |
| `chore` | 빌드, 설정 파일 수정 | chore: "Update gradle dependencies" |
| `perf` | 성능 개선 | perf: "Optimize image loading" |
| `ci` | CI/CD 설정 변경 | ci: "Add GitHub Actions workflow" |

### Examples

**단일 변경:**
```
feat: "Add Google Maps integration"
- Implement MapScreen composable
- Add location permission handling
```

**다중 변경:**
```
refactor: "Restructure navigation system"
- Extract NavGraph to separate file
- Create sealed class for Screen routes
- Add bottom navigation bar
```

**버그 수정:**
```
fix: "Resolve iOS simulator crash"
- Add CADisableMinimumFrameDurationOnPhone to Info.plist
- Update minimum iOS deployment target to 15.0
```

---

## Workflow

### 1. 기능 개발 (Feature)

```bash
# 1. develop 브랜치에서 feature 브랜치 생성
git checkout develop
git pull origin develop
git checkout -b feature/login

# 2. 작업 및 커밋 (가능한 세부적으로)
git add <files>
git commit -m 'feat: "Add login UI"
- Create LoginScreen composable
- Add email/password input fields'

git commit -m 'feat: "Implement login logic"
- Add LoginViewModel
- Connect to authentication service'

# 3. develop에 병합
git checkout develop
git pull origin develop
git merge feature/login

# 4. 원격에 push
git push origin develop

# 5. feature 브랜치 삭제
git branch -d feature/login
```

### 2. 릴리즈 (Release)

```bash
# 1. develop에서 release 브랜치 생성
git checkout develop
git checkout -b release/1.0.0

# 2. 버전 번호 업데이트, 버그 수정 등
git commit -m 'chore: "Bump version to 1.0.0"'

# 3. main에 병합 및 태그
git checkout main
git merge release/1.0.0
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin main --tags

# 4. develop에도 병합
git checkout develop
git merge release/1.0.0
git push origin develop

# 5. release 브랜치 삭제
git branch -d release/1.0.0
```

### 3. 긴급 수정 (Hotfix)

```bash
# 1. main에서 hotfix 브랜치 생성
git checkout main
git checkout -b hotfix/critical-crash

# 2. 버그 수정
git commit -m 'fix: "Resolve critical crash on app launch"
- Fix null pointer exception in MainActivity'

# 3. main에 병합 및 태그
git checkout main
git merge hotfix/critical-crash
git tag -a v1.0.1 -m "Hotfix: critical crash"
git push origin main --tags

# 4. develop에도 병합
git checkout develop
git merge hotfix/critical-crash
git push origin develop

# 5. hotfix 브랜치 삭제
git branch -d hotfix/critical-crash
```

---

## Commit Guidelines

### DO's
- 하나의 커밋은 하나의 논리적 변경만 포함
- 커밋 메시지는 명확하고 구체적으로 작성
- 관련 있는 파일들만 함께 커밋
- 자주, 작은 단위로 커밋

### DON'Ts
- 여러 기능을 하나의 커밋에 포함하지 않기
- "WIP", "fix", "update" 같은 모호한 메시지 사용 금지
- 빌드가 깨진 상태로 커밋하지 않기
- 민감한 정보 (API 키, 비밀번호) 커밋 금지

---

## Quick Reference

```bash
# 현재 브랜치 확인
git branch

# 브랜치 생성 및 이동
git checkout -b feature/new-feature

# 변경사항 확인
git status
git diff

# 스테이징 및 커밋
git add <file>
git commit -m 'type: "title"
- description'

# 원격 저장소 동기화
git pull origin <branch>
git push origin <branch>

# 브랜치 병합
git checkout develop
git merge feature/new-feature

# 브랜치 삭제
git branch -d feature/new-feature      # 로컬
git push origin -d feature/new-feature # 원격
```
