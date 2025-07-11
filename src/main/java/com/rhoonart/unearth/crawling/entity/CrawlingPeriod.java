package com.rhoonart.unearth.crawling.entity;

import com.rhoonart.unearth.global.BaseEntity;
import com.rhoonart.unearth.song.entity.SongInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "crawling_period")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingPeriod extends BaseEntity {
    @Id
    @UuidGenerator
    @Column(length = 32, nullable = false, updatable = false, unique = true)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private SongInfo song;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "youtube_title", nullable = false)
    private String youtubeTitle;

    @Column(name = "youtube_url", nullable = false)
    private String youtubeUrl;

    @Column(name = "track_order", nullable = false)
    private int track_order;
}