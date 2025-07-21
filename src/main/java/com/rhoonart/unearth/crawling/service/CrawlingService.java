package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataWithSongInfoDto;
import com.rhoonart.unearth.crawling.dto.CrawlingFailureDto;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingFailureRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final CrawlingFailureRepository crawlingFailureRepository;
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
                .songOrder(dto.getSongOrder())
                .uploadAt(parseUploadAt(dto.getUploadAt()))
                .build();

        crawlingPeriodRepository.save(crawlingPeriod);

        // 4. 크롤링 실행 (비동기)
        crawlingExecuteService.executeSingleSongCrawling(song.getId());
    }

    @Transactional
    public void executeCrawlingOnly(String songId) {
        // 음원 조회
        SongInfo song = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 크롤링 실행 (비동기)
        crawlingExecuteService.executeSingleSongCrawling(song.getId());
    }

    private LocalDate calculateStartDate() {
        return LocalDate.now();
    }

    private java.time.LocalDateTime parseUploadAt(String uploadAtStr) {
        if (uploadAtStr == null || uploadAtStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDateTime.parse(uploadAtStr);
        } catch (Exception e) {
            log.warn("영상 업로드 시점 파싱 실패: {}", uploadAtStr, e);
            return null;
        }
    }

    public CrawlingDataWithSongInfoDto getCrawlingDataWithFilters(String songId, LocalDate startDate,
            LocalDate endDate, PlatformType platform, int page, int size) {
        // 음원 존재 여부 확인
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 디버깅: CrawlingPeriod 데이터 확인
        long periodCount = crawlingPeriodRepository.countBySongId(songId);

        if (periodCount > 0) {
            List<CrawlingPeriod> allPeriods = crawlingPeriodRepository.findAllBySongId(songId);
        }

        // 1. Pageable 생성 (페이지는 0부터 시작하므로 -1)
        Pageable pageable = PageRequest.of(page - 1, size);

        // 2. DB 레벨에서 페이징된 데이터 조회
        Page<CrawlingData> pagedResult = crawlingDataRepository.findPagedCrawlingData(
                songId, platform, startDate, endDate, pageable);

        // 3. 날짜별로 그룹화
        Map<LocalDate, List<CrawlingData>> dataByDate = pagedResult.getContent().stream()
                .collect(Collectors.groupingBy(data -> data.getCreatedAt().toLocalDate()));

        // 4. 날짜별로 그룹화된 데이터 생성
        List<CrawlingDataResponseDto.DateGroupedData> groupedDataList = new ArrayList<>();

        for (Map.Entry<LocalDate, List<CrawlingData>> entry : dataByDate.entrySet()) {
            LocalDate currentDate = entry.getKey();
            List<CrawlingData> currentDataList = entry.getValue();

            // 영상 정보 조회 (startDate인 날에만)
            List<CrawlingDataResponseDto.VideoInfo> videoInfos = new ArrayList<>();
            boolean isStartDate = isStartDate(songId, currentDate);
            log.debug("날짜 {}는 시작일인가? {}", currentDate, isStartDate);

            if (isStartDate) {
                videoInfos = getVideoInfosForDate(songId, currentDate);
                log.debug("날짜 {}의 영상 정보 개수: {}", currentDate, videoInfos.size());
            }

            // 플랫폼별 데이터 생성
            List<CrawlingDataResponseDto.DateGroupedData.PlatformData> platformDataList = new ArrayList<>();

            for (CrawlingData currentData : currentDataList) {
                // 이전날 데이터를 DB에서 직접 조회
                LocalDate previousDate = currentDate.minusDays(1);
                Optional<CrawlingData> previousDataOpt = crawlingDataRepository
                        .findBySongIdAndPlatformAndDate(songId, currentData.getPlatform(), previousDate);

                long viewsIncrease = -1; // 기본값: 이전 데이터 없음
                long listenersIncrease = -1; // 기본값: 이전 데이터 없음

                if (previousDataOpt.isPresent()) {
                    CrawlingData previousData = previousDataOpt.get();

                    // 조회수 증가량 계산
                    if (currentData.getViews() == -999 || previousData.getViews() == -999) {
                        viewsIncrease = -1; // 오류 데이터가 섞여있어 제공되지 않음
                    } else if (currentData.getViews() == -1 || previousData.getViews() == -1) {
                        viewsIncrease = -1; // 기본적으로 데이터 제공되지 않음
                    } else {
                        viewsIncrease = currentData.getViews() - previousData.getViews();
                    }

                    // 청취자수 증가량 계산
                    if (currentData.getListeners() == -999 || previousData.getListeners() == -999) {
                        listenersIncrease = -1; // 오류 데이터가 섞여있어 제공되지 않음
                    } else if (currentData.getListeners() == -1 || previousData.getListeners() == -1) {
                        listenersIncrease = -1; // 기본적으로 데이터 제공되지 않음
                    } else {
                        listenersIncrease = currentData.getListeners() - previousData.getListeners();
                    }
                }

                CrawlingDataResponseDto.DateGroupedData.PlatformData platformData = CrawlingDataResponseDto.DateGroupedData.PlatformData
                        .builder()
                        .platform(currentData.getPlatform())
                        .views(currentData.getViews())
                        .listeners(currentData.getListeners())
                        .viewsIncrease(viewsIncrease)
                        .listenersIncrease(listenersIncrease)
                        .build();

                platformDataList.add(platformData);
            }

            CrawlingDataResponseDto.DateGroupedData groupedData = CrawlingDataResponseDto.DateGroupedData.builder()
                    .date(currentDate)
                    .videoInfos(videoInfos)
                    .platformDataList(platformDataList)
                    .build();

            groupedDataList.add(groupedData);
        }

        List<CrawlingDataResponseDto> resultList = new ArrayList<>();
        for (CrawlingDataResponseDto.DateGroupedData groupedData : groupedDataList) {
            for (CrawlingDataResponseDto.DateGroupedData.PlatformData platformData : groupedData
                    .getPlatformDataList()) {
                CrawlingDataResponseDto dto = CrawlingDataResponseDto.builder()
                        .date(groupedData.getDate())
                        .platform(platformData.getPlatform())
                        .views(platformData.getViews())
                        .listeners(platformData.getListeners())
                        .viewsIncrease(platformData.getViewsIncrease())
                        .listenersIncrease(platformData.getListenersIncrease())
                        .videoInfos(groupedData.getVideoInfos())
                        .build();

                resultList.add(dto);
            }
        }

        return CrawlingDataWithSongInfoDto.builder()
                .songInfo(songInfo)
                .crawlingDataList(resultList)
                .groupedDataList(groupedDataList)
                .totalPages(pagedResult.getTotalPages())
                .totalElements(pagedResult.getTotalElements())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * 특정 날짜가 크롤링 시작일인지 확인합니다.
     */
    private boolean isStartDate(String songId, LocalDate date) {
        return !crawlingPeriodRepository.findBySongIdAndStartDate(songId, date).isEmpty();
    }

    /**
     * 특정 날짜의 영상 정보를 조회합니다.
     */
    private List<CrawlingDataResponseDto.VideoInfo> getVideoInfosForDate(String songId, LocalDate date) {
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

    /**
     * 크롤링 실패한 곡들을 조회합니다.
     * crawling_failure 테이블에서 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<CrawlingFailureDto> getCrawlingFailures(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // 크롤링 실패 테이블에서 조회
        Page<com.rhoonart.unearth.crawling.entity.CrawlingFailure> failureResults = crawlingFailureRepository
                .findAllWithSongInfo(pageable);

        return failureResults.map(CrawlingFailureDto::from);
    }
}