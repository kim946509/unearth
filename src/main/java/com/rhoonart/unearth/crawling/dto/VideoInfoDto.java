package com.rhoonart.unearth.crawling.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 영상 정보 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoInfoDto {
    private String channel;
    private String youtubeTitle;
    private String youtubeUrl;
    private int songOrder;
    private LocalDateTime uploadAt;
    private Integer viewCount; // YouTube 조회수 (null: 크롤링되지 않음, -999: 크롤링 실패)
}