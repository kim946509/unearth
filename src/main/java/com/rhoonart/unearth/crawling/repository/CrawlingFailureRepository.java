package com.rhoonart.unearth.crawling.repository;

import com.rhoonart.unearth.crawling.entity.CrawlingFailure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlingFailureRepository extends JpaRepository<CrawlingFailure, String> {

    /**
     * 실패한 곡 목록을 페이징하여 조회 (최신순)
     */
    @Query("""
            SELECT cf FROM CrawlingFailure cf
            JOIN FETCH cf.song s
            JOIN FETCH s.rightHolder
            ORDER BY cf.failedAt DESC
            """)
    Page<CrawlingFailure> findAllWithSongInfo(Pageable pageable);
}