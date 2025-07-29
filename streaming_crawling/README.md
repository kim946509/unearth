# 🎶 Streaming Platform Auto Crawling System

스트리밍 플랫폼(Melon, YouTube, YouTube Music, Genie)을 자동으로 크롤링하여 음악 데이터를 수집하는 Django 기반 시스템입니다.

## 🎯 프로젝트 개요

이 시스템은 다음과 같은 특징을 가집니다:

- **명령어 기반 크롤링**: Django 웹서버 없이 CLI 명령어로 크롤링 실행
- **다중 플랫폼 지원**: Melon, YouTube, YouTube Music, Genie 플랫폼 동시 크롤링
- **모듈화된 구조**: 데이터, 서비스, 매니저 계층으로 명확한 역할 분리
- **YouTube Data API 통합**: YouTube 크롤링은 공식 API 사용으로 안정성 향상

## ⚙️ 기술 스택

- **Backend**: Django 4.2.21
- **Database**: MySQL (운영), SQLite (개발)
- **Web Scraping**: Selenium 4.34.2, BeautifulSoup4 4.13.4
- **Data Processing**: Pandas 2.3.1
- **YouTube API**: YouTube Data API v3

## 🚀 설치 및 실행


### 1. 가상환경 생성 및 활성화

```bash
# 가상환경 생성
python -m venv env

# 가상환경 활성화 (Windows)
env\Scripts\activate

# 가상환경 활성화 (Linux/Mac)
source env/bin/activate
```

### 2. 패키지 설치

```bash
pip install -r requirements.txt
```

### 3. 데이터베이스 마이그레이션

```bash
python manage.py makemigrations
python manage.py migrate
```


## 📁 프로젝트 구조

```
streaming_crawling/
├── 📁 crawling/                    # 메인 크롤링 앱
│   ├── 📁 models/                       # 데이터 모델
│   │   ├── base.py                      # 기본 모델 클래스
│   │   ├── song_info.py                 # 노래 정보 모델
│   │   ├── crawling_data.py             # 크롤링 데이터 모델
│   │   ├── crawling_period.py           # 크롤링 기간 모델
│   │   ├── crawling_failure.py          # 크롤링 실패 모델
│   │   └── youtube_video_viewcount.py   # YouTube 조회수 모델
│   ├── 📁 service/                      # 플랫폼별 크롤링 서비스
│   │   ├── 📁 melon/                    # Melon 크롤링
│   │   ├── 📁 genie/                    # Genie 크롤링
│   │   ├── 📁 youtube/                  # YouTube 크롤링 (API 기반)
│   │   └── 📁 youtube_music/            # YouTube Music 크롤링
│   ├── 📁 managers/                     # 크롤링 관리 계층
│   │   ├── crawling_manager.py          # 전체 크롤링 관리
│   │   └── single_crawling_manager.py   # 단일 곡 크롤링 관리
│   ├── 📁 repository/                   # 데이터 저장소
│   │   ├── db_writer.py                 # 데이터베이스 저장
│   │   ├── song_service.py              # 노래 정보 서비스
│   │   └── failure_service.py           # 실패 처리 서비스
│   ├── 📁 entrypoint/                   # 실행 진입점
│   │   ├── run_crawling.py              # 전체 크롤링 실행
│   │   └── run_single_song_crawling.py  # 단일 곡 크롤링 실행
│   ├── 📁 utils/                        # 유틸리티 함수
│   │   ├── driver.py                    # Selenium 드라이버
│   │   ├── constants.py                 # 상수 정의
│   │   ├── matching.py                  # 데이터 매칭
│   │   └── slack_notifier.py            # Slack 알림
│   └── 📁 test/                         # 테스트 파일
├── 📁 config/                           # Django 설정
│   ├── settings.py                      # 프로젝트 설정
│   ├── urls.py                          # URL 설정
│   └── wsgi.py                          # WSGI 설정
├── 📁 logs/                             # 크롤링 로그 파일
├── 📁 csv_folder/                       # CSV 출력 폴더
├── 📁 user_data/                        # 사용자 데이터
├── 📁 명령어/                           # 실행 명령어 모음
├── manage.py                            # Django 관리 명령어
├── requirements.txt                     # Python 패키지 목록
├── logging_setting.py                   # 로깅 설정
├── crawling_system_plan.md              # 시스템 기획서
└── README.md                            # 프로젝트 문서
```

## 🔧 사용법

### 1. 전체 크롤링 실행

```bash
# 모든 활성 노래에 대해 모든 플랫폼 크롤링
python crawling/entrypoint/run_crawling.py

# 특정 날짜 크롤링
python crawling/entrypoint/run_crawling.py --date 2024-07-28
```

### 2. 단일 곡 크롤링 실행

```bash
# 특정 곡 ID로 크롤링
python crawling/entrypoint/run_single_song_crawling.py --song_id abc123

# 특정 플랫폼만 크롤링
python crawling/entrypoint/run_single_song_crawling.py --song_id abc123 --platform YOUTUBE

# CSV/DB 저장 옵션 설정
python crawling/entrypoint/run_single_song_crawling.py --song_id abc123 --save_csv --save_db
```

### 3. Django 관리 명령어

```bash
# Django 서버 실행
python manage.py runserver

# 데이터베이스 마이그레이션
python manage.py makemigrations
python manage.py migrate
```

## 📊 데이터 모델

### SongInfo (노래 정보)

- `title_ko`, `artist_ko`: 한국어 제목/아티스트
- `title_en`, `artist_en`: 영어 제목/아티스트
- `youtube_url`: YouTube 영상 URL
- `melon_song_id`: Melon 곡 ID

### CrawlingData (크롤링 데이터)

- `song_id`: 노래 ID (SongInfo 참조)
- `views`: 조회수 (정상: 숫자, 미지원: -1, 오류: -999)
- `listeners`: 청취자 수 (정상: 숫자, 미지원: -1, 오류: -999)
- `platform`: 플랫폼명 (MELON, GENIE, YOUTUBE, YOUTUBE_MUSIC)

### CrawlingPeriod (크롤링 기간)

- `song_id`: 노래 ID
- `start_date`, `end_date`: 크롤링 기간
- `is_active`: 활성화 여부
- `youtube_url`: YouTube URL (크롤링용)
- `channel`, `youtube_title`: YouTube 영상 정보

### CrawlingFailure (크롤링 실패)

- `song_id`: 실패한 노래 ID
- `platform`: 실패한 플랫폼
- `error_message`: 오류 메시지
- `failed_at`: 실패 시점

### YoutubeVideoViewCount (YouTube 조회수)

- `crawling_period_id`: 크롤링 기간 ID
- `date`: 조회수 수집 날짜
- `view_count`: 해당 날짜의 조회수

## 🔄 크롤링 프로세스

### 전체 크롤링

1. **활성 노래 조회**: `CrawlingPeriod.is_active = True`인 노래들 조회
2. **플랫폼별 필터링**: 각 플랫폼에서 크롤링 가능한 노래 필터링
3. **크롤링 실행**:
   - **Melon/Genie/YouTube Music**: Selenium을 사용한 웹 스크래핑
   - **YouTube**: YouTube Data API v3 사용
4. **데이터 저장**: 데이터베이스 및 CSV 파일 저장
5. **실패 처리**: 크롤링 실패 시 `CrawlingFailure` 테이블에 기록

### 단일 곡 크롤링

1. **곡 정보 조회**: `SongInfo`에서 해당 곡 정보 조회
2. **플랫폼별 크롤링**: 지정된 플랫폼 또는 전체 플랫폼 크롤링
3. **결과 저장**: CSV/DB 저장 옵션에 따라 결과 저장

## 🚨 예외 처리

| 상황          | views/listeners 값 | 설명                               |
| ------------- | ------------------ | ---------------------------------- |
| 정상 수집     | 정수 (예: 123456)  | 정상적으로 크롤링된 데이터         |
| 플랫폼 미지원 | -1                 | 해당 플랫폼에서 지원하지 않는 필드 |
| 크롤링 실패   | -999               | 오류 발생 또는 응답 없음           |

## 📝 로그 관리

### 로그 파일 구조

```
streaming_crawling/
└── logs/                        # 시스템 로그
    ├── crawling_{date}_{time}.log               # 전체 크롤링 로그
    └── single_crawling_{date}_{time}_{songId}.log  # 단일 곡 크롤링 로그
```

### 로그 설정

- **로그 레벨**: INFO
- **인코딩**: UTF-8
- **출력**: 파일 + 콘솔
- **자동 정리**: 10일마다 오래된 로그 파일 자동 삭제(스프링 스케줄러가 실행행)

### 로그 확인 방법

```bash
# 전체 크롤링 로그 확인
tail -f logs/crawling_*.log

# 단일 곡 크롤링 로그 확인
tail -f logs/single_crawling_*.log
```

## 🔧 주요 기능

### YouTube Data API 통합

- YouTube 크롤링은 Selenium 대신 공식 API 사용
- 안정성 및 속도 향상
- API 키 설정 필요 (`config/keys.py`)

### 실패 처리 시스템

- 크롤링 실패 시 자동으로 `CrawlingFailure` 테이블에 기록
- 실패한 곡 재크롤링 기능
- 실패 통계 및 모니터링

### 로그 파일 관리

- 10일 자동 정리로 디스크 공간 절약
- 상세한 크롤링 과정 추적

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.
