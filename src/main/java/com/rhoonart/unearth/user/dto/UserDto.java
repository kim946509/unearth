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
    private final String rightHolderId; // 권리자 ID (권리자가 아닌 경우 null)

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    public boolean isRightHolder() {
        return role == Role.RIGHT_HOLDER;
    }

    public String getRightHolderId() {
        return rightHolderId;
    }
}