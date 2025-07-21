package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto.VideoInfo;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrawlingPeriodService {

    private final CrawlingPeriodRepository crawlingPeriodRepository;

    /**
     * 특정 날짜가 크롤링 시작일인지 확인합니다.
     */
    public boolean isStartDate(String songId, LocalDate date) {
        return !crawlingPeriodRepository.findBySongIdAndStartDate(songId, date).isEmpty();
    }

    /**
     * 특정 날짜의 영상 정보를 조회합니다.
     */
    public List<VideoInfo> getVideoInfosForDate(String songId, LocalDate date) {
        List<CrawlingPeriod> periods = crawlingPeriodRepository.findBySongIdAndStartDate(songId, date);

        return periods.stream()
                .map(period -> CrawlingDataResponseDto.VideoInfo.builder()
                        .channel(period.getChannel())
                        .youtubeTitle(period.getYoutubeTitle())
                        .youtubeUrl(period.getYoutubeUrl())
                        .songOrder(period.getSongOrder())
                        .uploadAt(period.getUploadAt())
                        .build())
                .collect(Collectors.toList());
    }
}
