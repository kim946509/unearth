package com.rhoonart.unearth.right_holder.dto;

import jakarta.validation.constraints.NotNull;
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
public class ContractExtendRequestDto {

    @NotNull(message = "새로운 계약 종료일은 필수입니다.")
    private String newEndDate;
}