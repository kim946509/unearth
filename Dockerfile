# 멀티스테이지 빌드: Java 빌드
FROM openjdk:17-jdk-slim AS java-builder

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
COPY src ./src
RUN chmod +x gradlew
RUN ./gradlew build -x test

# 최종 이미지 (Java + Python)
FROM openjdk:17-jdk-slim

# 시스템 패키지 + Python 설치
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    gnupg \
    nano \
    pkg-config \
    build-essential \
    libssl-dev \
    libffi-dev \
    libmariadb-dev \
    python3 \
    python3-pip \
    locales \
    && rm -rf /var/lib/apt/lists/*

# Chrome 설치
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리
WORKDIR /app

# Java 애플리케이션 복사
COPY --from=java-builder /app/build/libs/*.jar app.jar

# Python 의존성 설치
COPY streaming_crawling/requirements.txt ./streaming_crawling/requirements.txt
RUN pip3 install -r ./streaming_crawling/requirements.txt

# Django 프로젝트 복사
COPY streaming_crawling/crawling/ ./streaming_crawling/crawling/
COPY streaming_crawling/config/ ./streaming_crawling/config/
COPY streaming_crawling/manage.py ./streaming_crawling/
COPY streaming_crawling/logging_setting.py ./streaming_crawling/

# 디렉토리 생성
RUN mkdir -p streaming_crawling/logs \
    && mkdir -p streaming_crawling/csv_folder

# 환경변수
ENV TZ=Asia/Seoul
ENV PYTHONPATH="/app/streaming_crawling"
ENV DJANGO_SETTINGS_MODULE="config.settings"

# 포트 노출
EXPOSE 8080

# 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
