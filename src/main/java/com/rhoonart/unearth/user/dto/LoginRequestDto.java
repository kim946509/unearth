package com.rhoonart.unearth.user.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "of")
public class LoginRequestDto {
    private final String username;
    private final String password;
}