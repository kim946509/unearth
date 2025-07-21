package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.service.CrawlingManagerService;
import com.rhoonart.unearth.right_holder.service.RightHolderService;
import com.rhoonart.unearth.common.CommonResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

@Slf4j
@Controller
@RequestMapping("/crawling")
@RequiredArgsConstructor
public class CrawlingController {

    private final CrawlingManagerService crawlingManagerService;
    private final RightHolderService rightHolderService;

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

    @GetMapping("/failures")
    public String viewCrawlingFailures(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        Page<com.rhoonart.unearth.crawling.dto.CrawlingFailureDto> failures = crawlingManagerService.getCrawlingFailures(page,
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