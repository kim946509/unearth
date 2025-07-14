package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataWithSongInfoDto;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.service.CrawlingService;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.common.CommonResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/crawling")
@RequiredArgsConstructor
public class CrawlingController {

    private final CrawlingService crawlingService;

    @PostMapping("/execute")
    @ResponseBody
    public CommonResponse<String> executeCrawling(@Valid @ModelAttribute CrawlingExecuteRequestDto dto,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        crawlingService.executeCrawling(dto);
        return CommonResponse.success("크롤링이 성공적으로 실행되었습니다.");
    }

    @GetMapping("/data/{songId}")
    public String viewCrawlingData(
            @PathVariable String songId,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "platform", required = false) String platformStr,
            @RequestParam(value = "intervalDays", required = false) Integer intervalDays,
            HttpSession session,
            Model model) {
        SessionUserUtil.requireAdminRole(session);

        // 날짜 파싱
        LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now().minusDays(30);
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : LocalDate.now();

        // 플랫폼 파싱
        PlatformType platform = platformStr != null ? PlatformType.valueOf(platformStr) : null;

        // 간격 파싱 (null이면 1로 설정)
        Integer interval = intervalDays != null ? intervalDays : 1;

        CrawlingDataWithSongInfoDto crawlingDataWithSongInfo = crawlingService.getCrawlingDataWithFilters(songId,
                startDate,
                endDate, platform,
                interval);

        model.addAttribute("response", CommonResponse.success(crawlingDataWithSongInfo.getCrawlingDataList()));
        model.addAttribute("songId", songId);
        model.addAttribute("rightHolderId", crawlingDataWithSongInfo.getSongInfo().getRightHolder().getId());
        model.addAttribute("startDate", startDateStr);
        model.addAttribute("endDate", endDateStr);
        model.addAttribute("platform", platformStr);
        model.addAttribute("intervalDays", interval);
        model.addAttribute("songInfo", crawlingDataWithSongInfo.getSongInfo());

        return "crawling/data";
    }
}