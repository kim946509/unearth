package com.rhoonart.unearth.user.controller;

import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.user.dto.PasswordChangeRequestDto;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.service.PasswordChangeService;
import com.rhoonart.unearth.common.util.SessionUserUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/user/password")
@RequiredArgsConstructor
public class PasswordChangeController {
    private final PasswordChangeService passwordChangeService;

    /**
     * 비밀번호 변경 폼 페이지
     * 권리자 본인만 접근 가능
     */
    @GetMapping("/change")
    public String passwordChangeForm(Model model, HttpSession session) {
        // 세션에서 로그인 사용자 정보 확인
        String userRole = SessionUserUtil.getUserRole(session);

        if (userRole == null) {
            return "user/login";
        } else if (!userRole.equals("RIGHT_HOLDER")) {
            // 권리자가 아닌 경우 권리자 목록 페이지로 리다이렉트
            return "redirect:/right-holder/list";
        }

        model.addAttribute("passwordChangeRequestDto", PasswordChangeRequestDto.of("", "", "", ""));
        return "user/password-change";
    }

    /**
     * 비밀번호 변경 처리
     */
    @PostMapping("/change")
    @ResponseBody
    public CommonResponse<String> changePassword(
            @RequestBody PasswordChangeRequestDto requestDto,
            HttpSession session) {
        UserDto loginUser = SessionUserUtil.getLoginUser(session);
        if (loginUser == null) {
            return CommonResponse.fail(ResponseCode.FORBIDDEN, "로그인이 필요합니다.");
        }

        passwordChangeService.changePassword(requestDto, loginUser);
        return CommonResponse.success("비밀번호가 성공적으로 변경되었습니다.");

    }
}