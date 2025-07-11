package com.rhoonart.unearth.crawling.repository;

import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.song.entity.SongInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}