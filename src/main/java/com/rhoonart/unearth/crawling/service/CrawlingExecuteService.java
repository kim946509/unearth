package com.rhoonart.unearth.crawling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 크롤링 실행 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingExecuteService {

    private static final String DJANGO_PROJECT_PATH = "streaming_crawling";
    private static final String SINGLE_SONG_CRAWLING_PATH = "crawling/entrypoint/run_single_song_crawling.py";
    private static final String FULL_CRAWLING_PATH = "crawling/entrypoint/run_crawling.py";

    /**
     * 단일 곡 크롤링을 실행합니다.
     * 
     * @param songId 크롤링할 곡의 ID
     */
    public void executeSingleSongCrawling(String songId) {
        try {
            // Django 프로젝트 경로 설정
            Path djangoPath = Paths.get(DJANGO_PROJECT_PATH);

            // 운영체제별 명령어 생성
            List<String> command;
            if (CrawlingCommandUtil.isWindows()) {
                command = CrawlingCommandUtil.createWindowsCommand(
                        SINGLE_SONG_CRAWLING_PATH, "--song_id",
                        songId);
            } else {
                command = CrawlingCommandUtil.createLinuxCommand(SINGLE_SONG_CRAWLING_PATH,
                        "--song_id", songId);
            }

            // ProcessBuilder 생성
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // 작업 디렉토리 설정
            if (CrawlingCommandUtil.isWindows()) {
                processBuilder.directory(Paths.get(".").toFile()); // Windows: 프로젝트 루트에서 실행
            } else {
                processBuilder.directory(djangoPath.toFile()); // Linux: streaming_crawling 폴더에서 실행
            }

            // 환경 변수 설정 (Linux 환경에서만)
            if (!CrawlingCommandUtil.isWindows()) {
                Map<String, String> env = processBuilder.environment();
                env.put("PYTHONPATH", djangoPath.resolve("streaming_crawling").toString());
                env.put("PYTHONUNBUFFERED", "1");
            }

            // 프로세스 실행
            Process process = processBuilder.start();

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
            Path djangoPath = Paths.get(DJANGO_PROJECT_PATH);

            // 운영체제별 명령어 생성
            List<String> command;
            if (CrawlingCommandUtil.isWindows()) {
                command = CrawlingCommandUtil.createWindowsCommand(FULL_CRAWLING_PATH);
            } else {
                command = CrawlingCommandUtil.createLinuxCommand(FULL_CRAWLING_PATH);
            }

            // ProcessBuilder 생성
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // 작업 디렉토리 설정
            if (CrawlingCommandUtil.isWindows()) {
                processBuilder.directory(Paths.get(".").toFile()); // Windows: 프로젝트 루트에서 실행
            } else {
                processBuilder.directory(djangoPath.toFile()); // Linux: streaming_crawling 폴더에서 실행
            }

            // 환경 변수 설정 (Linux 환경에서만)
            if (!CrawlingCommandUtil.isWindows()) {
                Map<String, String> env = processBuilder.environment();
                env.put("PYTHONPATH", djangoPath.resolve("streaming_crawling").toString());
                env.put("PYTHONUNBUFFERED", "1"); // Python 출력 버퍼링 비활성화
            }

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