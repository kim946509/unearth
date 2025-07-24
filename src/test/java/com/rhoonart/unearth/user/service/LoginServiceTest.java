package com.rhoonart.unearth.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.service.RightHolderUtilService;
import com.rhoonart.unearth.user.dto.LoginRequestDto;
import com.rhoonart.unearth.user.dto.LoginResponseDto;
import com.rhoonart.unearth.user.entity.Role;
import com.rhoonart.unearth.user.entity.User;
import com.rhoonart.unearth.user.exception.LoginException;
import com.rhoonart.unearth.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RightHolderUtilService rightHolderUtilService;

    @InjectMocks
    private LoginService loginService;

    @Test
    @DisplayName("올바른 아이디와 비밀번호로 로그인하면 UserDto를 반환한다")
    void loginSuccess(){

        //Given
        User user = User.builder()
                .id("testUserId")
                .username("testuser")
                .password("encodedPassowrd")
                .role(Role.ADMIN)
                .isLoginEnabled(true)
                .build();

        LoginRequestDto request = LoginRequestDto.of("testuser","plainPassword");

        given(userRepository.findByUsername("testuser"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("plainPassword","encodedPassowrd"))
                .willReturn(true);

        //When
        LoginResponseDto response = loginService.login(request);

        //Then
        assertThat(response).isNotNull();
        assertThat(response.getUserDto().getUsername()).isEqualTo("testuser");
        assertThat(response.getUserDto().getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getUserDto().getId()).isEqualTo("testUserId");
        assertThat(response.getRightHolderId()).isNull(); // RightHolderId는 null로 설정됨
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 로그인하면 LoginException을 발생시킨다")
    void loginWithNonExistentUsername(){

        // given
        LoginRequestDto request = LoginRequestDto.of("notExistUser","plainPassword");

        given(userRepository.findByUsername("notExistUser"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(LoginException.class)
                .hasMessage("존재하지 않는 아이디입니다.");
    }
    
    @Test
    @DisplayName("비밀번호가 일치하지 않으면 LoginException을 발생시킨다")
    public void shouldThrowLoginExceptionWhenPasswordDoesNotMatch() {
        // given
        LoginRequestDto request = LoginRequestDto.of("testuser","plainPassword");

        User user = User.builder()
                .id("testUserId")
                .username("testuser")
                .password("encodedPassowrd")
                .role(Role.ADMIN)
                .isLoginEnabled(true)
                .build();

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("plainPassword", "encodedPassowrd"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(LoginException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비활성화된 계정으로 로그인하면 LoginException을 발생시킨다")
    public void shouldThrownLoginExceptionWhenIsLoginEnabledFalse(){
        // given
        LoginRequestDto request = LoginRequestDto.of("testuser","plainPassword");
        User user = User.builder()
                .id("testUserId")
                .username("testuser")
                .password("encodedPassowrd")
                .role(Role.ADMIN)
                .isLoginEnabled(false) // 비활성화된 계정
                .build();

        given(userRepository.findByUsername("testuser"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("plainPassword", "encodedPassowrd"))
                .willReturn(true);
        // when & then
        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(LoginException.class)
                .hasMessage("비활성화된 계정입니다. 관리자에게 문의하세요.");

    }

    @Test
    @DisplayName("RightHolder가 아닌 경우 RightHolderId는 null로 설정된다")
    public void shoulRightHolderIdBeNullWhenNotRightHolder() {
        // given
        User user = User.builder()
                .id("testUserId")
                .username("testuser")
                .password("encodedPassowrd")
                .role(Role.ADMIN) // RightHolder가 아님
                .isLoginEnabled(true)
                .build();

        LoginRequestDto request = LoginRequestDto.of("testuser","plainPassword");

        given(userRepository.findByUsername("testuser"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("plainPassword", "encodedPassowrd"))
                .willReturn(true);

        // when
        LoginResponseDto response = loginService.login(request);

        // then
        assertThat(response.getRightHolderId()).isNull();
    }

    @Test
    @DisplayName("RightHolder인 경우 RightHolderId가 설정된다")
    public void shouldSetRightHolderIdWhenUserIsRightHolder(){
        // given
        User user = User.builder()
                .id("testUserId")
                .username("testuser")
                .password("encodedPassword")
                .role(Role.RIGHT_HOLDER)
                .isLoginEnabled(true)
                .build();

        RightHolder rightHolder = RightHolder.builder()
                        .id("rightHolderId")
                        .holderName("rightHolderName")
                        .holderType(HolderType.개인)
                        .user(user)
                        .contractStart(LocalDate.now())
                        .contractEnd(LocalDate.now())
                        .businessNumber("000000")
                        .build();

        LoginRequestDto request = LoginRequestDto.of(user.getUsername(),user.getPassword());

        given(userRepository.findByUsername(user.getUsername()))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches(user.getPassword(), user.getPassword()))
                .willReturn(true);
        given(rightHolderUtilService.findByUserId("testUserId"))
                .willReturn(rightHolder);

        // when
        LoginResponseDto response = loginService.login(request);

        // then
        assertThat(response.getRightHolderId()).isEqualTo(rightHolder.getId());
    }

}
