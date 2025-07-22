package com.rhoonart.unearth.user.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.exception.BadRequestException;
import com.rhoonart.unearth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSignUpService {

    private final UserInfoService userInfoService;
    private final UserRepository userRepository;

    public User signUp(String username, String password, Role role) {

        // 1. username 중복 체크
        if (userInfoService.isUsernameDuplicate(username)) {
            throw new BadRequestException("이미 사용 중인 아이디입니다.");
        }

        // 2. User 생성 및 저장
        User user = User.builder()
                .username(username)
                .password(password) // 비밀번호 암호화
                .role(role)
                .isLoginEnabled(true)
                .build();

        return userRepository.save(user);
    }
}
