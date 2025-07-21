package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingCsvDownloadDto;
import com.rhoonart.unearth.crawling.service.CrawlingCsvService;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/crawling")
@RequiredArgsConstructor
public class CrawlingCsvController {

        private final CrawlingCsvService crawlingCsvService;

        @GetMapping("/data/{songId}/csv")
        public ResponseEntity<String> downloadCrawlingDataCsv(
                        @PathVariable String songId,
                        @RequestParam(value = "startDate", required = false) String startDateStr,
                        @RequestParam(value = "endDate", required = false) String endDateStr,
                        HttpSession session) {
                // 권한 체크: SUPER_ADMIN, ADMIN 또는 해당 권리자 본인만 접근 가능
                SessionUserUtil.requireLogin(session);

                // Service에서 CSV 데이터 및 파일명 생성
                CrawlingCsvDownloadDto csvDownloadDto = crawlingCsvService.generateCrawlingDataCsvForDownload(songId,
                                startDateStr,
                                endDateStr);

                // 디버깅 로그
                log.info("생성된 파일명: {}", csvDownloadDto.getFilename());

                // 한글 파일명 인코딩
                String encodedFilename = URLEncoder.encode(csvDownloadDto.getFilename(), StandardCharsets.UTF_8)
                                .replaceAll("\\+", "%20");

                log.info("인코딩된 파일명: {}", encodedFilename);

                // HTTP 헤더 설정
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
                headers.set("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

                log.info("Content-Disposition 헤더: attachment; filename*=UTF-8''{}", encodedFilename);

                return ResponseEntity.ok()
                                .headers(headers)
                                .body("\uFEFF" + csvDownloadDto.getCsvData()); // UTF-8 BOM 추가 (Excel에서 한글 깨짐 방지)
        }

}
