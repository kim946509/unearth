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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private LocalDate calculateStartDate() {
        return LocalDate.now();
    }

    public CrawlingDataWithSongInfoDto getCrawlingDataWithFilters(String songId, LocalDate startDate,
            LocalDate endDate, PlatformType platform, int page, int size) {
        // 음원 존재 여부 확인
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 1. Pageable 생성 (페이지는 0부터 시작하므로 -1)
        Pageable pageable = PageRequest.of(page - 1, size);

        // 2. DB 레벨에서 페이징된 데이터 조회
        Page<CrawlingData> pagedResult = crawlingDataRepository.findPagedCrawlingData(
                songId, platform, startDate, endDate, pageable);

        // 3. 페이징된 데이터에 대해 이전날 데이터 조회 및 증가량 계산
        List<CrawlingDataResponseDto> resultList = new ArrayList<>();

        for (CrawlingData currentData : pagedResult.getContent()) {
            LocalDate currentDate = currentData.getCreatedAt().toLocalDate();

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

            CrawlingDataResponseDto dto = CrawlingDataResponseDto.builder()
                    .date(currentDate)
                    .platform(currentData.getPlatform())
                    .views(currentData.getViews())
                    .listeners(currentData.getListeners())
                    .viewsIncrease(viewsIncrease)
                    .listenersIncrease(listenersIncrease)
                    .build();

            resultList.add(dto);
        }

        return CrawlingDataWithSongInfoDto.builder()
                .songInfo(songInfo)
                .crawlingDataList(resultList)
                .totalPages(pagedResult.getTotalPages())
                .totalElements(pagedResult.getTotalElements())
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