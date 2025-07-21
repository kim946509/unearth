package com.rhoonart.unearth.crawling.repository;

import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlingDataRepository extends JpaRepository<CrawlingData, String> {

        @Query("""
                            SELECT cd FROM CrawlingData cd
                            WHERE cd.song.id = :songId
                            ORDER BY cd.createdAt DESC
                        """)
        List<CrawlingData> findBySongIdOrderByCreatedAtDesc(@Param("songId") String songId);

        @Query("""
                            SELECT cd FROM CrawlingData cd
                            WHERE cd.song.id = :songId
                            AND (:platform IS NULL OR cd.platform = :platform)
                            AND (:startDate IS NULL OR DATE(cd.createdAt) >= :startDate)
                            AND (:endDate IS NULL OR DATE(cd.createdAt) <= :endDate)
                            ORDER BY DATE(cd.createdAt) ASC, cd.platform ASC
                        """)
        List<CrawlingData> findBySongIdAndDateRange(
                        @Param("songId") String songId,
                        @Param("platform") PlatformType platform,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * 페이징된 크롤링 데이터를 조회합니다.
         * Spring Data JPA의 Pageable을 사용하여 DB 레벨에서 페이징 처리합니다.
         */
        @Query("""
                            SELECT cd FROM CrawlingData cd
                            WHERE cd.song.id = :songId
                            AND (:platform IS NULL OR cd.platform = :platform)
                            AND (:startDate IS NULL OR DATE(cd.createdAt) >= :startDate)
                            AND (:endDate IS NULL OR DATE(cd.createdAt) <= :endDate)
                            ORDER BY DATE(cd.createdAt) DESC, cd.platform ASC
                        """)
        Page<CrawlingData> findPagedCrawlingData(
                        @Param("songId") String songId,
                        @Param("platform") PlatformType platform,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        /**
         * 특정 날짜의 특정 플랫폼 데이터를 조회합니다.
         * 이전날 데이터 비교를 위해 사용됩니다.
         */
        @Query("""
                            SELECT cd FROM CrawlingData cd
                            WHERE cd.song.id = :songId
                            AND cd.platform = :platform
                            AND DATE(cd.createdAt) = :date
                            ORDER BY cd.createdAt DESC
                        """)
        Optional<CrawlingData> findBySongIdAndPlatformAndDate(
                        @Param("songId") String songId,
                        @Param("platform") PlatformType platform,
                        @Param("date") LocalDate date);

        boolean existsBySongId(String songId);

}