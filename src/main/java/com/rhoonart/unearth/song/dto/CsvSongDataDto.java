package com.rhoonart.unearth.song.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * CSV 곡 데이터 DTO
 */
@Getter
@Builder
public class CsvSongDataDto {
    private String artistKo;
    private String artistEn;
    private String albumKo;
    private String albumEn;
    private String titleKo;
    private String titleEn;
    private String youtubeUrl;
    private String melonSongId;
    private String rightHolderName;

    @Override
    public String toString() {
        return String.format("%s - %s", artistKo, titleKo);
    }
}