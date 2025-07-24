package com.rhoonart.unearth.right_holder.controller;

import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.right_holder.dto.RightHolderSongListResponseDto;
import com.rhoonart.unearth.right_holder.service.RightHolderSongService;
import com.rhoonart.unearth.right_holder.service.RightHolderUtilService;
import com.rhoonart.unearth.user.dto.UserDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/right-holder")
@RequiredArgsConstructor
public class RightHolderSongController {

    private final RightHolderSongService rightHolderSongService;
    private final RightHolderUtilService rightHolderUtilService;

    @GetMapping("/{rightHolderId}")
    public String rightHolderDetailPage(
            @PathVariable String rightHolderId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "hasCrawlingData", required = false) Boolean hasCrawlingData,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        UserDto user = SessionUserUtil.requireLogin(session);

        // size 제한: 10, 30, 50만 허용
        if (size != 10 && size != 30 && size != 50)
            size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Page<RightHolderSongListResponseDto> songPage = rightHolderSongService.findSongsByRightHolder(user,rightHolderId, search,
                hasCrawlingData, pageable);

        CommonResponse<Page<RightHolderSongListResponseDto>> response = CommonResponse.success(songPage);
        model.addAttribute("response", response);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("hasCrawlingData", hasCrawlingData);
        model.addAttribute("rightHolderId", rightHolderId);
        model.addAttribute("user", user);
        model.addAttribute("userRole", user.getRole().name());
        // 권리자 정보 추가
        var rightHolder = rightHolderUtilService.findById(rightHolderId);
        model.addAttribute("rightHolder", rightHolder);
        return "right_holder/detail";
    }

}
