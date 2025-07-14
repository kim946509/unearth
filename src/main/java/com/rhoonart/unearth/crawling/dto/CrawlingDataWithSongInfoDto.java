package com.rhoonart.unearth.crawling.dto;

import com.rhoonart.unearth.song.entity.SongInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingDataWithSongInfoDto {
    private SongInfo songInfo;
    private List<CrawlingDataResponseDto> crawlingDataList;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;
}