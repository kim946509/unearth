package com.rhoonart.unearth.song.repository;

import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
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
                    OR s.rightHolder.holderName LIKE CONCAT('%', :search, '%')
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SongInfo> findByRightHolderIdWithSearch(@Param("rightHolderId") String rightHolderId,
            @Param("search") String search, Pageable pageable);
}