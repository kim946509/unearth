package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.crawling.dto.VideoInfoDto;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.entity.YoutubeVideoViewCount;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.crawling.repository.YoutubeVideoViewCountRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.exception.CannotFindSongException;
import com.rhoonart.unearth.song.service.SongInfoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrawlingPeriodService {

    // 크롤링 기간 (30일)
    private static final int CRAWLING_PERIOD_DAYS = 30;

    private final CrawlingPeriodRepository crawlingPeriodRepository;
    private final YoutubeVideoViewCountRepository youtubeVideoViewCountRepository;
    private final SongInfoService songInfoService;

    /**
     * 영상 등록시 크롤링 기간을 생성하고 저장합니다.
     * 
     * @param dto
     * @return
     */
    public CrawlingPeriod createAndSaveCrawlingPeriod(CrawlingExecuteRequestDto dto) {
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
                .youtubeUrl(dto.getYoutubeUrl())
                .youtubeTitle(dto.getYoutubeTitle())
                .channel(dto.getChannel())
                .songOrder(dto.getSongOrder())
                .uploadAt(parseUploadAt(dto.getUploadAt()))
                .isActive(true)
                .build();

        return crawlingPeriodRepository.save(crawlingPeriod);
    }

    /**
     * 특정 날짜가 크롤링 시작일인지 확인합니다.
     */
    public boolean isStartDate(String songId, LocalDate date) {
        return !crawlingPeriodRepository.findBySongIdAndStartDate(songId, date).isEmpty();
    }

    /**
     * 특정 날짜의 영상 정보를 조회합니다. (모든 날짜에 대해 조회)
     */
    public List<VideoInfoDto> getVideoInfosForDate(String songId, LocalDate date) {
        // 해당 날짜가 포함된 모든 크롤링 기간을 조회
        List<CrawlingPeriod> periods = crawlingPeriodRepository.findBySongIdAndDateRange(songId, date);

        return periods.stream()
                .map(period -> {
                    // 해당 날짜의 조회수 조회
                    Integer viewCount = null; // 기본값: 크롤링되지 않음
                    try {
                        YoutubeVideoViewCount viewCountData = youtubeVideoViewCountRepository
                                .findByCrawlingPeriodIdAndDate(period.getId(), date)
                                .orElse(null);
                        if (viewCountData != null) {
                            viewCount = viewCountData.getViewCount();
                        }
                    } catch (Exception e) {
                        // 조회수 조회 실패 시 기본값 유지
                    }

                    return VideoInfoDto.builder()
                            .channel(period.getChannel())
                            .youtubeTitle(period.getYoutubeTitle())
                            .youtubeUrl(period.getYoutubeUrl())
                            .songOrder(period.getSongOrder())
                            .uploadAt(period.getUploadAt())
                            .viewCount(viewCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜 범위의 모든 영상 정보를 일괄 조회합니다. (N+1 문제 해결)
     */
    public Map<LocalDate, List<VideoInfoDto>> getVideoInfosForDateRange(String songId, LocalDate startDate,
            LocalDate endDate) {
        // 1. 날짜 범위에 해당하는 모든 크롤링 기간을 일괄 조회 (쿼리 1)
        List<CrawlingPeriod> periods = crawlingPeriodRepository.findBySongIdAndDateRangeBatch(songId, startDate,
                endDate);

        if (periods.isEmpty()) {
            return new HashMap<>();
        }

        // 2. 크롤링 기간 ID 목록 추출
        List<String> periodIds = periods.stream()
                .map(CrawlingPeriod::getId)
                .collect(Collectors.toList());

        // 3. 해당 기간의 모든 YouTube 조회수 데이터를 일괄 조회 (쿼리 2)
        List<YoutubeVideoViewCount> viewCounts = youtubeVideoViewCountRepository
                .findByCrawlingPeriodIdsAndDateRange(periodIds, startDate, endDate);

        // 4. 조회수 데이터를 Map으로 변환 (periodId + date -> viewCount)
        Map<String, Integer> viewCountMap = viewCounts.stream()
                .collect(Collectors.toMap(
                        vc -> vc.getCrawlingPeriod().getId() + "_" + vc.getDate().toString(),
                        YoutubeVideoViewCount::getViewCount,
                        (existing, replacement) -> existing // 중복 시 기존 값 유지
                ));

        // 5. 날짜별로 영상 정보 그룹화
        Map<LocalDate, List<VideoInfoDto>> result = new HashMap<>();

        // 각 날짜에 대해 해당하는 영상 정보 생성
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<VideoInfoDto> videoInfosForDate = new ArrayList<>();

            for (CrawlingPeriod period : periods) {
                // 해당 날짜가 크롤링 기간에 포함되는지 확인
                if (!currentDate.isBefore(period.getStartDate()) && !currentDate.isAfter(period.getEndDate())) {
                    // 조회수 데이터 조회
                    String viewCountKey = period.getId() + "_" + currentDate.toString();
                    Integer viewCount = viewCountMap.get(viewCountKey);

                    VideoInfoDto videoInfo = VideoInfoDto.builder()
                            .channel(period.getChannel())
                            .youtubeTitle(period.getYoutubeTitle())
                            .youtubeUrl(period.getYoutubeUrl())
                            .songOrder(period.getSongOrder())
                            .uploadAt(period.getUploadAt())
                            .viewCount(viewCount) // null이면 크롤링되지 않음을 의미
                            .build();

                    videoInfosForDate.add(videoInfo);
                }
            }

            if (!videoInfosForDate.isEmpty()) {
                result.put(currentDate, videoInfosForDate);
            }

            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    // 크롤링 시작일을 현재 날짜로 설정
    private LocalDate calculateStartDate() {
        return LocalDate.now();
    }

    // 영상 업로드 시점을 LocalDateTime으로 파싱합니다.
    private LocalDateTime parseUploadAt(String uploadAtStr) {
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