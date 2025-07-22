package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.exception.CannotFindSongException;
import com.rhoonart.unearth.song.service.SongInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingManagerService {

    private final SongInfoService songInfoService;
    private final CrawlingExecuteService crawlingExecuteService;
    private final CrawlingPeriodService crawlingPeriodService;
    /**
     * 크롤링 실행 메인 로직 메서드
     * @param dto
     */
    @Transactional
    public void executeCrawling(CrawlingExecuteRequestDto dto) {

        // 크롤링 기간 및 영상 정보 저장
        crawlingPeriodService.createAndSaveCrawlingPeriod(dto);

        // 4. 크롤링 실행 (비동기)
        crawlingExecuteService.executeSingleSongCrawling(dto.getSongId());
    }

    @Transactional
    public void executeCrawlingOnly(String songId) {

        // 음원 조회
        SongInfo song = songInfoService.getSongInfoById(songId).orElseThrow(CannotFindSongException::new);

        // 크롤링 실행 (비동기)
        crawlingExecuteService.executeSingleSongCrawling(song.getId());
    }


}