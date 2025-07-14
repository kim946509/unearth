package com.rhoonart.unearth.user.controller;

import com.rhoonart.unearth.common.CommonResponse;
import com.rhoonart.unearth.user.dto.LoginRequestDto;
import com.rhoonart.unearth.user.dto.LoginResponseDto;
import com.rhoonart.unearth.user.service.LoginService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/user/login")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;

    @GetMapping
    public String loginForm(Model model) {
        model.addAttribute("loginRequestDto", new LoginRequestDto());
        return "user/login";
    }

    @PostMapping
    @ResponseBody
    public CommonResponse<LoginResponseDto> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session) {
        LoginRequestDto loginRequestDto = LoginRequestDto.of(username, password);
        LoginResponseDto response = loginService.login(loginRequestDto);
        session.setAttribute("LOGIN_USER", response.getUserDto());
        return CommonResponse.success(response);
    }
}