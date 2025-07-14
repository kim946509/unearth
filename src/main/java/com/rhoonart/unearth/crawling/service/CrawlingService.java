package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataWithSongInfoDto;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrawlingService {

    // 크롤링 실행 시간 (17시)
    private static final int CRAWLING_HOUR = 17;
    // 크롤링 기간 (30일)
    private static final int CRAWLING_PERIOD_DAYS = 30;

    private final CrawlingPeriodRepository crawlingPeriodRepository;
    private final CrawlingDataRepository crawlingDataRepository;
    private final SongInfoRepository songInfoRepository;

    @Transactional
    public void executeCrawling(CrawlingExecuteRequestDto dto) {
        // 1. 음원 조회
        SongInfo song = songInfoRepository.findById(dto.getSongId())
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

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
                .song_order(dto.getSongOrder())
                .build();

        crawlingPeriodRepository.save(crawlingPeriod);
    }

    /**
     * 크롤링 시작일 계산
     * - 현재 시간이 17시 이전이면 오늘부터 시작
     * - 현재 시간이 17시 이후면 내일부터 시작
     */
    private LocalDate calculateStartDate() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        if (now.isBefore(LocalTime.of(CRAWLING_HOUR - 1, 59))) {
            return today; // 17시 이전이면 오늘부터
        } else {
            return today.plusDays(1); // 17시 이후면 내일부터
        }
    }

    public CrawlingDataWithSongInfoDto getCrawlingDataWithFilters(String songId, LocalDate startDate,
            LocalDate endDate,
            PlatformType platform, Integer intervalDays) {
        // 음원 존재 여부 확인
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        List<CrawlingData> crawlingDataList = crawlingDataRepository.findBySongIdAndDateRange(songId, platform,
                startDate, endDate);

        // 날짜별로 그룹화
        Map<LocalDate, List<CrawlingData>> dataByDate = crawlingDataList.stream()
                .collect(Collectors.groupingBy(data -> data.getCreatedAt().toLocalDate()));

        // 간격 설정 (null이면 1로 설정)
        int interval = intervalDays != null ? intervalDays : 1;

        // 간격 적용 및 증가량 계산
        List<CrawlingDataResponseDto> resultList = dataByDate.entrySet().stream()
                .filter(entry -> {
                    // 간격에 맞는 날짜만 필터링 (시작일부터 간격만큼 건너뛰면서)
                    long daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, entry.getKey());
                    return daysFromStart % interval == 0;
                })
                .map(entry -> {
                    LocalDate currentDate = entry.getKey();
                    List<CrawlingData> currentDataList = entry.getValue();

                    return currentDataList.stream().map(currentData -> {
                        // 간격만큼 이전 날짜의 데이터 찾기
                        LocalDate previousDate = currentDate.minusDays(interval);

                        CrawlingData previousData = dataByDate.get(previousDate) != null
                                ? dataByDate.get(previousDate).stream()
                                        .filter(data -> data.getPlatform() == currentData.getPlatform())
                                        .findFirst()
                                        .orElse(null)
                                : null;

                        long viewsIncrease = 0;
                        long listenersIncrease = 0;

                        if (previousData != null) {
                            // 특수 값 처리: -1(데이터 없음), -999(오류)는 증가량 계산에서 제외
                            if (currentData.getViews() != -1 && currentData.getViews() != -999 &&
                                    previousData.getViews() != -1 && previousData.getViews() != -999) {
                                viewsIncrease = currentData.getViews() - previousData.getViews();
                            }

                            if (currentData.getListeners() != -1 && currentData.getListeners() != -999 &&
                                    previousData.getListeners() != -1 && previousData.getListeners() != -999) {
                                listenersIncrease = currentData.getListeners() - previousData.getListeners();
                            }
                        }

                        return CrawlingDataResponseDto.builder()
                                .date(currentDate)
                                .platform(currentData.getPlatform())
                                .views(currentData.getViews())
                                .listeners(currentData.getListeners())
                                .viewsIncrease(viewsIncrease)
                                .listenersIncrease(listenersIncrease)
                                .build();
                    }).collect(Collectors.toList());
                })
                .flatMap(List::stream)
                .sorted((a, b) -> {
                    int dateCompare = a.getDate().compareTo(b.getDate());
                    if (dateCompare != 0)
                        return dateCompare;
                    return a.getPlatform().compareTo(b.getPlatform());
                })
                .collect(Collectors.toList());

        return CrawlingDataWithSongInfoDto.builder()
                .songInfo(songInfo)
                .crawlingDataList(resultList)
                .build();
    }
}