package com.rhoonart.unearth.crawling.entity;

import com.rhoonart.unearth.global.BaseEntity;
import com.rhoonart.unearth.song.entity.SongInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "crawling_data")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingData extends BaseEntity {
    @Id
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false, unique = true)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20, nullable = false)
    private PlatformType platform;

    @ManyToOne(optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private SongInfo song;

    @Column(name = "views", nullable = false)
    private long views;

    @Column(name = "listeners", nullable = false)
    private long listeners;
}