package com.rhoonart.unearth.crawling.dto;

import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.song.entity.SongInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingDataResponseDto {
    private LocalDate date;
    private PlatformType platform;
    private long views;
    private long listeners;
    private long viewsIncrease; // 전날 대비 조회수 증가량 (0: 이전 데이터 없음, -999: 오류)
    private long listenersIncrease; // 전날 대비 청취자수 증가량 (0: 이전 데이터 없음, -999: 오류)
}