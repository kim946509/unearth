package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.dto.CrawlingFailureDto;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
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

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingManagerService {

    // 크롤링 실행 시간 (17시)
    private static final int CRAWLING_HOUR = 17;
    // 크롤링 기간 (30일)
    private static final int CRAWLING_PERIOD_DAYS = 30;

    private final CrawlingPeriodRepository crawlingPeriodRepository;
    private final CrawlingDataRepository crawlingDataRepository;
    private final CrawlingFailureRepository crawlingFailureRepository;
    private final SongInfoRepository songInfoRepository;
    private final CrawlingExecuteService crawlingExecuteService;

    /**
     * 크롤링 실행 메인 로직 메서드
     * @param dto
     */
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
            return java.time.LocalDateTime.parse(uploadAtStr);
        } catch (Exception e) {
            log.warn("영상 업로드 시점 파싱 실패: {}", uploadAtStr, e);
            return null;
        }
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

    /**
     * 크롤링 실패한 곡의 총 개수를 조회합니다.
     */
    @Transactional(readOnly = true)
    public long getCrawlingFailureCount() {
        return crawlingFailureRepository.count();
    }

}