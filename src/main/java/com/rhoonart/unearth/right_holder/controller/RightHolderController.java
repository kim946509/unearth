package com.rhoonart.unearth.right_holder.controller;

import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.service.RightHolderService;
import com.rhoonart.unearth.right_holder.dto.RightHolderListResponseDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderSongListResponseDto;
import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.servlet.http.HttpSession;
import com.rhoonart.unearth.right_holder.dto.RightHolderRegisterRequestDto;
import com.rhoonart.unearth.right_holder.dto.RightHolderUpdateRequestDto;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import com.rhoonart.unearth.common.exception.BaseException;

@Controller
@RequestMapping("/right-holder")
@RequiredArgsConstructor
public class RightHolderController {
    private final RightHolderService rightHolderService;

    @GetMapping("/list")
    public String listPage(
            @RequestParam(value = "holderType", required = false) String holderTypeStr,
            @RequestParam(value = "holderName", required = false) String holderName,
            @RequestParam(value = "contractDate", required = false) String contractDateStr,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);

        // 빈 문자열이면 null로 변환
        HolderType holderType = null;
        if (holderTypeStr != null && !holderTypeStr.isBlank()) {
            holderType = HolderType.valueOf(holderTypeStr);
        }
        if (holderName != null && holderName.isBlank())
            holderName = null;
        java.time.LocalDate contractDate = null;
        if (contractDateStr != null && !contractDateStr.isBlank()) {
            contractDate = java.time.LocalDate.parse(contractDateStr);
        }

        // size 제한: 10, 30, 50만 허용
        if (size != 10 && size != 30 && size != 50)
            size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<RightHolderListResponseDto> holderPage = rightHolderService.findRightHolders(holderType, holderName,
                contractDate, pageable);
        CommonResponse<Page<RightHolderListResponseDto>> response = CommonResponse.success(holderPage);
        model.addAttribute("response", response);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("user", user); // 뷰에서 사용자 정보 활용 가능
        model.addAttribute("holderType", holderTypeStr);
        model.addAttribute("holderName", holderName);
        model.addAttribute("contractDate", contractDateStr);
        return "right_holder/list";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute RightHolderRegisterRequestDto dto,
            HttpSession session,
            Model model) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);
        rightHolderService.register(dto);
        // 등록 성공 시 목록으로 이동
        return "redirect:/right-holder/list";
    }

    @PostMapping("/{rightHolderId}/update")
    public String updateSubmit(@PathVariable String rightHolderId,
            @Valid @ModelAttribute RightHolderUpdateRequestDto dto,
            HttpSession session,
            Model model) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);
        rightHolderService.update(rightHolderId, dto);
        // 수정 성공 시 목록으로 이동
        return "redirect:/right-holder/list";
    }

    @GetMapping("/{rightHolderId}")
    public String rightHolderDetailPage(
            @PathVariable String rightHolderId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "hasCrawlingData", required = false) Boolean hasCrawlingData,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            HttpSession session,
            Model model) {
        // 권한 체크: SUPER_ADMIN, ADMIN 또는 해당 권리자 본인만 접근 가능
        if (!SessionUserUtil.hasAccessToRightHolder(session, rightHolderId, rightHolderService)) {
            throw new BaseException(ResponseCode.FORBIDDEN, "접근 권한이 없습니다.");
        }

        UserDto user = SessionUserUtil.getLoginUser(session);

        // size 제한: 10, 30, 50만 허용
        if (size != 10 && size != 30 && size != 50)
            size = 10;

        Pageable pageable = PageRequest.of(page, size);
        Page<RightHolderSongListResponseDto> songPage = rightHolderService.findSongsByRightHolder(rightHolderId, search,
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
        var rightHolder = rightHolderService.findById(rightHolderId);
        model.addAttribute("rightHolder", rightHolder);
        return "right_holder/detail";
    }
}