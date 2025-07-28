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
- [개발 가이드](#-개발-가이드)
- [로그 관리](#-로그-관리)
- [최신 업데이트](#-최신-업데이트)

---

## 🎯 프로젝트 개요

Unearth는 **음원 스트리밍 플랫폼의 데이터를 자동으로 수집하고 분석하는 시스템**입니다.

### 🎵 주요 특징

- **다중 플랫폼 지원**: Genie, YouTube Music, YouTube, Melon 4개 플랫폼
- **자동화된 크롤링**: 매일 오후 5시 자동 실행
- **웹 기반 관리**: 직관적인 웹 인터페이스로 데이터 관리
- **실패 처리**: 크롤링 실패 시 복구 메커니즘
- **성능 최적화**: 실패 개수 제한으로 UI 성능 향상
- **API 기반 크롤링**: YouTube Data API 활용으로 안정성 향상
- **관리자 기능**: 전용 비밀번호 변경 및 권한 관리
- **로그 관리**: 자동 로그 정리 및 모니터링

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
│   (Thymleaf)    │◄──►│   (Backend)     │◄──►│   (Crawling)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │     MySQL       │    │   YouTube API   │
                       │   Database      │    │   Selenium      │
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
- **YouTube Data API** - YouTube 데이터 수집
- **Selenium** - 웹 브라우저 자동화 (Genie, Melon, YouTube Music)
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
- **관리자 전용 비밀번호 변경**: 별도 페이지에서 관리자 비밀번호 변경

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
- **실패 처리**: 크롤링 실패 시 복구 메커니즘
- **성능 최적화**: 실패 개수 제한으로 UI 성능 향상

### 📊 데이터 분석

- **플랫폼별 통계**: 각 플랫폼별 수집 데이터 분석
- **기간별 조회**: 날짜 범위별 데이터 조회
- **CSV 다운로드**: 수집 데이터 엑셀 파일로 다운로드

---

## 🎵 지원 플랫폼

### 1. 🎼 Genie (지니뮤직)

- **검색 방식**: 곡명/아티스트명 기반 검색
- **수집 데이터**: 조회수, 청취자 수
- **특징**: 국문/영문 다국어 지원
- **크롤링 방식**: 웹 페이지 직접 크롤링 (Selenium)

### 2. 🎵 YouTube Music

- **검색 방식**: 곡명/아티스트명 기반 검색
- **수집 데이터**: 조회수, 업로드 날짜
- **특징**: 로그인 기반 크롤링
- **크롤링 방식**: 쿠키 기반 세션 관리 (Selenium)

### 3. 📺 YouTube

- **검색 방식**: URL 기반 직접 크롤링
- **수집 데이터**: 조회수, 업로드 날짜, 동영상 메타데이터
- **특징**: 공개 동영상 데이터 수집
- **크롤링 방식**: **YouTube Data API v3** 활용
- **장점**: 안정성 향상, 속도 개선, API 할당량 관리

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
vim .env
```

#### 환경변수 설정 (.env) - docker-compose 파일과 동일 한 위치에 존재

```env
# MySQL 데이터베이스 설정
MYSQL_ROOT_PASSWORD=example-root-password
MYSQL_DATABASE=example_db
MYSQL_USER=example_user
MYSQL_PASSWORD=example-password

# Django 설정
DJANGO_SECRET_KEY=django-insecure-example-secret-key
YOUTUBE_MUSIC_ID=example@gmail.com
YOUTUBE_MUSIC_PASSWORD=example-password
MELON_API_URL=https://example.com/api/song/info.json
DJANGO_SETTING_MODULE=config.settings
PYTHONPATH=/app/example_project

# 데이터베이스 호스트 설정 (Docker 네트워크 내에서)
DB_HOST=example_db_host
DJANGO_DB_HOST=example_db_host

# Spring Boot 애플리케이션 설정
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
SUPERADMIN_USERNAME=example_admin
SUPERADMIN_PASSWORD=example_admin_password

# SLACK MESSAGE WEBHOOK URL
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/EXAMPLE/EXAMPLE/EXAMPLE_KEY

# YouTube Data API
YOUTUBE_API_KEY=AIzaSyEXAMPLE-KEY-FOR-YOUTUBE-API
```

### 2️⃣ Docker Compose 실행

```bash
# 전체 시스템 실행
docker-compose up -d
```

### 3️⃣ 업데이트 적용

✅ 1. 로컬에서 Docker 이미지 빌드

```
docker build -t example-user/example-app:latest .
```

Dockerfile을 읽어 이미지 생성

태그는 example-user/example-app:latest로 지정

✅ 2. Docker Hub로 이미지 푸시

```
docker push example-user/example-app:latest
```

위에서 빌드한 이미지를 Docker Hub 저장소로 업로드

✅ 3. 서버에서 최신 이미지 Pull

```
docker pull example-user/example-app:latest
```

서버에서 최신 이미지를 받아서 준비

✅ 4. 컨테이너 재시작 (예시)
docker compose down
docker compose up -d

기존 컨테이너를 정지 및 삭제한 후, 최신 이미지를 기반으로 재실행

## 📊 데이터베이스 구조

### 🗄️ 주요 테이블

#### users (사용자)

```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('SUPER_ADMIN', 'ADMIN', 'RIGHT_HOLDER') NOT NULL,
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

#### crawling_period (크롤링 기간)

```sql
CREATE TABLE crawling_period (
    id VARCHAR(36) PRIMARY KEY,
    song_id VARCHAR(36) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    youtube_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (song_id) REFERENCES song_info(id)
);
```

#### youtube_video_viewcount (YouTube 조회수)

```sql
CREATE TABLE youtube_video_viewcount (
    id VARCHAR(36) PRIMARY KEY,
    crawling_period_id VARCHAR(36) NOT NULL,
    view_count BIGINT,
    crawling_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (crawling_period_id) REFERENCES crawling_period(id)
);
```

#### crawling_failure (크롤링 실패)

```sql
CREATE TABLE crawling_failure (
    id VARCHAR(36) PRIMARY KEY,
    song_info_id VARCHAR(36) NOT NULL,
    platform_type ENUM('GENIE', 'YOUTUBE_MUSIC', 'YOUTUBE', 'MELON') NOT NULL,
    error_message TEXT,
    failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (song_info_id) REFERENCES song_info(id)
);
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
│   │   ├── GlobalControllerAdvice.java # 전역 컨트롤러 어드바이스
│   │   └── exception/             # 예외 처리
│   │       ├── BaseException.java # 기본 예외
│   │       └── GlobalExceptionHandler.java # 전역 예외 처리
│   ├── user/                      # 사용자 관리
│   │   ├── controller/            # 컨트롤러
│   │   │   ├── LoginController.java # 로그인 컨트롤러
│   │   │   └── AdminPasswordController.java # 관리자 비밀번호 변경
│   │   ├── service/               # 서비스
│   │   │   ├── LoginService.java  # 로그인 서비스
│   │   │   └── AdminPasswordChangeService.java # 비밀번호 변경 서비스
│   │   ├── repository/            # 리포지토리
│   │   ├── entity/                # 엔티티
│   │   └── dto/                   # DTO
│   ├── right_holder/              # 권리자 관리
│   ├── song/                      # 곡 정보 관리
│   └── crawling/                  # 크롤링 관리
│       ├── controller/            # 크롤링 컨트롤러
│       ├── service/               # 크롤링 서비스
│       ├── repository/            # 크롤링 리포지토리
│       └── entity/                # 크롤링 엔티티
├── src/main/resources/
│   ├── templates/                 # Thymeleaf 템플릿
│   │   ├── admin/                 # 관리자 페이지
│   │   │   └── password-change.html # 비밀번호 변경 페이지
│   │   ├── user/                  # 사용자 페이지
│   │   │   └── login.html         # 로그인 페이지
│   │   └── common/                # 공통 템플릿
│   │       ├── header.html        # 헤더
│   │       └── sidebar.html       # 사이드바
│   └── static/                    # 정적 리소스
│       └── css/                   # CSS 파일
│           ├── base.css           # 기본 스타일
│           ├── login.css          # 로그인 스타일
│           └── admin_password_change.css # 관리자 비밀번호 변경 스타일
├── streaming_crawling/            # Python 크롤링 시스템
│   ├── crawling/
│   │   ├── service/               # 플랫폼별 크롤러
│   │   │   ├── genie/             # Genie 크롤러
│   │   │   ├── melon/             # Melon 크롤러
│   │   │   ├── youtube/           # YouTube 크롤러 (API 기반)
│   │   │   │   ├── youtube_api_service.py # YouTube API 서비스
│   │   │   │   ├── youtube_main.py # YouTube 메인 로직
│   │   │   │   └── id_extractor.py # YouTube ID 추출
│   │   │   └── youtube_music/     # YouTube Music 크롤러
│   │   ├── models/                # Django 모델
│   │   ├── repository/            # 데이터 저장소
│   │   └── managers/              # 크롤링 매니저
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

## 📋 로그 관리

### 📁 로그 파일 구조

크롤링 시스템은 다음과 같은 로그 파일들을 생성합니다:

```
streaming_crawling
└── logs/                        # 시스템 로그
    ├── crawling_{date}_{time}.log               # 데이터베이스 연결 로그
    └── single_crawling_{date}{time}_{songId}.log              # 스케줄러 로그
```

### 🧹 자동 로그 정리

시스템은 **10일에 한 번씩 자동으로 10일보다 이전의 로그 데이터를 삭제**합니다.

#### 로그 정리 스케줄

- **실행 주기**: 매달 1일, 10일, 20일, 30일 | 00시 01분
- **보관 기간**: 10일
- **정리 대상**: 모든 크롤링 로그 파일
- **정리 방식**: 파일 삭제

## 🆕 최신 업데이트

### 🔄 v2.1.0 (2024-07-28)

#### ✨ 새로운 기능

- **YouTube Data API 통합**: Selenium 기반 크롤링에서 YouTube Data API v3로 전환
- **관리자 비밀번호 변경**: 전용 페이지에서 관리자 비밀번호 변경 기능
- **로그인 에러 처리 개선**: 알림창을 통한 사용자 친화적 에러 메시지
- **크롤링 실패 개수 최적화**: UI 성능 향상을 위한 실패 개수 제한 (10+ 표시)
- **자동 로그 정리**: 7일마다 자동으로 오래된 로그 파일 정리

#### 🔧 기술적 개선

- **YouTube 크롤링 안정성**: API 기반으로 전환하여 안정성 및 속도 향상
- **데이터베이스 스키마 개선**: UUID 기반 ID 시스템으로 통일
- **예외 처리 강화**: 전역 예외 처리 및 사용자 친화적 메시지
- **CSS 모듈화**: 페이지별 독립적인 CSS 파일 구조

#### 🐛 버그 수정

- **로그인 실패 처리**: HTTP 401 상태 코드 처리 개선
- **YouTube 조회수 표시**: 모든 날짜의 조회수 데이터 표시
- **크롤링 실패 감지**: 실패/미수집 상태 구분 개선
- **데이터베이스 타입 불일치**: Django-Spring 간 ID 타입 통일

#### 📊 성능 최적화

- **UI 응답성**: 크롤링 실패 개수 쿼리 최적화
- **메모리 사용량**: 불필요한 Selenium 의존성 제거
- **네트워크 효율성**: YouTube API 배치 처리 구현
- **디스크 공간**: 자동 로그 정리로 디스크 공간 절약

### 🔄 v2.0.0 (2024-07-15)

#### ✨ 주요 기능

- **다중 플랫폼 지원**: Genie, YouTube Music, YouTube, Melon
- **자동 스케줄링**: 매일 오후 5시 자동 크롤링
- **웹 기반 관리**: 직관적인 관리 인터페이스
- **CSV 대량 등록**: 엑셀 파일을 통한 곡 정보 일괄 등록

---

<div align="center">

**Made with ❤️ by the Unearth Team**

[![GitHub stars](https://img.shields.io/github/stars/your-username/unearth?style=social)](https://github.com/your-username/unearth/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/your-username/unearth?style=social)](https://github.com/your-username/unearth/network)
[![GitHub issues](https://img.shields.io/github/issues/your-username/unearth)](https://github.com/your-username/unearth/issues)

</div>
