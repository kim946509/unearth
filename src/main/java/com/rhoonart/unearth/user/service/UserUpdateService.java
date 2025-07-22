package com.rhoonart.unearth.user.service;

import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.exception.BadRequestException;
import com.rhoonart.unearth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserInfoService userInfoService;
    private final UserRepository userRepository;

    @Transactional
    public User updateUsername(User user, String newUsername) {

        if(!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(user.getUsername())) {
            throw new BadRequestException("이미 사용 중인 아이디입니다.");
        }

        user.updateUsername(newUsername);
        return userRepository.save(user);
    }

    @Transactional
    public User updateLoginEnabled(User user, boolean loginEnabled) {
        user.updateLoginEnabled(loginEnabled);
        return userRepository.save(user);
    }
}
