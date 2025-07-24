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

    /**
     * 로그인 폼을 보여주는 메서드
     * 
     * @param model : Spring MVC 모델, 뷰에 데이터를 전달하는 데 사용
     * @return : 로그인 폼 뷰의 이름
     */
    @GetMapping
    public String loginForm(Model model) {
        model.addAttribute("loginRequestDto", new LoginRequestDto());
        return "user/login";
    }

    /**
     * 로그인 요청을 처리하는 메서드
     * 
     * @param username : 사용자 이름
     * @param password : 사용자 비밀번호
     * @param session  : HTTP 세션, 로그인 상태를 유지하는 데 사용
     * @return : 로그인 성공 시 사용자 정보를 담은 응답 객체
     */
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