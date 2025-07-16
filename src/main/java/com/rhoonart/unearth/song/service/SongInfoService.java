package com.rhoonart.unearth.song.service;

import com.rhoonart.unearth.song.dto.SongInfoRegisterRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoUpdateRequestDto;
import com.rhoonart.unearth.song.dto.SongInfoWithCrawlingDto;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SongInfoService {
    private final SongInfoRepository songInfoRepository;
    private final RightHolderRepository rightHolderRepository;
    private final CrawlingPeriodRepository crawlingPeriodRepository;

    @Transactional
    public void register(SongInfoRegisterRequestDto dto) {
        // 1. songId(멜론) 중복 체크
        if (songInfoRepository.existsByMelonSongId(dto.getMelonSongId())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 등록된 음원입니다.");
        }
        // 2. 권리자명으로 RightHolder 조회
        RightHolder rightHolder = rightHolderRepository.findByHolderName(dto.getRightHolderName())
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));
        // 3. 엔티티 생성/저장
        SongInfo song = SongInfo.builder()
                .artistKo(dto.getArtistKo())
                .artistEn(dto.getArtistEn())
                .albumKo(dto.getAlbumKo())
                .albumEn(dto.getAlbumEn())
                .titleKo(dto.getTitleKo())
                .titleEn(dto.getTitleEn())
                .youtubeUrl(dto.getYoutubeUrl())
                .melonSongId(dto.getMelonSongId())
                .rightHolder(rightHolder)
                .build();
        songInfoRepository.save(song);
    }

    public Page<SongInfo> findSongs(String search, Pageable pageable) {
        // (예시) 아티스트명, 앨범명, 트랙명, 권리자명 등에서 검색
        return songInfoRepository.searchSearchFields(search, pageable);
    }

    public Page<SongInfoWithCrawlingDto> findSongsWithCrawling(String search, Pageable pageable) {
        Page<SongInfo> songPage = songInfoRepository.searchSearchFields(search, pageable);

        List<SongInfoWithCrawlingDto> songsWithCrawling = songPage.getContent().stream()
                .map(song -> {
                    Optional<CrawlingPeriod> latestCrawling = crawlingPeriodRepository.findLatestBySong(song);
                    if (latestCrawling.isPresent()) {
                        CrawlingPeriod period = latestCrawling.get();
                        return SongInfoWithCrawlingDto.from(song, period.getStartDate(), period.getEndDate());
                    } else {
                        return SongInfoWithCrawlingDto.from(song, null, null);
                    }
                })
                .toList();

        return new PageImpl<>(songsWithCrawling, pageable, songPage.getTotalElements());
    }

    @Transactional
    public void update(String songId, SongInfoUpdateRequestDto dto) {
        // 1. 음원 존재 여부 확인
        SongInfo song = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 2. melonSongId 중복 체크 (자신 제외)
        if (!song.getMelonSongId().equals(dto.getMelonSongId()) &&
                songInfoRepository.existsByMelonSongId(dto.getMelonSongId())) {
            throw new BaseException(ResponseCode.INVALID_INPUT, "이미 등록된 음원입니다.");
        }

        // 3. 권리자명으로 RightHolder 조회
        RightHolder rightHolder = rightHolderRepository.findByHolderName(dto.getRightHolderName())
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "권리자를 찾을 수 없습니다."));

        // 4. 음원 정보 업데이트
        song.updateInfo(
                dto.getArtistKo(),
                dto.getArtistEn(),
                dto.getAlbumKo(),
                dto.getAlbumEn(),
                dto.getTitleKo(),
                dto.getTitleEn(),
                dto.getYoutubeUrl(),
                dto.getMelonSongId(),
                rightHolder);

        songInfoRepository.save(song);
    }
}