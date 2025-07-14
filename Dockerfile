# 멀티스테이지 빌드: Java + Python 환경
FROM --platform=$BUILDPLATFORM openjdk:17-jdk-slim as java-builder

# Java 애플리케이션 빌드
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Python 환경 설정
FROM --platform=$BUILDPLATFORM python:3.11-slim as python-builder

# Python 의존성 설치
WORKDIR /app
COPY streaming_crawling/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# 최종 이미지
FROM python:3.11-slim

# 시스템 패키지 설치
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    wget \
    unzip \
    curl \
    gnupg \
    nano \
    pkg-config \
    default-libmysqlclient-dev \
    build-essential \
    libssl-dev \
    libffi-dev \
    libmysqlclient-dev \
    locales \
    && rm -rf /var/lib/apt/lists/*

# Chrome 설치 (크롤링용) - 아키텍처 감지
RUN if [ "$(uname -m)" = "x86_64" ]; then \
        # AMD64용 Chrome 설치
        wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
        && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
        && apt-get update \
        && apt-get install -y google-chrome-stable; \
    elif [ "$(uname -m)" = "aarch64" ]; then \
        # ARM64용 Chromium 설치 (Chrome 대신)
        apt-get update \
        && apt-get install -y chromium-browser; \
    fi \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# Java 애플리케이션 복사
COPY --from=java-builder /app/build/libs/*.jar app.jar

# Python 환경 복사
COPY --from=python-builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=python-builder /usr/local/bin /usr/local/bin

# Django 프로젝트 복사 (.env 제외)
COPY streaming_crawling/ ./streaming_crawling/

# Python 가상환경 생성 (Docker 내부용)
RUN python -m venv /app/venv
ENV PATH="/app/venv/bin:$PATH"

# Python 패키지 재설치 (가상환경에)
COPY streaming_crawling/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# 필요한 디렉토리 생성
RUN mkdir -p /app/streaming_crawling/logs \
    && mkdir -p /app/streaming_crawling/csv_folder \
    && mkdir -p /app/streaming_crawling/csv_folder/rhoonart

# 환경변수 설정
ENV JAVA_OPTS="-Xmx2g -Xms1g"
ENV PYTHONPATH="/app/streaming_crawling"
ENV DJANGO_SETTINGS_MODULE="config.settings"
ENV TZ=Asia/Seoul

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 시작 스크립트
COPY docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]