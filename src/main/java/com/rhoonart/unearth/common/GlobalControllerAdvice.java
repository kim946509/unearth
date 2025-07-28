package com.rhoonart.unearth.common;

import com.rhoonart.unearth.crawling.service.CrawlingFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CrawlingFailureService crawlingFailureService;

    /**
     * 사이드바 사용할 수 있는 크롤링 실패 개수를 제공합니다.
     * 10개를 초과하면 "10+" 형태로 표시합니다.
     */
    @ModelAttribute("crawlingFailureCount")
    public String addCrawlingFailureCount() {
        try {
            return crawlingFailureService.getLimitedCrawlingFailureCount();
        } catch (Exception e) {
            log.warn("크롤링 실패 개수 조회 중 오류 발생", e);
            return "0"; // 오류 시 0으로 표시
        }
    }
}