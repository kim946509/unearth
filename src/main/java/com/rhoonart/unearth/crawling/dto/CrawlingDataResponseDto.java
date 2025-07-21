package com.rhoonart.unearth.crawling.dto;

import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.song.entity.SongInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingDataResponseDto {
    private LocalDate date;
    private PlatformType platform;
    private long views;
    private long listeners;
    private long viewsIncrease; // 전날 대비 조회수 증가량 (-1: 이전 데이터 없음, -999: 오류)
    private long listenersIncrease; // 전날 대비 청취자수 증가량 (-1: 이전 데이터 없음, -999: 오류)

    // 영상 정보 (startDate인 날에만 표시)
    private List<VideoInfo> videoInfos;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VideoInfo {
        private String channel;
        private String youtubeTitle;
        private String youtubeUrl;
        private int songOrder;
        private LocalDateTime uploadAt;
    }

    /**
     * 날짜별로 그룹화된 크롤링 데이터를 위한 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DateGroupedData {
        private LocalDate date;
        private List<VideoInfo> videoInfos;
        private List<PlatformData> platformDataList;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class PlatformData {
            private PlatformType platform;
            private long views;
            private long listeners;
            private long viewsIncrease;
            private long listenersIncrease;
        }
    }
}