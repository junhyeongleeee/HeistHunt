# DBeaver로 PostgreSQL 데이터베이스 연결 가이드

## 📋 연결 정보

프로젝트의 데이터베이스 설정:
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `heisthunt`
- **Username**: `junhyeong`
- **Password**: (환경변수 `DATABASE_PASSWORD` 또는 빈 문자열)

## 🔧 DBeaver 설치

### macOS에서 설치

```bash
brew install --cask dbeaver-community
```

또는 [DBeaver 공식 사이트](https://dbeaver.io/download/)에서 다운로드

## 📝 연결 방법

### 1. DBeaver 실행

DBeaver를 실행합니다.

### 2. 새 데이터베이스 연결 생성

1. **상단 메뉴**: `Database` → `New Database Connection` 클릭
   - 또는 왼쪽 상단의 `새 연결` 버튼 클릭
   - 또는 `Cmd + Shift + N` 단축키

2. **데이터베이스 선택**
   - 목록에서 **PostgreSQL** 선택
   - `Next` 클릭

### 3. 연결 정보 입력

**Main 탭에서:**
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `heisthunt`
- **Username**: `junhyeong`
- **Password**: 
  - 환경변수 `DATABASE_PASSWORD`가 설정되어 있다면 해당 값 입력
  - 설정되어 있지 않다면 빈 문자열 (비워두기)
  - `Save password` 체크 (선택사항)

### 4. 드라이버 다운로드 (필요시)

- DBeaver가 PostgreSQL 드라이버를 자동으로 다운로드할 수 있습니다
- 다운로드가 필요하면 `Download` 버튼 클릭

### 5. 연결 테스트

- `Test Connection` 버튼 클릭
- 성공 메시지가 나오면 `Finish` 클릭

### 6. 연결 완료

- 왼쪽 Database Navigator에서 `heisthunt` 데이터베이스가 표시됩니다
- 확장하면 테이블 목록을 볼 수 있습니다

## 📊 데이터베이스 테이블 확인

프로젝트에서 사용하는 테이블들:
- `users` - 사용자 정보
- `refresh_tokens` - 리프레시 토큰
- `rooms` - 게임 방
- `room_participants` - 방 참가자
- `games` - 게임 정보
- `game_players` - 게임 플레이어

## 🔍 유용한 기능

### SQL 쿼리 실행
1. 상단 메뉴: `SQL Editor` → `New SQL Script`
2. SQL 쿼리 작성
3. `Ctrl + Enter` (또는 `Cmd + Enter` on Mac)로 실행

### 데이터 편집
- 테이블을 우클릭 → `Edit Data` 선택
- 직접 데이터를 편집할 수 있습니다

### ER 다이어그램
- 데이터베이스를 우클릭 → `View Diagram` 선택
- 테이블 간 관계를 시각적으로 확인

## ⚠️ 문제 해결

### 연결 실패 시

1. **PostgreSQL이 실행 중인지 확인**
   ```bash
   # PostgreSQL 상태 확인
   brew services list | grep postgresql
   
   # 또는
   pg_isready
   ```

2. **PostgreSQL 시작** (필요시)
   ```bash
   brew services start postgresql
   ```

3. **데이터베이스가 존재하는지 확인**
   ```bash
   psql -U junhyeong -d postgres -c "\l" | grep heisthunt
   ```

4. **데이터베이스 생성** (없는 경우)
   ```bash
   createdb -U junhyeong heisthunt
   ```

### 드라이버 오류

- DBeaver → `Database` → `Driver Manager` → PostgreSQL 드라이버 확인
- 필요시 드라이버 재다운로드

## 📚 참고

- [DBeaver 공식 문서](https://dbeaver.com/docs/)
- [PostgreSQL 연결 가이드](https://dbeaver.com/docs/dbeaver/Connect-to-a-Database/Connect-to-PostgreSQL/)
