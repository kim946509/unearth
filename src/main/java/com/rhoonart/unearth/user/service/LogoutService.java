package com.rhoonart.unearth.user.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    /**
     * 사용자 로그아웃을 처리합니다.
     * 세션을 무효화하여 로그인 상태를 해제합니다.
     */
    public void logout(HttpSession session) {
        if (session != null) {
            String username = (String) session.getAttribute("username");
            session.invalidate();
        }
    }
}
