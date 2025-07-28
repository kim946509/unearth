package com.rhoonart.unearth.user.controller;

import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.util.SessionUserUtil;
import com.rhoonart.unearth.user.dto.AdminPasswordChangeRequestDto;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.service.AdminPasswordChangeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 관리자 비밀번호 변경 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/admin/password")
@RequiredArgsConstructor
public class AdminPasswordController {

    private final AdminPasswordChangeService adminPasswordChangeService;

    /**
     * 관리자 비밀번호 변경 페이지
     */
    @GetMapping("/change")
    public String passwordChangePage(HttpSession session, Model model) {
        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);
        return "admin/password-change";
    }

    /**
     * 관리자 비밀번호 변경 API
     * 
     * @param requestDto 비밀번호 변경 요청
     * @return 변경 결과
     */
    @PostMapping("/change")
    @ResponseBody
    public CommonResponse<Void> changePassword(HttpSession session,@RequestBody AdminPasswordChangeRequestDto requestDto) {

        // SUPER_ADMIN 또는 ADMIN 권한 체크
        UserDto user = SessionUserUtil.requireAdminRole(session);

        // 비밀번호 변경 실행
        adminPasswordChangeService.changeAdminPassword(requestDto, user.getId());

        return CommonResponse.success(null);
    }
}