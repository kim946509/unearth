package com.rhoonart.unearth.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 관리자 비밀번호 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPasswordChangeRequestDto {
    private String currentPassword;
    private String newPassword;
}