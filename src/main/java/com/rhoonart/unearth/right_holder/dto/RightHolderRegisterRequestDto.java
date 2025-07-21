package com.rhoonart.unearth.right_holder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class RightHolderRegisterRequestDto {
    @NotNull(message = "계약 시작일은 필수입니다.")
    private LocalDate contractStart;

    @NotNull(message = "계약 종료일은 필수입니다.")
    private LocalDate contractEnd;

    @NotBlank(message = "사업자 번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "사업자 번호는 숫자만 입력 가능합니다.")
    private String businessNumber;

    @NotNull(message = "사업자 타입은 필수입니다.")
    private String holderType; // Enum(HolderType)으로 변환 필요

    @NotBlank(message = "사업자 이름은 필수입니다.")
    private String holderName;

    @NotBlank(message = "로그인 아이디는 필수입니다.")
    private String username;

    @NotBlank(message = "로그인 비밀번호는 필수입니다.")
    private String password;
}