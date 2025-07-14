package com.rhoonart.unearth.crawling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingExecuteService {

    /**
     * 단일 곡 크롤링을 실행합니다.
     * 
     * @param songId 크롤링할 곡의 ID
     */
    public void executeSingleSongCrawling(String songId) {
        try {
            // Django 프로젝트 경로 설정
            Path djangoPath = Paths.get("streaming_crawling");
            Path scriptPath = djangoPath.resolve("crawling_view/controller/run_single_song_crawling.py");

            // Python 명령어 구성 (가상환경 활성화 후 스크립트 실행)
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd", "/c",
                    "env\\Scripts\\activate.bat && python crawling_view/controller/run_single_song_crawling.py --song_id "
                            + songId);

            // 작업 디렉토리 설정 (Django 프로젝트 루트)
            processBuilder.directory(djangoPath.toFile());

            log.info("크롤링 실행 시작: songId={}", songId);

            // 프로세스 실행
            Process process = processBuilder.start();

            // 출력 스트림 읽기 (별도 스레드에서)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("크롤링 출력: {}", line);
                    }
                } catch (IOException e) {
                    log.error("크롤링 출력 읽기 오류", e);
                }
            }).start();

            // 에러 스트림 읽기 (별도 스레드에서)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Django 로그는 대부분 정보성 메시지이므로 INFO 레벨로 로깅
                        if (line.contains("ERROR") || line.contains("Exception") || line.contains("Traceback")) {
                            log.error("크롤링 에러: {}", line);
                        } else {
                            log.info("크롤링 로그: {}", line);
                        }
                    }
                } catch (IOException e) {
                    log.error("크롤링 에러 읽기 오류", e);
                }
            }).start();

            // 비동기 실행 - 프로세스 시작 후 즉시 반환
            log.info("크롤링 실행 시작됨: songId={}", songId);

            // 프로세스 완료를 별도 스레드에서 모니터링 (선택사항)
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        log.info("크롤링 실행 완료: songId={}", songId);
                    } else {
                        log.error("크롤링 실행 실패: songId={}, exitCode={}", songId, exitCode);
                    }
                } catch (InterruptedException e) {
                    log.error("크롤링 프로세스 모니터링 중 인터럽트: songId={}", songId, e);
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (IOException e) {
            log.error("크롤링 프로세스 실행 오류: songId={}", songId, e);
            throw new RuntimeException("크롤링 프로세스 실행 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 전체 크롤링을 실행합니다.
     */
    public void executeFullCrawling() {
        try {
            // Django 프로젝트 경로 설정
            Path djangoPath = Paths.get("streaming_crawling");

            // Python 명령어 구성 (가상환경 활성화 후 스크립트 실행)
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd", "/c",
                    "env\\Scripts\\activate.bat && python crawling_view/controller/run_crawling.py");

            // 작업 디렉토리 설정 (Django 프로젝트 루트)
            processBuilder.directory(djangoPath.toFile());

            log.info("전체 크롤링 실행 시작");

            // 프로세스 실행
            Process process = processBuilder.start();

            // 출력 스트림 읽기 (별도 스레드에서)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("전체 크롤링 출력: {}", line);
                    }
                } catch (IOException e) {
                    log.error("전체 크롤링 출력 읽기 오류", e);
                }
            }).start();

            // 에러 스트림 읽기 (별도 스레드에서)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Django 로그는 대부분 정보성 메시지이므로 INFO 레벨로 로깅
                        if (line.contains("ERROR") || line.contains("Exception") || line.contains("Traceback")) {
                            log.error("전체 크롤링 에러: {}", line);
                        } else {
                            log.info("전체 크롤링 로그: {}", line);
                        }
                    }
                } catch (IOException e) {
                    log.error("전체 크롤링 에러 읽기 오류", e);
                }
            }).start();

            // 비동기 실행 - 프로세스 시작 후 즉시 반환
            log.info("전체 크롤링 실행 시작됨");

            // 프로세스 완료를 별도 스레드에서 모니터링 (선택사항)
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        log.info("전체 크롤링 실행 완료");
                    } else {
                        log.error("전체 크롤링 실행 실패: exitCode={}", exitCode);
                    }
                } catch (InterruptedException e) {
                    log.error("전체 크롤링 프로세스 모니터링 중 인터럽트", e);
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (IOException e) {
            log.error("전체 크롤링 프로세스 실행 오류", e);
            throw new RuntimeException("전체 크롤링 프로세스 실행 중 오류가 발생했습니다.", e);
        }
    }
}