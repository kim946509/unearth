package com.rhoonart.unearth.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * 홈 페이지로 접근 시 로그인 페이지로 리다이렉트
     * @return
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/user/login";
    }
}