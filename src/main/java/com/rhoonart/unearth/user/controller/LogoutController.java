package com.rhoonart.unearth.user.controller;

import com.rhoonart.unearth.user.service.LogoutService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class LogoutController {

    private final LogoutService logoutService;

    /**
     * 로그아웃을 처리하고 로그인 페이지로 리다이렉트합니다.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        logoutService.logout(session);
        return "redirect:/user/login";
    }
}
