package com.rhoonart.unearth.crawling.repository;

import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.song.entity.SongInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlingPeriodRepository extends JpaRepository<CrawlingPeriod, String> {

    @Query("""
                SELECT cp FROM CrawlingPeriod cp
                WHERE cp.song = :song
                ORDER BY cp.createdAt DESC
                LIMIT 1
            """)
    Optional<CrawlingPeriod> findLatestBySong(@Param("song") SongInfo song);

    /**
     * 특정 음원의 특정 시작일 크롤링 기간을 조회합니다.
     */
    @Query("""
                SELECT cp FROM CrawlingPeriod cp
                WHERE cp.song.id = :songId
                AND cp.startDate = :startDate
                ORDER BY cp.songOrder ASC
            """)
    List<CrawlingPeriod> findBySongIdAndStartDate(
            @Param("songId") String songId,
            @Param("startDate") LocalDate startDate);

    /**
     * 특정 음원의 특정 날짜가 포함된 모든 크롤링 기간을 조회합니다.
     */
    @Query("""
                SELECT cp FROM CrawlingPeriod cp
                WHERE cp.song.id = :songId
                AND cp.startDate <= :date
                AND cp.endDate >= :date
                AND cp.isActive = true
                ORDER BY cp.songOrder ASC
            """)
    List<CrawlingPeriod> findBySongIdAndDateRange(
            @Param("songId") String songId,
            @Param("date") LocalDate date);

    /**
     * 특정 음원의 모든 크롤링 기간을 조회합니다. (디버깅용)
     */
    @Query("""
                SELECT cp FROM CrawlingPeriod cp
                WHERE cp.song.id = :songId
                ORDER BY cp.startDate DESC
            """)
    List<CrawlingPeriod> findAllBySongId(@Param("songId") String songId);

    /**
     * 특정 음원의 크롤링 기간 개수를 조회합니다. (디버깅용)
     */
    @Query("""
                SELECT COUNT(cp) FROM CrawlingPeriod cp
                WHERE cp.song.id = :songId
            """)
    long countBySongId(@Param("songId") String songId);
}