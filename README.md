# 🎵 Unearth - 음원 스트리밍 데이터 크롤링 시스템

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-green?style=for-the-badge&logo=spring-boot)
![Python](https://img.shields.io/badge/Python-3.x-blue?style=for-the-badge&logo=python)
![Django](https://img.shields.io/badge/Django-4.2.21-darkgreen?style=for-the-badge&logo=django)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![Docker](https://img.shields.io/badge/Docker-✓-blue?style=for-the-badge&logo=docker)

**음원 스트리밍 플랫폼의 데이터를 자동으로 수집하고 분석하는 통합 관리 시스템**

</div>

---

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [시스템 아키텍처](#️-시스템-아키텍처)
- [주요 기능](#-주요-기능)
- [지원 플랫폼](#-지원-플랫폼)
- [설치 및 실행](#️-설치-및-실행)
- [데이터베이스 구조](#-데이터베이스-구조)
- [API 문서](#-api-문서)
- [개발 가이드](#-개발-가이드)
- [문제 해결](#-문제-해결)

---

## 🎯 프로젝트 개요

Unearth는 **음원 스트리밍 플랫폼의 데이터를 자동으로 수집하고 분석하는 시스템**입니다.

### 🎵 주요 특징

- **다중 플랫폼 지원**: Genie, YouTube Music, YouTube, Melon 4개 플랫폼
- **자동화된 크롤링**: 매일 오후 5시 자동 실행
- **웹 기반 관리**: 직관적인 웹 인터페이스로 데이터 관리
- **실시간 모니터링**: 크롤링 상태 및 결과 실시간 확인
- **데이터 검증**: 곡명/아티스트명 매칭 검증 시스템

### 🏢 비즈니스 가치

- **음원 권리자**: 수익 분석 및 시장 동향 파악
- **음악 기획사**: 아티스트 인기도 및 성과 측정
- **연구자**: 음악 산업 데이터 분석 및 연구

---

## 🏗️ 시스템 아키텍처

### 📊 전체 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Browser   │    │  Spring Boot    │    │   Python        │
│   (Frontend)    │◄──►│   (Backend)     │◄──►│   (Crawling)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │     MySQL       │    │   Selenium      │
                       │   Database      │    │   WebDriver     │
                       └─────────────────┘    └─────────────────┘
```

### 🔧 기술 스택

#### 백엔드 (Spring Boot)

- **Java 17** + **Spring Boot 3.5.3**
- **Spring Security** - 인증/인가
- **Spring Data JPA** - 데이터 접근
- **Thymeleaf** - 서버사이드 템플릿
- **MySQL 8.0** - 데이터베이스
- **Gradle** - 빌드 도구

#### 크롤링 시스템 (Python/Django)

- **Python 3.x** + **Django 4.2.21**
- **Selenium** - 웹 브라우저 자동화
- **BeautifulSoup4** - HTML 파싱
- **Pandas** - 데이터 처리
- **mysqlclient** - MySQL 연결

#### 인프라

- **Docker** - 컨테이너화
- **Docker Compose** - 멀티 컨테이너 관리
- **Nginx** - 웹 서버 (선택사항)

---

## ✨ 주요 기능

### 🔐 사용자 관리

- **로그인/로그아웃**: 세션 기반 인증
- **권한 관리**: 관리자/일반 사용자 구분
- **비밀번호 변경**: 보안 강화

### 👥 권리자 관리

- **권리자 등록**: 음원 권리자 정보 등록
- **권리자 조회**: 등록된 권리자 목록 확인
- **권리자 수정**: 권리자 정보 업데이트
- **계약 기간 관리**: 계약 시작/종료일 관리

### 🎵 곡 정보 관리

- **단일 곡 등록**: 개별 곡 정보 등록
- **CSV 대량 등록**: 엑셀 파일을 통한 일괄 등록
- **곡 정보 수정**: 등록된 곡 정보 업데이트
- **중복 검사**: 동일 곡 중복 등록 방지

### 🕷️ 크롤링 관리

- **자동 스케줄링**: 매일 오후 5시 자동 실행
- **수동 실행**: 즉시 크롤링 실행
- **실시간 모니터링**: 크롤링 진행 상황 확인
- **실패 처리**: 크롤링 실패 시 복구 메커니즘

### 📊 데이터 분석

- **플랫폼별 통계**: 각 플랫폼별 수집 데이터 분석
- **기간별 조회**: 날짜 범위별 데이터 조회
- **CSV 다운로드**: 수집 데이터 엑셀 파일로 다운로드
- **데이터 시각화**: 차트를 통한 데이터 시각화

---

## 🎵 지원 플랫폼

### 1. 🎼 Genie (지니뮤직)

- **검색 방식**: 곡명/아티스트명 기반 검색
- **수집 데이터**: 조회수, 청취자 수
- **특징**: 국문/영문 다국어 지원
- **크롤링 방식**: 웹 페이지 직접 크롤링

### 2. 🎵 YouTube Music

- **검색 방식**: 곡명/아티스트명 기반 검색
- **수집 데이터**: 조회수, 업로드 날짜
- **특징**: 로그인 기반 크롤링
- **크롤링 방식**: 쿠키 기반 세션 관리

### 3. 📺 YouTube

- **검색 방식**: URL 기반 직접 크롤링
- **수집 데이터**: 조회수, 업로드 날짜, 동영상 메타데이터
- **특징**: 공개 동영상 데이터 수집
- **크롤링 방식**: 동영상 페이지 직접 접근

### 4. 🍈 Melon (멜론)

- **검색 방식**: 곡 ID 기반 크롤링
- **수집 데이터**: 조회수, 청취자 수
- **특징**: API 기반 데이터 수집
- **크롤링 방식**: Melon API 활용

---

## 🚀 설치 및 실행

### 📋 사전 요구사항

- **Docker** 20.10+
- **Docker Compose** 2.0+
- **Java 17** (개발용)
- **Python 3.8+** (개발용)

### 1️⃣ 환경 설정

```bash
# 프로젝트 클론
git clone https://github.com/your-username/unearth.git
cd unearth

# 환경변수 파일 생성
cp .env.example .env
```

#### 환경변수 설정 (.env)

```env
# MySQL 설정
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=streaming_db
MYSQL_USER=admin
MYSQL_PASSWORD=1234
DB_HOST=localhost

# Django 설정
DJANGO_SECRET_KEY=your_django_secret_key_here

# Melon API 설정
MELON_API_URL=https://www.melon.com/api

# YouTube Music 로그인 정보
YOUTUBE_MUSIC_ID=your_youtube_music_id
YOUTUBE_MUSIC_PASSWORD=your_youtube_music_password

# 로깅 설정
LOG_LEVEL=INFO
```

### 2️⃣ Docker Compose 실행 (권장)

```bash
# 전체 시스템 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 특정 서비스 로그 확인
docker-compose logs -f spring-app
docker-compose logs -f python-crawler
```

### 3️⃣ 개별 실행 (개발용)

#### Spring Boot 실행

```bash
# Gradle 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/unearth-0.0.1-SNAPSHOT.jar
```

#### Python 크롤링 실행

```bash
# Python 환경 설정
cd streaming_crawling
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# Django 서버 실행
python manage.py runserver

# 크롤링 실행
python manage.py crawl_one_song "아티스트명" "곡명"
```

### 4️⃣ 초기 설정

```bash
# 데이터베이스 마이그레이션
docker-compose exec spring-app ./gradlew flywayMigrate

# Django 마이그레이션
docker-compose exec python-crawler python manage.py migrate

# 관리자 계정 생성
docker-compose exec spring-app ./gradlew createSuperAdmin
```

---

## 📊 데이터베이스 구조

### 🗄️ 주요 테이블

#### users (사용자)

```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### right_holders (권리자)

```sql
CREATE TABLE right_holders (
    id VARCHAR(36) PRIMARY KEY,
    holder_name VARCHAR(255) NOT NULL,
    holder_type ENUM('COMPANY', 'INDIVIDUAL') NOT NULL,
    contract_start_date DATE,
    contract_end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### song_info (곡 정보)

```sql
CREATE TABLE song_info (
    id VARCHAR(36) PRIMARY KEY,
    artist_ko VARCHAR(255) NOT NULL,
    artist_en VARCHAR(255) NOT NULL,
    title_ko VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    album_ko VARCHAR(255),
    album_en VARCHAR(255),
    youtube_url VARCHAR(500),
    melon_song_id VARCHAR(100) UNIQUE,
    right_holder_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (right_holder_id) REFERENCES right_holders(id),
    UNIQUE KEY uk_artist_title (artist_ko, title_ko)
);
```

#### crawling_data (크롤링 데이터)

```sql
CREATE TABLE crawling_data (
    id VARCHAR(36) PRIMARY KEY,
    song_info_id VARCHAR(36) NOT NULL,
    platform_type ENUM('GENIE', 'YOUTUBE_MUSIC', 'YOUTUBE', 'MELON') NOT NULL,
    view_count BIGINT,
    listener_count BIGINT,
    crawling_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (song_info_id) REFERENCES song_info(id)
);
```

---

## 🔧 API 문서

### 🔐 인증 API

#### 로그인

```http
POST /user/login
Content-Type: application/x-www-form-urlencoded

username=admin&password=password
```

#### 로그아웃

```http
POST /user/logout
```

### 👥 권리자 API

#### 권리자 목록 조회

```http
GET /right-holder/list?page=1&size=10
```

#### 권리자 등록

```http
POST /right-holder/register
Content-Type: application/json

{
    "holderName": "권리자명",
    "holderType": "COMPANY",
    "contractStartDate": "2024-01-01",
    "contractEndDate": "2024-12-31"
}
```

### 🎵 곡 정보 API

#### 곡 목록 조회

```http
GET /song/list?page=1&size=10&artistKo=아티스트명
```

#### 곡 등록

```http
POST /song/register
Content-Type: application/json

{
    "artistKo": "아티스트명",
    "titleKo": "곡명",
    "rightHolderName": "권리자명"
}
```

#### CSV 대량 등록

```http
POST /song/bulk-register
Content-Type: multipart/form-data

file: [CSV 파일]
```

### 🕷️ 크롤링 API

#### 크롤링 실행

```http
POST /crawling/execute
Content-Type: application/json

{
    "platformType": "GENIE",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31"
}
```

#### 크롤링 데이터 조회

```http
GET /crawling/data?platformType=GENIE&startDate=2024-01-01&endDate=2024-01-31
```

---

## 🛠️ 개발 가이드

### 📁 프로젝트 구조

```
unearth/
├── src/main/java/com/rhoonart/unearth/
│   ├── config/                    # Spring 설정
│   │   ├── SecurityConfig.java    # 보안 설정
│   │   ├── JpaConfig.java         # JPA 설정
│   │   └── SchedulerConfig.java   # 스케줄러 설정
│   ├── common/                    # 공통 유틸리티
│   │   ├── CommonResponse.java    # 공통 응답 객체
│   │   ├── ResponseCode.java      # 응답 코드
│   │   └── exception/             # 예외 처리
│   ├── user/                      # 사용자 관리
│   │   ├── controller/            # 컨트롤러
│   │   ├── service/               # 서비스
│   │   ├── repository/            # 리포지토리
│   │   └── entity/                # 엔티티
│   ├── right_holder/              # 권리자 관리
│   ├── song/                      # 곡 정보 관리
│   └── crawling/                  # 크롤링 관리
├── streaming_crawling/            # Python 크롤링 시스템
│   ├── crawling_view/
│   │   ├── view/                  # 플랫폼별 크롤러
│   │   │   ├── genie/             # Genie 크롤러
│   │   │   ├── melon/             # Melon 크롤러
│   │   │   ├── youtube/           # YouTube 크롤러
│   │   │   └── youtube_music/     # YouTube Music 크롤러
│   │   ├── data/                  # 데이터 저장
│   │   ├── utils/                 # 유틸리티
│   │   └── controller/            # 크롤링 컨트롤러
│   ├── requirements.txt           # Python 의존성
│   └── manage.py                  # Django 관리
├── docs/                          # 문서 및 스크린샷
├── docker-compose.yml             # Docker 설정
├── Dockerfile                     # Docker 이미지
├── build.gradle                   # Gradle 설정
└── README.md                      # 프로젝트 문서
```

### 🔧 개발 환경 설정

#### IntelliJ IDEA 설정

1. **프로젝트 열기**: `File` → `Open` → 프로젝트 폴더 선택
2. **Gradle 동기화**: `View` → `Tool Windows` → `Gradle`
3. **Java 17 설정**: `File` → `Project Structure` → `Project SDK`
4. **Run Configuration**: `Run` → `Edit Configurations` → `Spring Boot`

#### VS Code 설정

1. **확장 프로그램 설치**:

   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Python
   - Docker

2. **settings.json**:

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic"
}
```

### 🧪 테스트

#### Spring Boot 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests SongServiceTest

# 테스트 커버리지 확인
./gradlew jacocoTestReport
```

#### Python 테스트

```bash
# Django 테스트 실행
cd streaming_crawling
python manage.py test

# 특정 테스트 실행
python manage.py test crawling_view.test.test_platform_crawlers
```

### 📝 코딩 컨벤션

#### Java 코딩 컨벤션

- **클래스명**: PascalCase (예: `SongInfo`)
- **메서드명**: camelCase (예: `getSongInfo()`)
- **상수명**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`)
- **패키지명**: 소문자 (예: `com.rhoonart.unearth`)

#### Python 코딩 컨벤션

- **클래스명**: PascalCase (예: `MelonCrawler`)
- **함수명**: snake_case (예: `extract_song_info()`)
- **변수명**: snake_case (예: `song_title`)
- **상수명**: UPPER_SNAKE_CASE (예: `MAX_RETRY_COUNT`)

---

## 🚨 문제 해결

### 🔧 일반적인 문제

#### 1. Docker 실행 오류

```bash
# Docker 서비스 상태 확인
docker --version
docker-compose --version

# 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs [서비스명]
```

#### 2. 데이터베이스 연결 오류

```bash
# MySQL 컨테이너 상태 확인
docker-compose exec mysql mysql -u root -p

# 데이터베이스 생성 확인
SHOW DATABASES;
USE streaming_db;
SHOW TABLES;
```

#### 3. 크롤링 실패

```bash
# Python 환경 확인
cd streaming_crawling
python --version
pip list

# Selenium 드라이버 확인
python -c "from selenium import webdriver; print('Selenium OK')"
```

### 📊 로그 분석

#### Spring Boot 로그

```bash
# 애플리케이션 로그 확인
docker-compose logs -f spring-app

# 특정 로그 필터링
docker-compose logs spring-app | grep "ERROR"
```

#### Python 크롤링 로그

```bash
# 크롤링 로그 확인
docker-compose logs -f python-crawler

# 로그 파일 확인
docker-compose exec python-crawler ls -la /app/logs/
```

### 🔄 성능 최적화

#### 데이터베이스 최적화

```sql
-- 인덱스 확인
SHOW INDEX FROM song_info;

-- 쿼리 성능 분석
EXPLAIN SELECT * FROM song_info WHERE artist_ko = '아티스트명';
```

#### 크롤링 성능 최적화

```python
# 병렬 크롤링 설정
MAX_WORKERS = 4
TIMEOUT = 30

# 메모리 사용량 모니터링
import psutil
print(f"Memory usage: {psutil.virtual_memory().percent}%")
```

---

## 📈 모니터링 및 유지보수

### 📊 모니터링 지표

- **크롤링 성공률**: 95% 이상 유지
- **응답 시간**: 평균 2초 이하
- **데이터 정확도**: 99% 이상
- **시스템 가용성**: 99.9% 이상

### 🔄 정기 유지보수

#### 일일 점검

- [ ] 크롤링 성공률 확인
- [ ] 에러 로그 확인
- [ ] 데이터베이스 연결 상태 확인

#### 주간 점검

- [ ] 시스템 성능 분석
- [ ] 데이터 정확도 검증
- [ ] 보안 업데이트 확인

#### 월간 점검

- [ ] 전체 시스템 백업
- [ ] 성능 최적화
- [ ] 새로운 플랫폼 검토

---

## 🤝 기여하기

### 📝 기여 방법

1. **Fork** 프로젝트
2. **Feature Branch** 생성 (`git checkout -b feature/AmazingFeature`)
3. **Commit** 변경사항 (`git commit -m 'Add some AmazingFeature'`)
4. **Push** 브랜치 (`git push origin feature/AmazingFeature`)
5. **Pull Request** 생성

### 🐛 버그 리포트

버그를 발견하셨다면 [Issues](https://github.com/your-username/unearth/issues)에 등록해 주세요.

### 💡 기능 제안

새로운 기능 제안은 [Discussions](https://github.com/your-username/unearth/discussions)에서 논의해 주세요.

---

## 📄 라이선스

이 프로젝트는 **MIT License** 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

## 📞 연락처

- **프로젝트 관리자**: [your-email@example.com](mailto:your-email@example.com)
- **기술 지원**: [tech-support@example.com](mailto:tech-support@example.com)
- **프로젝트 홈페이지**: [https://github.com/your-username/unearth](https://github.com/your-username/unearth)

---

<div align="center">

**Made with ❤️ by the Unearth Team**

[![GitHub stars](https://img.shields.io/github/stars/your-username/unearth?style=social)](https://github.com/your-username/unearth/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/your-username/unearth?style=social)](https://github.com/your-username/unearth/network)
[![GitHub issues](https://img.shields.io/github/issues/your-username/unearth)](https://github.com/your-username/unearth/issues)

</div>
