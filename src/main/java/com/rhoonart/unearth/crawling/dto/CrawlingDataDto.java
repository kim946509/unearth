package com.rhoonart.unearth.crawling.dto;

import com.rhoonart.unearth.crawling.entity.PlatformType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 크롤링 데이터 DTO
 * 플랫폼별 크롤링 데이터를 담는 기본 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingDataDto {
    // 기본 크롤링 데이터
    private LocalDate date;
    private PlatformType platform;
    private long views;
    private long listeners;
    private long viewsIncrease;
    private long listenersIncrease;

    /**
     * 기본 크롤링 데이터로 DTO 생성
     */
    public static CrawlingDataDto ofBasicData(LocalDate date, PlatformType platform,
            long views, long listeners,
            long viewsIncrease, long listenersIncrease) {
        return CrawlingDataDto.builder()
                .date(date)
                .platform(platform)
                .views(views)
                .listeners(listeners)
                .viewsIncrease(viewsIncrease)
                .listenersIncrease(listenersIncrease)
                .build();
    }
}