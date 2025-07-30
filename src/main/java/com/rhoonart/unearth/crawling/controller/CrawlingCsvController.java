package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.crawling.dto.CrawlingCsvDownloadDto;
import com.rhoonart.unearth.crawling.service.CrawlingCsvService;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.exception.ForbiddenException;
import com.rhoonart.unearth.common.util.SessionUserUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
@RequestMapping("/crawling")
@RequiredArgsConstructor
public class CrawlingCsvController {

        private final CrawlingCsvService crawlingCsvService;

        @GetMapping("/data/{songId}/csv")
        public ResponseEntity<byte[]> downloadCrawlingDataCsv(
                        @PathVariable String songId,
                        @RequestParam(value = "startDate", required = false) String startDateStr,
                        @RequestParam(value = "endDate", required = false) String endDateStr,
                        HttpSession session) {

                try {
                        // 로그인 검사
                        UserDto userDto = SessionUserUtil.requireLogin(session);

                        // CSV 데이터 및 파일명 생성
                        CrawlingCsvDownloadDto csvDownloadDto = crawlingCsvService.generateCrawlingDataCsvForDownload(
                                        userDto,
                                        songId,
                                        startDateStr,
                                        endDateStr);

                        // UTF-8 BOM + CSV 데이터를 바이트 배열로 변환
                        String csvContent = "\uFEFF" + csvDownloadDto.getCsvData();
                        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

                        // HTTP 헤더 설정 - Excel 호환성을 위해 강화된 설정
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                        headers.set("Content-Disposition",
                                        "attachment; filename*=UTF-8''" + csvDownloadDto.getFilename());
                        headers.set("Content-Transfer-Encoding", "binary");
                        headers.set("Content-Length", String.valueOf(csvBytes.length));

                        // 추가 헤더로 인코딩 명시
                        headers.set("Content-Type", "application/octet-stream; charset=UTF-8");

                        return ResponseEntity.ok()
                                        .headers(headers)
                                        .body(csvBytes);

                } catch (ForbiddenException e) {
                        log.error("❌ CSV 다운로드 권한 없음: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body("권한이 없습니다.".getBytes(StandardCharsets.UTF_8));

                } catch (BaseException e) {
                        log.error("❌ CSV 다운로드 실패 (BaseException): {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body(e.getMessage().getBytes(StandardCharsets.UTF_8));

                } catch (Exception e) {
                        log.error("❌ CSV 다운로드 실패 (Exception): {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .body("CSV 다운로드 중 오류가 발생했습니다.".getBytes(StandardCharsets.UTF_8));
                }
        }
}
