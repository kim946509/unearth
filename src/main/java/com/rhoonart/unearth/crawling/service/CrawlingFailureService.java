package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.crawling.dto.CrawlingFailureDto;
import com.rhoonart.unearth.crawling.repository.CrawlingFailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlingFailureService {

    private final CrawlingFailureRepository crawlingFailureRepository;

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
