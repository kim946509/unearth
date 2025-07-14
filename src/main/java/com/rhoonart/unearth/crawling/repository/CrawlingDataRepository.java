package com.rhoonart.unearth.crawling.repository;

import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.song.entity.SongInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
                AND DATE(cd.createdAt) BETWEEN :startDate AND :endDate
                ORDER BY DATE(cd.createdAt) ASC, cd.platform ASC
            """)
    List<CrawlingData> findBySongIdAndDateRange(
            @Param("songId") String songId,
            @Param("platform") PlatformType platform,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsBySongId(String songId);
}