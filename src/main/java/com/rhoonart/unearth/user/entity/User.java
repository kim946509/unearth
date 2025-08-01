package com.rhoonart.unearth.user.entity;

import com.rhoonart.unearth.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    @Id
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false, unique = true)
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_login_enabled", nullable = true)
    private boolean isLoginEnabled;

    public void updateUsername(String newUsername) {
        this.username = newUsername;
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 로그인 활성화 상태 변경
     */
    public void updateLoginEnabled(boolean isLoginEnabled) {
        this.isLoginEnabled = isLoginEnabled;
    }
}