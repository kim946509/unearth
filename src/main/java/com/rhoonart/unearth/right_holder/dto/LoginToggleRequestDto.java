package com.rhoonart.unearth.right_holder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginToggleRequestDto {
    private String isLoginEnabled; // 로그인 기능 활성화 여부

    public boolean getIsLoginEnabledAsBoolean() {
        return "true".equalsIgnoreCase(isLoginEnabled);
    }
}