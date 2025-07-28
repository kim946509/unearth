package com.rhoonart.unearth.song.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.right_holder.service.RightHolderUtilService;
import com.rhoonart.unearth.song.dto.SongInfoUpdateRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoWithCrawlingDto;
import com.rhoonart.unearth.song.service.SongInfoService;
import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.song.service.SongUpdateService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/song")
@RequiredArgsConstructor
public class SongInfoController {
    private final SongInfoService songInfoService;
    private final SongUpdateService songUpdateService;
    private final RightHolderUtilService rightHolderUtilService;

    @GetMapping("/list")
    public String listPage(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "isCrawlingActive", required = false) Boolean isCrawlingActive,
            Model model,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        // size 허용값 검증
        if (size != 10 && size != 30 && size != 50) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<SongInfoWithCrawlingDto> songPage = songInfoService.findSongsWithCrawling(search, pageable,
                isCrawlingActive);
        model.addAttribute("response", CommonResponse.success(songPage));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("isCrawlingActive", isCrawlingActive != null && isCrawlingActive);
        // 권리자 드롭다운용 목록
        var rightHolders = rightHolderUtilService.findAllForDropdown();
        model.addAttribute("rightHolders", rightHolders);
        
        return "song/list";
    }

    @PostMapping("/{songId}/update")
    public String update(@PathVariable String songId,
            @Valid @ModelAttribute SongInfoUpdateRequestDto dto,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        songUpdateService.update(songId, dto);

        // redirect 파라미터가 있으면 해당 페이지로, 없으면 기본 음원 리스트로
        if (redirect != null && !redirect.trim().isEmpty()) {
            return "redirect:" + redirect;
        }
        return "redirect:/song/list";
    }
}