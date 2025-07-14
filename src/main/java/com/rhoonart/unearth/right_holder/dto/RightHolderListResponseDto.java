package com.rhoonart.unearth.right_holder.dto;

import com.rhoonart.unearth.right_holder.entity.HolderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "of")
public class RightHolderListResponseDto {
    private final String holderId;
    private final HolderType holderType;
    private final String holderName;
    private final LocalDate contractStart;
    private final LocalDate contractEnd;
    private final int songCount;
}