package com.rhoonart.unearth.common;

import com.rhoonart.unearth.crawling.service.CrawlingManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CrawlingManagerService crawlingManagerService;

    /**
     * 모든 페이지에서 사용할 수 있는 크롤링 실패 개수를 제공합니다.
     */
    @ModelAttribute("crawlingFailureCount")
    public long addCrawlingFailureCount() {
        try {
            return crawlingManagerService.getCrawlingFailureCount();
        } catch (Exception e) {
            log.warn("크롤링 실패 개수 조회 중 오류 발생", e);
            return 0L; // 오류 시 0으로 표시
        }
    }
}