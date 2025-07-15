# 멀티스테이지 빌드
FROM openjdk:17-jdk-slim AS java-builder

# Java 애플리케이션 빌드
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
COPY src ./src
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Python 환경 설정
FROM python:3.11-slim AS python-builder

# 시스템 패키지 설치
RUN apt-get update && apt-get install -y \
    pkg-config \
    build-essential \
    libssl-dev \
    libffi-dev \
    libmariadb-dev \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 설치
WORKDIR /app
COPY streaming_crawling/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# 최종 이미지
FROM openjdk:17-jdk-slim

# 시스템 패키지 설치
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
    locales \
    && rm -rf /var/lib/apt/lists/*

# Chrome 설치
RUN wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# Java 애플리케이션 복사
COPY --from=java-builder /app/build/libs/*.jar app.jar

# Python 환경 복사
COPY --from=python-builder /usr/local /usr/local

# Django 프로젝트 복사
COPY streaming_crawling/crawling_view/ ./streaming_crawling/crawling_view/
COPY streaming_crawling/config/ ./streaming_crawling/config/
COPY streaming_crawling/manage.py ./streaming_crawling/
COPY streaming_crawling/logging_setting.py ./streaming_crawling/

# 필요한 디렉토리 생성
RUN mkdir -p streaming_crawling/logs \
    && mkdir -p streaming_crawling/csv_folder

# 환경변수 설정
ENV TZ=Asia/Seoul
ENV PYTHONPATH="/app/streaming_crawling"
ENV DJANGO_SETTINGS_MODULE="config.settings"

# 포트 노출
EXPOSE 8080

# 시작 명령
ENTRYPOINT ["java", "-jar", "app.jar"]