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
        Page<com.rhoonart.unearth.crawling.entity.CrawlingFailure> failureResults =
                crawlingFailureRepository.findAllWithSongInfo(pageable);
        return failureResults.map(CrawlingFailureDto::from);
    }

    /**
     * 크롤링 실패한 곡의 개수를 제한적으로 조회합니다.
     * 10개를 초과하면 "10+" 형태로 반환합니다.
     */
    @Transactional(readOnly = true)
    public String getLimitedCrawlingFailureCount() {
        long count = crawlingFailureRepository.countLimitedFailures();
        return (count > 10) ? "10+" : String.valueOf(count);
    }
}
