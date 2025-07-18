package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataWithSongInfoDto;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.service.CrawlingService;
import com.rhoonart.unearth.right_holder.service.RightHolderService;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.user.exception.ForbiddenException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.data.domain.Page;

@Controller
@RequestMapping("/crawling")
@RequiredArgsConstructor
public class CrawlingController {

    private final CrawlingService crawlingService;
    private final RightHolderService rightHolderService;

    @PostMapping("/execute")
    @ResponseBody
    public CommonResponse<String> executeCrawling(@Valid @ModelAttribute CrawlingExecuteRequestDto dto,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        crawlingService.executeCrawling(dto);
        return CommonResponse.success("크롤링이 성공적으로 실행되었습니다.");
    }

    @PostMapping("/execute-only")
    @ResponseBody
    public CommonResponse<String> executeCrawlingOnly(
            HttpSession session, @RequestParam String songId) {
        SessionUserUtil.requireAdminRole(session);
        crawlingService.executeCrawlingOnly(songId);
        return CommonResponse.success("크롤링이 성공적으로 실행되었습니다.");
    }

    @GetMapping("/data/{songId}")
    public String viewCrawlingData(
            @PathVariable String songId,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "platform", required = false) String platformStr,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            HttpSession session,
            Model model) {
        // 권한 체크: SUPER_ADMIN, ADMIN 또는 해당 권리자 본인만 접근 가능
        SessionUserUtil.requireLogin(session);

        // 음원 정보를 통해 권리자 ID 확인
        SongInfo songInfo = crawlingService.getSongInfoById(songId);
        String rightHolderId = songInfo.getRightHolder().getId();

        // 권한 확인
        if (!SessionUserUtil.hasAccessToRightHolder(session, rightHolderId, rightHolderService)) {
            throw new ForbiddenException("해당 권리자의 크롤링 데이터에 접근할 권한이 없습니다.");
        }

        // 날짜 파싱 (전체 기간으로 설정)
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            try {
                startDate = LocalDate.parse(startDateStr);
            } catch (DateTimeParseException e) {
                // 잘못된 날짜 형식이면 null로 처리
            }
        }

        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            try {
                endDate = LocalDate.parse(endDateStr);
            } catch (DateTimeParseException e) {
                // 잘못된 날짜 형식이면 null로 처리
            }
        }

        // 플랫폼 파싱 (빈 문자열이면 null로 처리)
        PlatformType platform = null;
        if (platformStr != null && !platformStr.trim().isEmpty()) {
            try {
                platform = PlatformType.valueOf(platformStr);
            } catch (IllegalArgumentException e) {
                // 잘못된 플랫폼 값이면 null로 처리
            }
        }

        // 페이지 크기 제한: 10, 20, 50, 100만 허용
        if (size != 10 && size != 20 && size != 50 && size != 100)
            size = 10;

        // 페이징 처리 (페이지 번호는 1부터 시작)
        int pageNumber = Math.max(1, page);
        int pageSize = size;

        CrawlingDataWithSongInfoDto crawlingDataWithSongInfo = crawlingService.getCrawlingDataWithFilters(
                songId, startDate, endDate, platform, pageNumber, pageSize);

        model.addAttribute("response", CommonResponse.success(crawlingDataWithSongInfo.getCrawlingDataList()));
        model.addAttribute("crawlingData", crawlingDataWithSongInfo); // 그룹화된 데이터 포함
        model.addAttribute("songId", songId);
        model.addAttribute("rightHolderId", crawlingDataWithSongInfo.getSongInfo().getRightHolder().getId());
        model.addAttribute("startDate", startDateStr);
        model.addAttribute("endDate", endDateStr);
        model.addAttribute("platform", platformStr);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", crawlingDataWithSongInfo.getTotalPages());
        model.addAttribute("totalElements", crawlingDataWithSongInfo.getTotalElements());
        model.addAttribute("songInfo", crawlingDataWithSongInfo.getSongInfo());

        return "crawling/data";
    }

    @GetMapping("/failures")
    public String viewCrawlingFailures(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        Page<com.rhoonart.unearth.crawling.dto.CrawlingFailureDto> failures = crawlingService.getCrawlingFailures(page,
                size);
        model.addAttribute("failures", failures);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        // 권리자 드롭다운용 목록
        var rightHolders = rightHolderService.findAllForDropdown();
        model.addAttribute("rightHolders", rightHolders);
        return "crawling/failures";
    }
}