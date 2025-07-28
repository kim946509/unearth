package com.rhoonart.unearth.song.entity;

import com.rhoonart.unearth.global.BaseEntity;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "song_info", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "artist_ko", "title_ko" })
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongInfo extends BaseEntity {
    @Id
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false, unique = true)
    private String id;

    @Column(name = "artist_ko", length = 255, nullable = false)
    private String artistKo;

    @Column(name = "artist_en", length = 255, nullable = false)
    private String artistEn;

    @Column(name = "title_ko", length = 255, nullable = false)
    private String titleKo;

    @Column(name = "title_en", length = 255, nullable = false)
    private String titleEn;

    @Column(name = "youtube_url", length = 500, nullable = false)
    private String youtubeUrl;

    @Column(name = "melon_song_id", length = 100, unique = true, nullable = true)
    private String melonSongId;

    @Column(name = "album_ko", length = 255, nullable = false)
    private String albumKo;

    @Column(name = "album_en", length = 255, nullable = false)
    private String albumEn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "right_holder_id", nullable = false)
    private RightHolder rightHolder;

    public void updateInfo(String artistKo, String artistEn, String albumKo, String albumEn,
            String titleKo, String titleEn, String youtubeUrl, String melonSongId,
            RightHolder rightHolder) {
        this.artistKo = artistKo;
        this.artistEn = artistEn;
        this.albumKo = albumKo;
        this.albumEn = albumEn;
        this.titleKo = titleKo;
        this.titleEn = titleEn;
        this.youtubeUrl = youtubeUrl;
        this.melonSongId = melonSongId;
        this.rightHolder = rightHolder;
    }
}