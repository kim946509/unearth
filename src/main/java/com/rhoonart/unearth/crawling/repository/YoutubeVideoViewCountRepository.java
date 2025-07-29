package com.rhoonart.unearth.crawling.repository;

import com.rhoonart.unearth.crawling.entity.YoutubeVideoViewCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

        /**
         * 특정 크롤링 기간 목록의 날짜 범위에 해당하는 모든 조회수 데이터를 일괄 조회합니다. (N+1 문제 해결)
         */
        @Query("""
                        SELECT yvvc FROM YoutubeVideoViewCount yvvc
                        WHERE yvvc.crawlingPeriod.id IN :crawlingPeriodIds
                        AND yvvc.date >= :startDate
                        AND yvvc.date <= :endDate
                        """)
        List<YoutubeVideoViewCount> findByCrawlingPeriodIdsAndDateRange(
                        @Param("crawlingPeriodIds") List<String> crawlingPeriodIds,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}