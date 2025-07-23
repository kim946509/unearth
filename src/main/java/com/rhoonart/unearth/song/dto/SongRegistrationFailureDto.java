package com.rhoonart.unearth.song.dto;

/**
 * 곡 등록 실패 데이터 DTO
 */
public class SongRegistrationFailureDto {
    private final CsvSongDataDto csvData;
    private final String reason;

    public SongRegistrationFailureDto(CsvSongDataDto csvData, String reason) {
        this.csvData = csvData;
        this.reason = reason;
    }

    public CsvSongDataDto getCsvData() {
        return csvData;
    }

    public String getReason() {
        return reason;
    }
}