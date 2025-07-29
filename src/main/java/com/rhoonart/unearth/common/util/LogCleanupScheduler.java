package com.rhoonart.unearth.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupScheduler {

    private final LogFileUtil logFileUtil;

    /**
     * 매월 1, 11, 21, 31일 00시 01분에 실행
     * 최근 10일 이전의 로그 파일을 삭제합니다.
     */
    @Scheduled(cron = "1 1 0 1,10,20,30 * ?")
    public void cleanupOldLogs() {
        log.info("🧹 로그 파일 정리 스케줄러 시작");

        try {
            int deletedCount = logFileUtil.deleteOldLogs(10);
            log.info("✅ 로그 파일 정리 완료: {}개 파일 삭제됨", deletedCount);

        } catch (Exception e) {
            log.error("❌ 로그 파일 정리 중 오류 발생", e);
        }
    }
}