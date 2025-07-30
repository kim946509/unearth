# 테스트 데이터 생성 가이드

## 📋 개요

2024년 7월 24일부터 2025년 7월 28일까지 특정 곡(`a0cbe0c5-1808-4a1f-958e-01974a4716f8`)에 대해 각 플랫폼별 가짜 크롤링 데이터를 생성하는 방법을 설명합니다.

## 🎯 생성될 데이터

- **기간**: 2024-07-28 ~ 2025-07-29 (약 1년, 367일)
- **플랫폼**: YOUTUBE_MUSIC, MELON, GENIE, YOUTUBE
- **데이터 수**: 4개 플랫폼 × 367일 × 곡 개수 = 1,468개 레코드 × 곡 개수
- **Views**: 100에서 시작하여 매일 1 ~ 1000씩 랜덤하게 증가
- **Listeners**:
  - YOUTUBE, YOUTUBE_MUSIC: -1 (제공하지 않음)
  - MELON, GENIE: 100에서 시작하여 매일 1 ~ 1000씩 랜덤하게 증가
- **Created At**: 각 날짜별 오후 5시 (17:00:00)로 고정
- **Updated At**: Created At과 동일 (초기 생성 시)

## 🚀 방법 1: ApplicationRunner 사용 (권장)

### 1. 설정 파일 수정

`src/main/resources/application.yml` 파일에서 테스트 데이터 생성을 활성화:

```yaml
test:
  data:
    enabled: true # false에서 true로 변경
    song:
      ids: # 여러 곡 ID를 리스트로 설정
        - "a0cbe0c5-1808-4a1f-958e-01974a4716f8"
        - "another-song-id-here"
        - "third-song-id-here"
    start:
      date: "2024-07-28"
    end:
      date: "2025-07-29"
```

### 2. 애플리케이션 실행

```bash
# Spring Boot 애플리케이션 실행
./gradlew bootRun
```

### 3. 로그 확인

애플리케이션 시작 시 다음과 같은 로그가 출력됩니다:

```
🧪 테스트 데이터 초기화 시작
📅 기간: 2024-07-28 ~ 2025-07-29
🎵 대상 곡 ID 개수: 3
🎵 곡 ID a0cbe0c5-1808-4a1f-958e-01974a4716f8 처리 중...
✅ 곡 ID a0cbe0c5-1808-4a1f-958e-01974a4716f8 테스트 데이터 생성 완료: 1484개 데이터 생성됨
🎵 곡 ID another-song-id-here 처리 중...
✅ 곡 ID another-song-id-here 테스트 데이터 생성 완료: 1484개 데이터 생성됨
🎵 곡 ID third-song-id-here 처리 중...
✅ 곡 ID third-song-id-here 테스트 데이터 생성 완료: 1484개 데이터 생성됨
🎉 전체 테스트 데이터 생성 완료: 총 4452개 데이터 생성됨
```
