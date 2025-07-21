package com.rhoonart.unearth.user.dto;

import lombok.Getter;

@Getter
public class PasswordChangeRequestDto {
    private final String currentPassword;
    private final String newPassword;
    private final String confirmPassword;
    private final String businessRegistrationNumber;

    public PasswordChangeRequestDto(String currentPassword, String newPassword,
            String confirmPassword, String businessRegistrationNumber) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
        this.businessRegistrationNumber = businessRegistrationNumber;
    }

    /**
     * 정적 팩토리 메서드로 DTO 생성
     */
    public static PasswordChangeRequestDto of(String currentPassword, String newPassword,
            String confirmPassword, String businessRegistrationNumber) {
        return new PasswordChangeRequestDto(currentPassword, newPassword, confirmPassword, businessRegistrationNumber);
    }

    /**
     * 새 비밀번호와 확인 비밀번호가 일치하는지 검증
     */
    public boolean isPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

    /**
     * 비밀번호 유효성 검증
     */
    public boolean isValidPassword() {
        return newPassword != null && newPassword.length() >= 8;
    }
}