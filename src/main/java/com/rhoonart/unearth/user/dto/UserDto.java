package com.rhoonart.unearth.user.dto;

import com.rhoonart.unearth.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "of")
public class UserDto {
    private final String id;
    private final String username;
    private final Role role;
}