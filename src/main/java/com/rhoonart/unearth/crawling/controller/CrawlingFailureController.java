package com.rhoonart.unearth.crawling.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.crawling.dto.CrawlingFailureDto;
import com.rhoonart.unearth.crawling.service.CrawlingFailureService;
import com.rhoonart.unearth.right_holder.service.RightHolderUtilService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/crawling")
@RequiredArgsConstructor
public class CrawlingFailureController {

    private final CrawlingFailureService crawlingFailureService;
    private final RightHolderUtilService rightHolderUtilService;

    @GetMapping("/failures")
    public String viewCrawlingFailures(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        Page<CrawlingFailureDto> failures = crawlingFailureService.getCrawlingFailures(page,
                size);
        model.addAttribute("failures", failures);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        // 권리자 드롭다운용 목록
        List<String> rightHolders = rightHolderUtilService.findAllForDropdown();
        model.addAttribute("rightHolders", rightHolders);
        return "crawling/failures";
    }
}

