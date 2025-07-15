package com.rhoonart.unearth.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "of")
public class LoginResponseDto {
    private final UserDto userDto;
    private final String rightHolderId; // 권리자 ID (권리자인 경우에만 값이 있음)
}