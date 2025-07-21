package com.rhoonart.unearth.user.service;

import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.user.dto.PasswordChangeRequestDto;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.user.exception.UnauthorizedException;
import com.rhoonart.unearth.user.exception.BadRequestException;
import com.rhoonart.unearth.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordChangeService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RightHolderRepository rightHolderRepository;

    /**
     * 비밀번호 변경 처리
     * 1. 사용자 정보 조회회
     * 2 현재 비밀번호 검증
     * 3. 사업자 등록번호 검증
     * 4새 비밀번호 유효성 검증
     * 5. 비밀번호 변경
     */
    @Transactional
    public void changePassword(PasswordChangeRequestDto requestDto, UserDto loginUser) {

        // 1. 사용자 정보 조회
        User user = userRepository.findByUsername(loginUser.getUsername())
                .orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 3. 사업자 등록번호 검증
        validateBusinessRegistrationNumber(user, requestDto.getBusinessRegistrationNumber());

        // 4. 비밀번호 유효성 검증
        validateNewPassword(requestDto);

        // 5 변경
        String encodedNewPassword = passwordEncoder.encode(requestDto.getNewPassword());
        user.changePassword(encodedNewPassword);

        // 6. 데이터베이스에 저장
        userRepository.save(user);
    }

    /**
     * 사업자 등록번호 검증
     */
    private void validateBusinessRegistrationNumber(User user, String businessRegistrationNumber) {

        if (businessRegistrationNumber == null || businessRegistrationNumber.trim().isEmpty()) {
            throw new BadRequestException("사업자 등록번호를 입력해주세요.");
        }

        // 사업자 등록번호가 현재 사용자와 일치하는지 확인
        RightHolder rightHolder = rightHolderRepository.findByUserId(user.getId()).orElseThrow(
                () -> new BadRequestException("사용자에 대한 권리자 정보를 찾을 수 없습니다."));

        if (!businessRegistrationNumber.equals(rightHolder.getBusinessNumber())) {
            throw new BadRequestException("사업자 등록번호가 일치하지 않습니다.");
        }
    }

    /**
     * 새 비밀번호 유효성 검증
     */
    private void validateNewPassword(PasswordChangeRequestDto requestDto) {
        if (!requestDto.isPasswordMatch()) {
            throw new BadRequestException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        if (!requestDto.isValidPassword()) {
            throw new BadRequestException("비밀번호는 8자 이상아어야 합니다.");
        }

        if (requestDto.getCurrentPassword().equals(requestDto.getNewPassword())) {
            throw new BadRequestException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
    }
}