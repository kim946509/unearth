package com.rhoonart.unearth.crawling.dto;

import com.rhoonart.unearth.song.entity.SongInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingDataWithSongInfoDto {
    private SongInfo songInfo;
    private List<CrawlingDataResponseDto> crawlingDataList;

    // 날짜별로 그룹화된 데이터 (새로운 형식)
    private List<CrawlingDataResponseDto.DateGroupedData> groupedDataList;

    // 페이지네이션 정보
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;
}