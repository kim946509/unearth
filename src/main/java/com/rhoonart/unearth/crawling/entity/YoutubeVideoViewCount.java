package com.rhoonart.unearth.crawling.entity;

import com.rhoonart.unearth.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "youtube_video_viewcount", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "crawling_period_id", "date" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class YoutubeVideoViewCount extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false, unique = true)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crawling_period_id", nullable = false)
    private CrawlingPeriod crawlingPeriod;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = -999;

}