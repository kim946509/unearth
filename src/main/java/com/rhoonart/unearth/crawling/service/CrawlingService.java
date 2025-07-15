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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
    private final CrawlingExecuteService crawlingExecuteService;

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

        // 4. 크롤링 실행 (비동기)
        crawlingExecuteService.executeSingleSongCrawling(dto.getSongId());
    }

    /**
     * 크롤링 시작일 계산
     * - 현재 시간이 17시 이전이면 오늘부터 시작
     * - 현재 시간이 17시 이후면 내일부터 시작
     */
    private LocalDate calculateStartDate() {
        // LocalTime now = LocalTime.now();
        // LocalDate today = LocalDate.now();

        // if (now.isBefore(LocalTime.of(CRAWLING_HOUR - 1, 59))) {
        // return today; // 17시 이전이면 오늘부터
        // } else {
        // return today.plusDays(1); // 17시 이후면 내일부터
        // }

        return LocalDate.now();
    }

    public CrawlingDataWithSongInfoDto getCrawlingDataWithFilters(String songId, LocalDate startDate,
            LocalDate endDate, PlatformType platform, int page, int size) {
        // 음원 존재 여부 확인
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        List<CrawlingData> crawlingDataList = crawlingDataRepository.findBySongIdAndDateRange(songId, platform,
                startDate, endDate);

        // 날짜별로 그룹화
        Map<LocalDate, List<CrawlingData>> dataByDate = crawlingDataList.stream()
                .collect(Collectors.groupingBy(data -> data.getCreatedAt().toLocalDate()));

        // 모든 데이터를 처리 (간격 필터링 제거)
        List<CrawlingDataResponseDto> resultList = dataByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate currentDate = entry.getKey();
                    List<CrawlingData> currentDataList = entry.getValue();

                    return currentDataList.stream().map(currentData -> {
                        // 1일 전 데이터 찾기 (간격은 항상 1로 고정)
                        LocalDate previousDate = currentDate.minusDays(1);

                        CrawlingData previousData = dataByDate.get(previousDate) != null
                                ? dataByDate.get(previousDate).stream()
                                        .filter(data -> data.getPlatform() == currentData.getPlatform())
                                        .findFirst()
                                        .orElse(null)
                                : null;

                        long viewsIncrease = 0; // 기본값: 이전 데이터 없음
                        long listenersIncrease = 0; // 기본값: 이전 데이터 없음

                        if (previousData != null) {
                            // 조회수 증가량 계산
                            if (currentData.getViews() == -999 || previousData.getViews() == -999) {
                                viewsIncrease = -999; // 오류 상황
                            } else if (currentData.getViews() == -1 || previousData.getViews() == -1) {
                                viewsIncrease = -1; // 데이터 제공되지 않음
                            } else {
                                viewsIncrease = currentData.getViews() - previousData.getViews();
                            }

                            // 청취자수 증가량 계산
                            if (currentData.getListeners() == -999 || previousData.getListeners() == -999) {
                                listenersIncrease = -999; // 오류 상황
                            } else if (currentData.getListeners() == -1 || previousData.getListeners() == -1) {
                                listenersIncrease = -1; // 데이터 제공되지 않음
                            } else {
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

        // 페이징 처리
        int totalElements = resultList.size();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        // 페이지 범위 계산 (안전한 범위 체크)
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        // 시작 인덱스가 총 개수보다 크면 빈 리스트 반환
        List<CrawlingDataResponseDto> pagedData;
        if (startIndex >= totalElements) {
            pagedData = new ArrayList<>();
        } else {
            pagedData = resultList.subList(startIndex, endIndex);
        }

        return CrawlingDataWithSongInfoDto.builder()
                .songInfo(songInfo)
                .crawlingDataList(pagedData)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * 음원 ID로 음원 정보를 조회합니다.
     * 
     * @param songId 음원 ID
     * @return SongInfo
     */
    public SongInfo getSongInfoById(String songId) {
        return songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));
    }
}