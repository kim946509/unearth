package com.rhoonart.unearth.crawling.dto;

import com.rhoonart.unearth.crawling.entity.CrawlingFailure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingFailureDto {
    private String songId;
    private String artistKo;
    private String titleKo;
    private String albumKo;
    private String artistEn;
    private String titleEn;
    private String albumEn;
    private String youtubeUrl;
    private String melonSongId;
    private String rightHolderName;
    private LocalDateTime failedAt;
    private List<String> failedPlatforms;

    /**
     * CrawlingFailure 엔티티로부터 DTO 생성
     */
    public static CrawlingFailureDto from(CrawlingFailure failure) {
        return CrawlingFailureDto.builder()
                .songId(failure.getSong().getId())
                .artistKo(failure.getSong().getArtistKo())
                .titleKo(failure.getSong().getTitleKo())
                .albumKo(failure.getSong().getAlbumKo())
                .artistEn(failure.getSong().getArtistEn())
                .titleEn(failure.getSong().getTitleEn())
                .albumEn(failure.getSong().getAlbumEn())
                .youtubeUrl(failure.getSong().getYoutubeUrl())
                .melonSongId(failure.getSong().getMelonSongId())
                .rightHolderName(failure.getSong().getRightHolder().getHolderName())
                .failedAt(failure.getFailedAt())
                .failedPlatforms(failure.getFailedPlatformsList())
                .build();
    }
}