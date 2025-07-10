package com.rhoonart.unearth.song.entity;

import com.rhoonart.unearth.global.BaseEntity;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "song_info")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongInfo extends BaseEntity {
    @Id
    @UuidGenerator
    @Column(length = 32, nullable = false, updatable = false, unique = true)
    private String id;

    @Column(name = "artist_ko", length = 255, nullable = false)
    private String artistKo;

    @Column(name = "artist_en", length = 255, nullable = false)
    private String artistEn;

    @Column(name = "title_ko", length = 255, nullable = false)
    private String titleKo;

    @Column(name = "title_en", length = 255, nullable = false)
    private String titleEn;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "melon_song_id", length = 100, unique = true)
    private String melonSongId;

    @Column(name = "album_ko", length = 255)
    private String albumKo;

    @Column(name = "album_en", length = 255)
    private String albumEn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "right_holder_id", nullable = false)
    private RightHolder rightHolder;
}