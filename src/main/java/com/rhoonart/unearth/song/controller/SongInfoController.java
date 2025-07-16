package com.rhoonart.unearth.song.controller;

import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.song.dto.SongInfoRegisterRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoUpdateRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoWithCrawlingDto;
import com.rhoonart.unearth.song.service.SongInfoService;
import com.rhoonart.unearth.right_holder.service.RightHolderService;
import com.rhoonart.unearth.common.CommonResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/song")
@RequiredArgsConstructor
public class SongInfoController {
    private final SongInfoService songInfoService;
    private final RightHolderService rightHolderService;

    @GetMapping("/list")
    public String listPage(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        Pageable pageable = PageRequest.of(page, size);
        Page<SongInfoWithCrawlingDto> songPage = songInfoService.findSongsWithCrawling(search, pageable);
        model.addAttribute("response", CommonResponse.success(songPage));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        // 권리자 드롭다운용 목록
        var rightHolders = rightHolderService.findAllForDropdown();
        model.addAttribute("rightHolders", rightHolders);

        // 권리자가 없으면 경고 로그 출력 (디버깅용)
        if (rightHolders.isEmpty()) {
            System.out.println("⚠️ 등록된 권리자가 없습니다. 음원 등록을 위해서는 먼저 권리자를 등록해주세요.");
        } else {
            System.out.println("✅ 권리자 목록 로드 완료: " + rightHolders.size() + "개");
        }
        return "song/list";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute SongInfoRegisterRequestDto dto,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        songInfoService.register(dto);
        return "redirect:/song/list";
    }

    @PostMapping("/{songId}/update")
    public String update(@PathVariable String songId,
            @Valid @ModelAttribute SongInfoUpdateRequestDto dto,
            HttpSession session) {
        SessionUserUtil.requireAdminRole(session);
        songInfoService.update(songId, dto);
        return "redirect:/song/list";
    }
}