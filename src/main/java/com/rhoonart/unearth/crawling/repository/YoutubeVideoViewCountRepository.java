package com.rhoonart.unearth.crawling.repository;

import com.rhoonart.unearth.crawling.entity.YoutubeVideoViewCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface YoutubeVideoViewCountRepository extends JpaRepository<YoutubeVideoViewCount, String> {

        /**
         * 특정 크롤링 기간의 특정 날짜 조회수를 조회합니다.
         */
        @Query("""
                        SELECT yvvc FROM YoutubeVideoViewCount yvvc
                        WHERE yvvc.crawlingPeriod.id = :crawlingPeriodId
                        AND yvvc.date = :date
                        """)
        Optional<YoutubeVideoViewCount> findByCrawlingPeriodIdAndDate(
                        @Param("crawlingPeriodId") String crawlingPeriodId,
                        @Param("date") LocalDate date);
}