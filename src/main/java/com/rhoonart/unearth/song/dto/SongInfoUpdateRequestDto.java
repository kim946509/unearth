package com.rhoonart.unearth.song.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SongInfoUpdateRequestDto {
    @NotBlank(message = "아티스트명(국문)은 필수입니다.")
    private String artistKo;

    @NotBlank(message = "아티스트명(영문)은 필수입니다.")
    private String artistEn;

    @NotBlank(message = "앨범명(국문)은 필수입니다.")
    private String albumKo;

    @NotBlank(message = "앨범명(영문)은 필수입니다.")
    private String albumEn;

    @NotBlank(message = "트랙명(국문)은 필수입니다.")
    private String titleKo;

    @NotBlank(message = "트랙명(영문)은 필수입니다.")
    private String titleEn;

    @NotBlank(message = "유튜브 공식 URL은 필수입니다.")
    private String youtubeUrl;

    private String melonSongId;

    @NotBlank(message = "권리자명은 필수입니다.")
    private String rightHolderName;
}