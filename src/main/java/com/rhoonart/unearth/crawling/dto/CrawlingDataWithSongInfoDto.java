package com.rhoonart.unearth.crawling.dto;

import com.rhoonart.unearth.song.entity.SongInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 음원 정보와 크롤링 데이터를 함께 담는 DTO
 * 날짜별로 그룹화된 크롤링 데이터 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingDataWithSongInfoDto {
    // 음원 정보
    private SongInfo songInfo;

    // 날짜별로 그룹화된 크롤링 데이터 리스트
    private List<DateGroupedCrawlingDataDto> groupedDataList;

    // 페이지네이션 정보
    private PageInfoDto pageInfo;

    /**
     * 음원 정보와 그룹화된 크롤링 데이터로 생성
     */
    public static CrawlingDataWithSongInfoDto of(SongInfo songInfo, List<DateGroupedCrawlingDataDto> groupedDataList) {
        return CrawlingDataWithSongInfoDto.builder()
                .songInfo(songInfo)
                .groupedDataList(groupedDataList)
                .build();
    }

    /**
     * 페이지네이션 정보 추가
     */
    public CrawlingDataWithSongInfoDto withPageInfo(PageInfoDto pageInfo) {
        this.pageInfo = pageInfo;
        return this;
    }
}