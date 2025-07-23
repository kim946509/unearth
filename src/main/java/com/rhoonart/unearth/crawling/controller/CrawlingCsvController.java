package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingCsvDownloadDto;
import com.rhoonart.unearth.crawling.service.CrawlingCsvService;
import com.rhoonart.unearth.user.dto.UserDto;
import jakarta.servlet.http.HttpSession;
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

                // 로그인 검사
                UserDto userDto = SessionUserUtil.requireLogin(session);

                // CSV 데이터 및 파일명 생성
                CrawlingCsvDownloadDto csvDownloadDto = crawlingCsvService.generateCrawlingDataCsvForDownload(userDto,
                                songId,
                                startDateStr,
                                endDateStr);

                // HTTP 헤더 설정 - Excel 호환성을 위해 application/octet-stream 사용
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.set("Content-Disposition", "attachment; filename*=UTF-8''" + csvDownloadDto.getFilename());
                headers.set("Content-Transfer-Encoding", "binary");

                return ResponseEntity.ok()
                                .headers(headers)
                                .body("\uFEFF" + csvDownloadDto.getCsvData()); // UTF-8 BOM 추가 (Excel에서 한글 깨짐 방지)
        }
}
