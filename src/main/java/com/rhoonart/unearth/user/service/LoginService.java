package com.rhoonart.unearth.user.service;

import com.rhoonart.unearth.right_holder.service.RightHolderUtilService;
import com.rhoonart.unearth.user.dto.LoginRequestDto;
import com.rhoonart.unearth.user.dto.LoginResponseDto;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.exception.LoginException;
import com.rhoonart.unearth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RightHolderUtilService rightHolderUtilService;

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new LoginException("존재하지 않는 아이디입니다.");
        }
        User user = userOpt.get();

        // 비밀번호를 먼저 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new LoginException("비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호가 맞으면 계정 활성화 상태 확인
        if (!user.isLoginEnabled()) {
            throw new LoginException("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        return toLoginSuccess(user);
    }

    private LoginResponseDto toLoginSuccess(User user) {
        String rightHolderId = null;
        if (user.getRole() == Role.RIGHT_HOLDER) {
            try {
                rightHolderId = rightHolderUtilService.findByUserId(user.getId()).getId();
            } catch (Exception e) {
                rightHolderId = null;
            }
        }
        UserDto userDto = UserDto.of(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                rightHolderId);
        return LoginResponseDto.of(userDto, rightHolderId);
    }
}