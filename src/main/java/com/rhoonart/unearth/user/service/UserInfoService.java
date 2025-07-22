package com.rhoonart.unearth.user.service;

import com.rhoonart.unearth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserRepository userRepository;

    /**
     * 유저 네임(아이디) 중복 체크
     */
    public boolean isUsernameDuplicate(String username) {
        return userRepository.existsByUsername(username);
    }



}
