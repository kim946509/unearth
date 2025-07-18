package com.rhoonart.unearth.crawling.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
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
public class CrawlingExecuteRequestDto {

    @NotBlank(message = "음원 ID는 필수입니다.")
    private String songId;

    @NotBlank(message = "채널명은 필수입니다.")
    private String channel;

    @NotBlank(message = "유튜브 영상 제목은 필수입니다.")
    private String youtubeTitle;

    @NotBlank(message = "유튜브 영상 URL은 필수입니다.")
    private String youtubeUrl;

    @NotNull(message = "수록 순서는 필수입니다.")
    @Min(value = 1, message = "수록 순서는 1 이상이어야 합니다.")
    private Integer songOrder;

    @NotNull(message = "영상 업로드 시점은 필수입니다.")
    private String uploadAt;
}