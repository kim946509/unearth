package com.rhoonart.unearth.crawling.dto;

/**
 * CSV 다운로드를 위한 DTO
 */
public class CrawlingCsvDownloadDto {
    private final String csvData;
    private final String filename;

    public CrawlingCsvDownloadDto(String csvData, String filename) {
        this.csvData = csvData;
        this.filename = filename;
    }

    public String getCsvData() {
        return csvData;
    }

    public String getFilename() {
        return filename;
    }
}