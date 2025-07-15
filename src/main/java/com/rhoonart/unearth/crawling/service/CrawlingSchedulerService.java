package com.rhoonart.unearth.crawling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CrawlingSchedulerService {

    private final CrawlingExecuteService crawlingExecuteService;

    // 크롤링 실행 상태 추적
    private volatile boolean isCrawlingRunning = false;

    // 서비스 초기화 시 로그
    public CrawlingSchedulerService(CrawlingExecuteService crawlingExecuteService) {
        this.crawlingExecuteService = crawlingExecuteService;
        log.info("🎯 CrawlingSchedulerService 초기화 완료 - 3분 주기 스케줄러 등록됨");
        log.info("⏰ 다음 스케줄 실행 예정: 매 3분마다 (0초, 3분, 6분, 9분...)");
    }

    /**
     * 30분마다 전체 크롤링 실행 (테스트용)
     * cron: 초 분 시 일 월 요일
     */
    // @Scheduled(cron = "0 */3 * * * *") // 매 5분마다 (테스트용)
    // public void scheduleFullCrawlingEvery30Minutes() {
    // log.info("3분 주기 전체 크롤링 스케줄 실행");

    // // 이미 크롤링이 실행 중이면 스킵
    // if (isCrawlingRunning) {
    // log.warn("⚠️ 크롤링이 이미 실행 중이므로 스킵합니다.");
    // return;
    // }

    // try {
    // isCrawlingRunning = true;
    // crawlingExecuteService.executeFullCrawling();
    // log.info("✅ 3분 주기 전체 크롤링 스케줄 완료");
    // } catch (Exception e) {
    // log.error("❌ 3분 주기 전체 크롤링 스케줄 실행 중 오류", e);
    // } finally {
    // // 5분 후에 상태 초기화 (크롤링 완료 예상 시간)
    // new Thread(() -> {
    // try {
    // Thread.sleep(5 * 60 * 1000); // 5분
    // isCrawlingRunning = false;
    // log.info("🔄 크롤링 실행 상태 초기화 완료");
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // }
    // }).start();
    // }
    // }

    /**
     * 매일 오후 5시에 전체 크롤링 실행 (운영용)
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 17 * * *") // 매일 오후 5시
    public void scheduleFullCrawlingDaily() {
        log.info("일일 전체 크롤링 스케줄 실행");
        try {
            crawlingExecuteService.executeFullCrawling();
            log.info("✅ 일일 전체 크롤링 스케줄 완료");
        } catch (Exception e) {
            log.error("❌ 일일 전체 크롤링 스케줄 실행 중 오류", e);
        }
    }

    /**
     * 수동으로 전체 크롤링 실행
     */
    public void executeFullCrawlingManually() {
        log.info("🔧 수동 전체 크롤링 실행");

        // 이미 크롤링이 실행 중이면 예외 발생
        if (isCrawlingRunning) {
            throw new RuntimeException("크롤링이 이미 실행 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            isCrawlingRunning = true;
            crawlingExecuteService.executeFullCrawling();
            log.info("✅ 수동 전체 크롤링 완료");
        } catch (Exception e) {
            log.error("❌ 수동 전체 크롤링 실행 중 오류", e);
            throw e;
        } finally {
            // 5분 후에 상태 초기화
            new Thread(() -> {
                try {
                    Thread.sleep(5 * 60 * 1000); // 5분
                    isCrawlingRunning = false;
                    log.info("🔄 수동 크롤링 실행 상태 초기화 완료");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}