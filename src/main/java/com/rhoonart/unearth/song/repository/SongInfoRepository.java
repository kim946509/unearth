package com.rhoonart.unearth.song.repository;

import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SongInfoRepository extends JpaRepository<SongInfo, String> {
    int countByRightHolder(RightHolder rightHolder);

    boolean existsByMelonSongId(String melonSongId);

    Optional<SongInfo> findByMelonSongId(String melonSongId);

    // artist_ko와 title_ko로 중복 검사
    boolean existsByArtistKoAndTitleKo(String artistKo, String titleKo);

    // artist_ko와 title_ko로 중복 검사 (자신 제외)
    @Query("SELECT COUNT(s) > 0 FROM SongInfo s WHERE s.artistKo = :artistKo AND s.titleKo = :titleKo AND s.id != :excludeId")
    boolean existsByArtistKoAndTitleKoExcludingId(@Param("artistKo") String artistKo,
            @Param("titleKo") String titleKo,
            @Param("excludeId") String excludeId);

    @Query("""
                SELECT s FROM SongInfo s
                WHERE (
                    :search IS NULL OR :search = ''
                    OR s.artistKo LIKE CONCAT('%', :search, '%')
                    OR s.albumKo LIKE CONCAT('%', :search, '%')
                    OR s.titleKo LIKE CONCAT('%', :search, '%')
                    OR s.rightHolder.holderName LIKE CONCAT('%', :search, '%')
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SongInfo> searchSearchFields(@Param("search") String search, Pageable pageable);

    @Query("""
                SELECT s FROM SongInfo s
                WHERE s.rightHolder.id = :rightHolderId
                AND (
                    :search IS NULL OR :search = ''
                    OR s.artistKo LIKE CONCAT('%', :search, '%')
                    OR s.albumKo LIKE CONCAT('%', :search, '%')
                    OR s.titleKo LIKE CONCAT('%', :search, '%')
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SongInfo> findByRightHolderIdWithSearch(@Param("rightHolderId") String rightHolderId,
            @Param("search") String search, Pageable pageable);

    @Query("""
                SELECT s FROM SongInfo s
                WHERE (
                    :search IS NULL OR :search = ''
                    OR s.artistKo LIKE CONCAT('%', :search, '%')
                    OR s.albumKo LIKE CONCAT('%', :search, '%')
                    OR s.titleKo LIKE CONCAT('%', :search, '%')
                    OR s.rightHolder.holderName LIKE CONCAT('%', :search, '%')
                )
                AND (
                    :isCrawlingActive IS NULL
                    OR :isCrawlingActive = false
                    OR (
                        :isCrawlingActive = true
                        AND EXISTS (
                            SELECT 1 FROM CrawlingPeriod cp
                            WHERE cp.song = s
                            AND cp.startDate IS NOT NULL
                            AND cp.endDate IS NOT NULL
                            AND :currentDate >= cp.startDate
                            AND :currentDate <= cp.endDate
                        )
                    )
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SongInfo> searchSongsWithCrawlingFilter(
            @Param("search") String search,
            @Param("isCrawlingActive") Boolean isCrawlingActive,
            @Param("currentDate") java.time.LocalDate currentDate,
            Pageable pageable);
}