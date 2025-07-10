# Unearth Streaming Application

## 데이터베이스 설정

### 1. MySQL 설치 및 설정

- MySQL 8.0 이상 설치
- MySQL 서비스 시작

### 2. 데이터베이스 및 사용자 생성

MySQL에 root로 접속한 후 다음 명령어를 실행하세요:

```sql
-- MySQL Command Line Client 또는 MySQL Workbench에서 실행
source database_setup.sql
```

또는 수동으로 실행:

```sql
CREATE DATABASE IF NOT EXISTS streaming_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON streaming_db.* TO 'admin'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 환경변수 설정

`src/main/resources/env.properties` 파일에서 데이터베이스 설정을 확인하세요:

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=streaming_db
DB_USERNAME=admin
DB_PASSWORD=1234
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 5. 데이터베이스 연결 테스트

애플리케이션이 실행된 후 다음 URL로 접속하여 데이터베이스 연결을 테스트할 수 있습니다:

- 데이터베이스 연결 테스트: `http://localhost:8080/api/test/db-connection`
- 모든 사용자 조회: `http://localhost:8080/api/test/users`
- 사용자 생성: `POST http://localhost:8080/api/test/create-user`

## 프로젝트 구조

```
src/main/java/com/rhoonart/unearth/
├── config/
│   └── EnvConfig.java          # 환경변수 설정
├── controller/
│   └── TestController.java     # 테스트 컨트롤러
├── entity/
│   └── User.java              # 사용자 엔티티
├── repository/
│   └── UserRepository.java    # 사용자 리포지토리
└── UnearthApplication.java    # 메인 애플리케이션
```

## 기술 스택

- Spring Boot 3.5.3
- Spring Data JPA
- MySQL 8.0
- Lombok
- Gradle
