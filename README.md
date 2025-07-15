# Unearth - 음원 스트리밍 데이터 크롤링 시스템

## 📋 프로젝트 개요

Unearth는 음원 스트리밍 플랫폼의 데이터를 자동으로 수집하고 분석하는 시스템입니다. Spring Boot 기반의 웹 관리 시스템과 Python 기반의 크롤링 시스템으로 구성되어 있습니다.

## 🏗️ 시스템 아키텍처

### 백엔드 (Spring Boot)

- **Java 17** + **Spring Boot 3.5.3**
- **MySQL 8.0** 데이터베이스
- **Spring Security** 인증/인가
- **Thymeleaf** 템플릿 엔진
- **JPA/Hibernate** ORM

### 크롤링 시스템 (Python/Django)

- **Python 3.x** + **Django 4.2.21**
- **Selenium** 웹 크롤링
- **BeautifulSoup4** HTML 파싱
- **MySQL** 데이터베이스 연동

## 🎵 지원 플랫폼

### 1. Genie (지니뮤직)

- 곡명/아티스트명 기반 검색
- 조회수, 청취자 수 수집
- 국문/영문 다국어 지원

### 2. YouTube Music

- 로그인 기반 크롤링
- 곡명/아티스트명 기반 검색
- 조회수 수집
- 쿠키 기반 세션 관리

### 3. YouTube

- URL 기반 직접 크롤링
- 조회수, 업로드 날짜 수집
- 동영상 메타데이터 추출

### 4. Melon (멜론)

- API 기반 데이터 수집
- 곡 ID 기반 크롤링
- 조회수, 청취자 수 수집

## 🚀 주요 기능

### 웹 관리 시스템

- **사용자 관리**: 로그인/로그아웃, 권한 관리
- **권리자 관리**: 음원 권리자 등록 및 관리
- **곡 정보 관리**: 곡 등록, 수정, 삭제
- **크롤링 데이터 조회**: 수집된 데이터 확인
- **크롤링 스케줄 관리**: 자동 크롤링 설정

### 크롤링 시스템

- **다중 플랫폼 지원**: 4개 주요 스트리밍 플랫폼
- **자동 스케줄링**: 매일 오후 5시 자동 실행
- **데이터 검증**: 곡명/아티스트명 매칭 검증
- **CSV/DB 저장**: 수집 데이터 저장
- **에러 처리**: 크롤링 실패 시 복구 메커니즘

## 🛠️ 설치 및 실행

### 1. 환경 설정

```bash
# 환경변수 파일 생성
cp .env.example .env

# 환경변수 설정
MYSQL_ROOT_PASSWORD=your_password
MYSQL_DATABASE=streaming_db
MYSQL_USER=admin
MYSQL_PASSWORD=1234
DB_HOST=localhost
DJANGO_SECRET_KEY=your_secret_key
MELON_API_URL=your_melon_api_url
YOUTUBE_MUSIC_ID=your_youtube_music_id
YOUTUBE_MUSIC_PASSWORD=your_youtube_music_password
```

### 2. Docker Compose 실행

```bash
# 전체 시스템 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app
```

### 3. 개별 실행 (개발용)

```bash
# Spring Boot 실행
./gradlew bootRun

# Python 크롤링 실행
cd streaming_crawling
python manage.py runserver
```

## 📊 데이터베이스 구조

### 주요 테이블

- **users**: 사용자 정보
- **right_holders**: 권리자 정보
- **song_info**: 곡 기본 정보
- **crawling_period**: 크롤링 기간 설정
- **crawling_data**: 크롤링 결과 데이터

## 🔧 크롤링 설정

### 스케줄 설정

- **자동 크롤링**: 매일 오후 5시 실행
- **수동 크롤링**: 관리자 페이지에서 즉시 실행 가능
- **중복 실행 방지**: 크롤링 중복 실행 방지 메커니즘

### 플랫폼별 설정

- **Genie**: 기본 설정으로 즉시 사용 가능
- **YouTube Music**: 로그인 정보 필요
- **YouTube**: URL 등록 필요
- **Melon**: API URL 설정 필요

## 📁 프로젝트 구조

```
unearth/
├── src/main/java/com/rhoonart/unearth/
│   ├── config/           # Spring 설정
│   ├── crawling/         # 크롤링 관리
│   ├── right_holder/     # 권리자 관리
│   ├── song/            # 곡 정보 관리
│   ├── user/            # 사용자 관리
│   └── common/          # 공통 유틸리티
├── streaming_crawling/   # Python 크롤링 시스템
│   ├── crawling_view/
│   │   ├── view/        # 플랫폼별 크롤러
│   │   ├── data/        # 데이터 저장
│   │   ├── utils/       # 유틸리티
│   │   └── controller/  # 크롤링 컨트롤러
│   └── requirements.txt
├── docs/                # 문서 및 스크린샷
├── docker-compose.yml   # Docker 설정
└── build.gradle        # Gradle 설정
```

## 🔐 보안

- **Spring Security** 기반 인증/인가
- **세션 기반** 로그인 관리
- **권한별** 접근 제어
- **환경변수** 기반 민감 정보 관리

## 📈 모니터링

- **로그 시스템**: 상세한 크롤링 로그
- **에러 추적**: 크롤링 실패 시 상세 에러 정보
- **성능 모니터링**: 크롤링 시간 및 성공률 추적
