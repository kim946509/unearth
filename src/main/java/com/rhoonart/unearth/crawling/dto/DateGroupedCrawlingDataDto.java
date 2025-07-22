package com.rhoonart.unearth.crawling.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * 날짜별로 그룹화된 크롤링 데이터 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DateGroupedCrawlingDataDto {
    private LocalDate date;
    private List<CrawlingDataDto> dataList; // 해당 날짜의 플랫폼별 데이터들
    private List<VideoInfoDto> videoInfos; // 해당 날짜의 영상 정보들 (startDate인 경우에만)

    /**
     * 해당 날짜에 데이터가 있는지 확인
     */
    public boolean hasData() {
        return dataList != null && !dataList.isEmpty();
    }

    /**
     * 해당 날짜에 영상 정보가 있는지 확인
     */
    public boolean hasVideoInfo() {
        return videoInfos != null && !videoInfos.isEmpty();
    }

    /**
     * 플랫폼 데이터 개수 반환 (rowspan 계산용)
     */
    public int getDataListSize() {
        return dataList != null ? dataList.size() : 0;
    }
}