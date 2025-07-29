package com.rhoonart.unearth.global.initializer;

import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${superadmin.username}")
    private String superAdminUsername;

@Value("${superadmin.password}")
    private String superAdminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername(superAdminUsername)) {
            User superAdmin = User.builder()
                    .username(superAdminUsername)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .role(Role.SUPER_ADMIN)
                    .isLoginEnabled(true)
                    .build();

            userRepository.save(superAdmin);
            log.info("Super Admin 계정이 생성되었습니다: {}", superAdmin.getUsername());
        } else {
            log.info("Super Admin 계정이 이미 존재합니다: {}", superAdminUsername);
        }
    }
}