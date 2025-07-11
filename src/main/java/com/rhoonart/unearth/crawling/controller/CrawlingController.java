package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.service.CrawlingService;
import com.rhoonart.unearth.common.CommonResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
}