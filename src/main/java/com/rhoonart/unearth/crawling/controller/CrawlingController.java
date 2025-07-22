package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.service.CrawlingManagerService;
import com.rhoonart.unearth.common.CommonResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/crawling")
@RequiredArgsConstructor
public class CrawlingController {

    private final CrawlingManagerService crawlingManagerService;

    /**
     * 영상 정보와 함께 크롤링을 실행 할 때 사용하는 API
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/execute")
    @ResponseBody
    public CommonResponse<String> executeCrawling(@Valid @ModelAttribute CrawlingExecuteRequestDto dto,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        crawlingManagerService.executeCrawling(dto);
        return CommonResponse.success("크롤링이 성공적으로 실행되었습니다.");
    }

    /**
     * 실패한 곡을 크롤링 할 때 사용하는 API
     * @param session
     * @param songId
     * @return
     */
    @PostMapping("/execute-only")
    @ResponseBody
    public CommonResponse<String> executeCrawlingOnly(
            HttpSession session, @RequestParam String songId) {
        SessionUserUtil.requireAdminRole(session);
        crawlingManagerService.executeCrawlingOnly(songId);
        return CommonResponse.success("크롤링이 성공적으로 실행되었습니다.");
    }


}