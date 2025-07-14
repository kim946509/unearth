package com.rhoonart.unearth.right_holder.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RightHolderSongListResponseDto {
    private String songId;
    private String rightHolderName;
    private String artistKo;
    private String albumKo;
    private String titleKo;
    private String youtubeUrl;
    private boolean hasCrawlingData;
}