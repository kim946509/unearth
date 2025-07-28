package com.rhoonart.unearth.user.service;

import com.rhoonart.unearth.user.exception.BadRequestException;
import com.rhoonart.unearth.user.dto.AdminPasswordChangeRequestDto;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 비밀번호 변경 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 관리자 비밀번호를 변경합니다.
     * 
     * @param requestDto 비밀번호 변경 요청
     * @param adminId    관리자 ID
     */
    @Transactional
    public void changeAdminPassword(AdminPasswordChangeRequestDto requestDto, String adminId) {
        log.info("관리자 비밀번호 변경 시작: adminId={}", adminId);

        // 현재 비밀번호 유효성 검사
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BadRequestException("관리자 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), admin.getPassword())) {
            throw new BadRequestException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 유효성 검사
        validateNewPassword(requestDto.getNewPassword());

        // 비밀번호 변경
        String encodedNewPassword = passwordEncoder.encode(requestDto.getNewPassword());
        admin.changePassword(encodedNewPassword);

        userRepository.save(admin);

        log.info("관리자 비밀번호 변경 완료: adminId={}", adminId);
    }

    /**
     * 새 비밀번호 유효성을 검사합니다.
     * 
     * @param newPassword 새 비밀번호
     */
    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException("새 비밀번호를 입력해주세요.");
        }

        if (newPassword.length() < 8) {
            throw new BadRequestException("비밀번호는 8자리 이상이어야 합니다.");
        }
    }
}