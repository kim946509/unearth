package com.rhoonart.unearth.song.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class SongBulkRegisterResponseDto {

    private final int totalCount;
    private final int successCount;
    private final int duplicateCount;
    private final int failureCount;

    private final List<SongInfo> successList;
    private final List<SongInfo> duplicateList;
    private final List<FailureInfo> failureList;

    /**
     * SongBulkRegisterResultDto의 결과를 ResponseDto로 변환
     */
    public static SongBulkRegisterResponseDto from(SongBulkRegisterResultDto result) {
        return SongBulkRegisterResponseDto.builder()
                .totalCount(result.getTotalCount())
                .successCount(result.getSuccessCount())
                .duplicateCount(result.getDuplicateCount())
                .failureCount(result.getFailureCount())
                .successList(convertToSongInfoList(result.getSuccessList()))
                .duplicateList(convertToSongInfoList(result.getDuplicateList()))
                .failureList(convertToFailureInfoList(result.getFailureList()))
                .build();
    }

    private static List<SongInfo> convertToSongInfoList(List<CsvSongDataDto> csvDataList) {
        return csvDataList.stream()
                .map(csvData -> SongInfo.builder()
                        .artistKo(csvData.getArtistKo())
                        .artistEn(csvData.getArtistEn())
                        .albumKo(csvData.getAlbumKo())
                        .albumEn(csvData.getAlbumEn())
                        .titleKo(csvData.getTitleKo())
                        .titleEn(csvData.getTitleEn())
                        .youtubeUrl(csvData.getYoutubeUrl())
                        .melonSongId(csvData.getMelonSongId())
                        .rightHolderName(csvData.getRightHolderName())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<FailureInfo> convertToFailureInfoList(
            List<SongRegistrationFailureDto> failureDataList) {
        return failureDataList.stream()
                .map(failureData -> FailureInfo.builder()
                        .artistKo(failureData.getCsvData().getArtistKo())
                        .artistEn(failureData.getCsvData().getArtistEn())
                        .albumKo(failureData.getCsvData().getAlbumKo())
                        .albumEn(failureData.getCsvData().getAlbumEn())
                        .titleKo(failureData.getCsvData().getTitleKo())
                        .titleEn(failureData.getCsvData().getTitleEn())
                        .youtubeUrl(failureData.getCsvData().getYoutubeUrl())
                        .melonSongId(failureData.getCsvData().getMelonSongId())
                        .rightHolderName(failureData.getCsvData().getRightHolderName())
                        .reason(failureData.getReason())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 성공한 곡 정보 DTO
     */
    @Getter
    @Builder
    public static class SongInfo {
        private final String artistKo;
        private final String artistEn;
        private final String albumKo;
        private final String albumEn;
        private final String titleKo;
        private final String titleEn;
        private final String youtubeUrl;
        private final String melonSongId;
        private final String rightHolderName;

        @Override
        public String toString() {
            return String.format("%s - %s", artistKo, titleKo);
        }
    }

    /**
     * 실패한 곡 정보 DTO (실패 이유 포함)
     */
    @Getter
    @Builder
    public static class FailureInfo {
        private final String artistKo;
        private final String artistEn;
        private final String albumKo;
        private final String albumEn;
        private final String titleKo;
        private final String titleEn;
        private final String youtubeUrl;
        private final String melonSongId;
        private final String rightHolderName;
        private final String reason;

        @Override
        public String toString() {
            return String.format("%s - %s (실패: %s)", artistKo, titleKo, reason);
        }
    }
}