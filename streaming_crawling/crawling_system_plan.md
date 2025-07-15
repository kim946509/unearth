
# 🎯 크롤링 시스템 기획서 (FK 없음, Enum 플랫폼, Redis/Celery 미사용)

---

## 📌 1. 개요

본 시스템은 Melon, Genie, YouTube, YouTube Music 등 다양한 음악 플랫폼에서 노래 관련 데이터를 수집하여 저장하는 **정기 + 트리거 기반 크롤링 시스템**입니다.  
Celery나 Redis 같은 메시지 브로커 없이 구현되며, 플랫폼 간 일관된 데이터를 제공하는 것이 목표입니다.

---

## 🗂️ 2. 테이블 구조

### ✅ Enum 정의 (platform)

~~~~dbml
Enum PlatformType {
  melon
  genie
  youtube
  youtube_music
}
~~~~

---

### ✅ song_info

| 필드명        | 타입       | 설명                         |
|---------------|------------|------------------------------|
| id            | char(32)   | UUID (PK), FK 없음           |
| title         | varchar    | 곡 제목                      |
| artist        | varchar    | 아티스트명                   |
| youtube_url   | varchar    | 유튜브 영상 링크             |
| melon_song_id | varchar    | 멜론 곡 ID (API 분석용)      |
| createdAt     | datetime   | 생성일                       |
| updatedAt     | datetime   | 수정일                       |

---

### ✅ crawling_period

| 필드명     | 타입       | 설명                         |
|------------|------------|------------------------------|
| id         | char(32)   | UUID (PK)                    |
| song_id    | char(32)   | song_info.id 참조 (FK 없음)  |
| start_date | date       | 크롤링 시작일                |
| end_date   | date       | 크롤링 종료일                |
| isActive   | boolean    | 현재 유효한지 여부           |
| createdAt  | datetime   | 생성일                       |
| updatedAt  | datetime   | 수정일                       |

---

### ✅ crawling_data

| 필드명     | 타입         | 설명                                |
|------------|--------------|-------------------------------------|
| id         | char(32)     | UUID (PK)                           |
| song_id    | char(32)     | song_info.id 참조 (FK 없음)         |
| views      | bigint       | 조회수                               |
| listeners  | bigint       | 청취자 수 (없을 경우 -1 또는 -999)  |
| platform   | PlatformType | melon, genie, youtube 등 (enum)     |
| createdAt  | datetime     | 수집 시점                           |
| updatedAt  | datetime     | 수정 시점                           |

---

## 🧠 3. 크롤링 방식

### 📌 (1) 정기 크롤링

- **시간:** 매일 새벽 3시
- **대상:** crawling_period.isActive = true AND 오늘이 start_date ~ end_date 사이인 곡
- **방식:** Django management command + cron
- **흐름:**
  1. 유효한 곡 필터링
  2. 각 플랫폼별 크롤링
  3. crawling_data에 저장

---

### 📌 (2) 신규 등록 크롤링

- **시점:** 곡 등록 후 즉시
- **목적:** 크롤링 가능 여부 확인 및 테스트
- **흐름:**
  1. song_info + crawling_period 생성
  2. 등록 즉시 1회 크롤링 수행
  3. crawling_data 저장

---

## 🔁 4. 데이터 처리 정책

### 📋 플랫폼별 지원 항목

| 플랫폼        | 조회수(views) | 청취자 수(listeners) |
|---------------|---------------|------------------------|
| Melon         | ✅ 지원       | ✅ 지원                |
| Genie         | ✅ 지원       | ✅ 지원                |
| YouTube       | ✅ 지원       | ❌ 미지원              |
| YouTube Music | ✅ (예정)     | ❌ 미지원              |

---

### 🚨 필드 예외 처리 기준

| 상황                    | 값     | 설명                              |
|-------------------------|--------|-----------------------------------|
| 정상 수집               | 정수   | ex. 123456                        |
| 해당 플랫폼 미지원      | `-1`   | ex. YouTube의 listeners           |
| 크롤링 실패 / 예외 발생 | `-999` | 응답 없음, 오류 발생 등          |

- `nullable = false`, 기본값 설정을 통해 NULL 방지

---

## 🛠️ 5. 기술 스택 및 구현 요소

- **Backend:** Django
- **크롤링 도구:** Selenium, BeautifulSoup
- **DB:** MySQL (UUID → char(32))
- **스케줄링:** cron + `python manage.py run_crawling_job`
- **저장:** crawling_data (DB), 필요 시 CSV로 병렬 저장
