package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.crawling.service.CrawlingSchedulerService;
import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/crawling")
@RequiredArgsConstructor
public class CrawlingSchedulerController {

    private final CrawlingSchedulerService crawlingSchedulerService;

    /**
     * 수동으로 전체 크롤링 실행
     */
    @PostMapping("/execute-full")
    public ResponseEntity<CommonResponse<String>> executeFullCrawling() {
        try {
            log.info("🔧 수동 전체 크롤링 요청 받음");
            crawlingSchedulerService.executeFullCrawlingManually();

            return ResponseEntity.ok(CommonResponse.success("전체 크롤링이 시작되었습니다."));
        } catch (Exception e) {
            log.error("❌ 수동 전체 크롤링 실행 중 오류", e);
            return ResponseEntity.internalServerError()
                    .body(CommonResponse.fail(ResponseCode.SERVER_ERROR,
                            "전체 크롤링 실행 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}