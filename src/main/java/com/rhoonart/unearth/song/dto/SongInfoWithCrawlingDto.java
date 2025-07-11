package com.rhoonart.unearth.song.dto;

import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "of")
public class SongInfoWithCrawlingDto {
    private final String id;
    private final String artistKo;
    private final String artistEn;
    private final String albumKo;
    private final String albumEn;
    private final String titleKo;
    private final String titleEn;
    private final String youtubeUrl;
    private final String melonSongId;
    private final RightHolder rightHolder;
    private final LocalDate crawlingStartDate;
    private final LocalDate crawlingEndDate;

    public static SongInfoWithCrawlingDto from(SongInfo song, LocalDate startDate, LocalDate endDate) {
        return SongInfoWithCrawlingDto.of(
                song.getId(),
                song.getArtistKo(),
                song.getArtistEn(),
                song.getAlbumKo(),
                song.getAlbumEn(),
                song.getTitleKo(),
                song.getTitleEn(),
                song.getYoutubeUrl(),
                song.getMelonSongId(),
                song.getRightHolder(),
                startDate,
                endDate);
    }
}