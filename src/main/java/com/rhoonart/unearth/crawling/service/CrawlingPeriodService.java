package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto.VideoInfo;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.exception.CannotFindSongException;
import com.rhoonart.unearth.song.service.SongInfoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrawlingPeriodService {

    // 크롤링 기간 (30일)
    private static final int CRAWLING_PERIOD_DAYS = 30;

    private final CrawlingPeriodRepository crawlingPeriodRepository;
    private final SongInfoService songInfoService;
    /**
     * 영상 등록시 크롤링 기간을 생성하고 저장합니다.
     * @param dto
     * @return
     */
    public CrawlingPeriod createAndSaveCrawlingPeriod(CrawlingExecuteRequestDto dto){
        // 1. 음원 조회
        SongInfo song = songInfoService.getSongInfoById(dto.getSongId()).orElseThrow(CannotFindSongException::new);

        // 2. 크롤링 시작일과 종료일 자동 계산
        LocalDate startDate = calculateStartDate();
        LocalDate endDate = startDate.plusDays(CRAWLING_PERIOD_DAYS);

        // 3. CrawlingPeriod 생성 및 저장
        CrawlingPeriod crawlingPeriod = CrawlingPeriod.builder()
                .song(song)
                .startDate(startDate)
                .endDate(endDate)
                .isActive(true)
                .channel(dto.getChannel())
                .youtubeTitle(dto.getYoutubeTitle())
                .youtubeUrl(dto.getYoutubeUrl())
                .songOrder(dto.getSongOrder())
                .uploadAt(parseUploadAt(dto.getUploadAt()))
                .build();

        crawlingPeriodRepository.save(crawlingPeriod);

    }

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

    // 크롤링 시작일을 현재 날짜로 설정
    private LocalDate calculateStartDate() {
        return LocalDate.now();
    }

    // 영상 업로드 시점을 LocalDateTime으로 파싱합니다.
    private java.time.LocalDateTime parseUploadAt(String uploadAtStr) {
        if (uploadAtStr == null || uploadAtStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(uploadAtStr);
        } catch (Exception e) {
            return null;
        }
    }

}
