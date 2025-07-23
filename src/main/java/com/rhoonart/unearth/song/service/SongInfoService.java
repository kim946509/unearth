package com.rhoonart.unearth.song.service;

import com.rhoonart.unearth.song.dto.SongInfoRegisterRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoUpdateRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoWithCrawlingDto;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SongInfoService {
    private final SongInfoRepository songInfoRepository;
    private final CrawlingPeriodRepository crawlingPeriodRepository;


    public Page<SongInfoWithCrawlingDto> findSongsWithCrawling(String search, Pageable pageable,
            Boolean isCrawlingActive) {
        LocalDate now = LocalDate.now();

        // Repository에서 크롤링 필터 조건을 포함하여 조회
        Page<SongInfo> songPage = songInfoRepository.searchSongsWithCrawlingFilter(search, isCrawlingActive, now,
                pageable);

        List<SongInfoWithCrawlingDto> songsWithCrawling = songPage.getContent().stream()
                .map(song -> {
                    Optional<CrawlingPeriod> latestCrawling = crawlingPeriodRepository.findLatestBySong(song);
                    if (latestCrawling.isPresent()) {
                        CrawlingPeriod period = latestCrawling.get();
                        return SongInfoWithCrawlingDto.from(song, period.getStartDate(), period.getEndDate());
                    } else {
                        return SongInfoWithCrawlingDto.from(song, null, null);
                    }
                })
                .toList();

        return new PageImpl<>(songsWithCrawling, pageable, songPage.getTotalElements());
    }


    /**
     * 음원 ID로 음원 정보를 조회합니다.
     *
     * @param songId 음원 ID
     * @return SongInfo
     */
    public Optional<SongInfo> getSongInfoById(String songId) {
        return songInfoRepository.findById(songId);
    }

}