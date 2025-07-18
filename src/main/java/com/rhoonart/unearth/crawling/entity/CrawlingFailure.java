package com.rhoonart.unearth.crawling.entity;

import com.rhoonart.unearth.global.BaseEntity;
import com.rhoonart.unearth.song.entity.SongInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "crawling_failure")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingFailure extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false, unique = true)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", insertable = false, updatable = false, unique = true)
    private SongInfo song;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "failed_platforms", nullable = false)
    private String failedPlatforms;

    /**
     * 실패한 플랫폼 목록을 리스트로 반환
     */
    public List<String> getFailedPlatformsList() {
        if (failedPlatforms == null || failedPlatforms.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(failedPlatforms.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 실패 정보 업데이트
     */
    public void updateFailure(LocalDateTime failedAt, String failedPlatforms) {
        this.failedAt = failedAt;
        this.failedPlatforms = failedPlatforms;
    }
}