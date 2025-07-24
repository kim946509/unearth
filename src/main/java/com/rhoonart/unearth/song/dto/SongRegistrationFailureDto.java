package com.rhoonart.unearth.song.dto;

/**
 * 곡 등록 실패 데이터 DTO
 */
public record SongRegistrationFailureDto(CsvSongDataDto csvData, String reason) {

}