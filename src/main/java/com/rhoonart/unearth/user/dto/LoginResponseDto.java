package com.rhoonart.unearth.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "of")
public class LoginResponseDto {
    private final UserDto userDto;
}