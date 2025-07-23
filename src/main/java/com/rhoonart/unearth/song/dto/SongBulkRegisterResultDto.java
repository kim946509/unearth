package com.rhoonart.unearth.song.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 일괄 등록 결과 DTO
 */
public class SongBulkRegisterResultDto {
    private final List<CsvSongDataDto> successList = new ArrayList<>();
    private final List<CsvSongDataDto> duplicateList = new ArrayList<>();
    private final List<SongRegistrationFailureDto> failureList = new ArrayList<>();

    public void addSuccess(CsvSongDataDto csvData) {
        successList.add(csvData);
    }

    public void addDuplicate(CsvSongDataDto csvData) {
        duplicateList.add(csvData);
    }

    public void addFailure(CsvSongDataDto csvData, String reason) {
        failureList.add(new SongRegistrationFailureDto(csvData, reason));
    }

    public int getSuccessCount() {
        return successList.size();
    }

    public int getDuplicateCount() {
        return duplicateList.size();
    }

    public int getFailureCount() {
        return failureList.size();
    }

    public int getTotalCount() {
        return getSuccessCount() + getDuplicateCount() + getFailureCount();
    }

    public List<CsvSongDataDto> getSuccessList() {
        return new ArrayList<>(successList);
    }

    public List<CsvSongDataDto> getDuplicateList() {
        return new ArrayList<>(duplicateList);
    }

    public List<SongRegistrationFailureDto> getFailureList() {
        return new ArrayList<>(failureList);
    }
}